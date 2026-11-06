# Design

## 1. 方案与现有实现

| 当前落点 | 现状 | 本次职责 |
|---|---|---|
| identity/InitializationCatalog、TenantInitializer、TenantInitializationRepository | 初始化引用租户 SYSTEM 固定版本并写分配 | 以 ownerMemberId 建立拥有者；删除新建流程对治理版本及固定拥有者分配的依赖 |
| evaluation/AuthorizationEvaluator、AuthorizationEvaluationRepository | 平台有动态超管分支；租户从 revisionId 合成功能和范围 | 增加独立租户拥有者求值；租户分配解析 roleId 的当前快照 |
| role/RoleService、RoleSynthesis、RoleSynthesisCache | 完整私有定义与固定共享基础差异并存 | 租户/共享保存完整当前定义；旧差异只供迁移和历史解释 |
| assignment/AssignmentService、TenantScopeCandidates | 输入及候选依赖固定版本 | 拆出租户角色身份契约，持续绑定资源及参数 |
| delegation/DelegationService、DelegationAdmission、IamMembershipSql | 版本白名单及逐操作上限 | 租户按角色白名单及资源上限校验；平台分支保持 |
| session/SessionService、SessionRepository | 应用开通与普通人群过滤 | 与同次拥有者事实一致，拥有者应用/菜单不受普通人群排除 |
| extension/LocalResourceAuthorizationClient、policy/FieldAccessEvaluator、DirectoryVisibilityEvaluator | 租户字段/通讯录策略单独求值 | 拥有者覆盖本地策略，完整已注册租户资源统一执行 |
| ingot-access-control/RemoteAuthorizationClient、mybatis-scope/AuthorizationSnapshotAccess | 普通读取可用 30 秒热缓存 | 租户执行读取实时结果，旧允许结论不跨请求放行 |

代码路径均相对仓库根的 `ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/`，框架落点见表中模块。不得将平台特权布尔值改造成同时表示拥有者。

## 2. 数据模型

采用现有不可变 `iam_role_revision` / `iam_role_grant` / `iam_role_parameter` 保存内部完整快照，减少对平台固定版本模型的侵入。快照序号、ID、修改历史只由服务器维护。

| 数据 | 目标约束 |
|---|---|
| iam_tenant.owner_member_id | 拥有者唯一事实；引用有效本租户成员；保留组织乐观锁 |
| iam_role_definition.current_revision_id（新增） | SHARED/TENANT_CUSTOM 当前完整快照；提交前必须指向本角色无共享基础的快照；平台自有角色不使用本字段改变既有版本语义 |
| 租户角色快照 | grants 完整保存；base_revision_id 为空；无新 delta。历史快照/差异保留，不继续作为运行时继承来源 |
| iam_role_assignment.role_id / role_kind / role_tenant_key（新增） | TENANT 有效记录仅可关联 SHARED 或本租户 TENANT_CUSTOM；PLATFORM 继续使用 revision_id/revision_kind |
| 租户分配旧 revision_id/revision_kind | 旧记录保留为迁移及回滚证据，不参与 TENANT 当前授权；新租户记录可空；平台记录仍必填 |
| iam_delegation_role（新增） | 租户委派 → 角色身份，主键 delegation_id+role_id；服务器与复合归属约束验证 SHARED/当前租户私有角色 |
| iam_delegation_resource_ceiling（新增） | 租户委派 → resource_id+scopes+scope_bindings，主键 delegation_id+resource_id；角色当前操作必须落入资源上限 |
| 既有 delegation_role_revision/action_ceiling | 平台继续使用；旧租户内容保存为迁移来源，不能与新表并集授权 |
| 迁移批次与映射 | 保存来源校验摘要、目标指针、旧新引用、冲突处置、退休 SYSTEM 分配和审计映射；不参与授权求值 |

当前快照外键使用 `(current_revision_id, role_id)` 验证快照属于本角色；首次创建在事务中先建立角色/快照再设置指针。有效分配的角色归属采用 `(domain, role_tenant_key, role_id, role_kind)` 复合引用，角色定义增加对应唯一键；共享 key=0，私有 key=当前 tenantId。CHECK 约束只允许两类租户角色，平台仍走原固定引用。历史退休 SYSTEM 分配允许保留旧引用及 REVOKED 状态，由历史接口解释，不进入普通角色分配列表。

不能只在前端隐藏 revisionId，也不能把旧 RoleRevisionRef.id 改成角色 ID 后继续复用同名契约。新请求/响应使用独立 Tenant 类型，公共平台 DTO 原语义保留。

## 3. 拥有者动态授权

### 3.1 判定与展开

从本次可信身份及数据库组织行读取 ownerMemberId，形成独立 `tenantOwner` 内部结论；它不接受 JWT 或请求体输入。批量 JOIN 当前租户有效开通、启用租户应用和启用 ACTION，返回精确操作及本租户 ALL 范围；包含最近开通截止，不延长权限有效期。

查询拥有者开通不依赖 iam_app_audience 的启停/人群。普通成员仍核对人群。应用可用目录与 SessionService 的 applications/menus 使用同一身份和开通事实，避免操作可用但菜单消失。基础应用的必开规则保留。

资源执行仍使用 AuthorizationAccess、可信 tenantId 和已注册执行器。ALL 表示组织内全部，不能取消租户 SQL 条件。未注册资源/字段、只读系统字段、内部凭证和第三方未声明能力不因拥有者扩权。

### 3.2 字段、通讯录与转交

拥有者字段决策按资源 descriptor 和字段绑定清单生成 FULL 条款，并与平台强制上限及真实 WRITE/FILTER 能力求交，覆盖租户规则但不伪造字段能力。分离“平台强制上限”与“租户默认/规则”，不能把默认脱敏误当强制上限。普通租户字段行为不变，不增加角色字段快照配置。

LocalResourceAuthorizationClient 的拥有者字段分支必须支持任意已注册 TENANT 资源，不只成员/通讯录。内置成员读取、通讯录可见性、导出和字段投影也使用同次拥有者事实，不逐行查询。无拥有者资格时保持现有资源支持边界。

转交在组织锁与成员锁下重新检查当前拥有者、目标成员、组织版本；只更新 ownerMemberId 并记录审计，不拷贝或迁移特权角色分配。普通角色保留。新建组织及转交返回后，本会话应刷新 capabilities/bootstrap。

## 4. 角色保存与分配

共享角色平台写入口、私有角色租户写入口分别检查真实管理资格及角色归属。保存完整定义时校验资源/ACTION 域、应用归属、范围能力、参数唯一及稳定绑定。私有角色新选操作限定在当前租户有效开通的应用；原定义中应用暂时停用/到期的操作可保留并标记不可执行，不能因资料编辑自动删除。

事务步骤：锁当前角色 → 核对 expectedVersion → 按最新目录校验 → 插入完整内部快照 → 更新 current_revision_id 与配置锁 → 审计 → 提交后失效广播。不得对全部成员/组分配执行版本批量改写。预览仅生成当前定义、范围变化及可披露影响，不修改数据；提交重验，不信预览布尔值。

分配保存 roleId、主体、绑定、期限和可选 delegationGrantId。求值批量读取有效直接/组来源 → 关联当前角色快照 → 每条来源独立绑定范围 → 对来源委派求交 → 按操作求并集。用户组成员变化实时展开并逐成员校验委派接收资格。

绑定 identity 是 `(resourceId, bindingKind, parameterKey)`；现有稳定键保留，新增键从候选目录生成。定义声明 resourceId 后验证所有引用操作属于该资源。已有键改变资源/类型返回 400，使用新键表示新绑定。资料与范围变化不复制角色，不产生升级任务。

角色增加参数而存量分配缺失时，返回 derived `scopeIssues`，对应操作关闭；不创建持久 PARTIAL 状态，不把其他有效操作一并关掉。角色更新可提交，预览 warnings 明确说明影响。新分配/分配编辑仍需补齐当前全部必填绑定。旧无用键不求值并保留原始审计；移除新字段不是回退到默认全部。

## 5. 委派与管理资格

租户委派白名单读 roleId，资源上限读取完整 ResourceKey 对应 resourceId。运行时先检查委派/接收资格/期限，再对每个操作以其资源上限收紧。新增操作不新增逐操作上限；新资源缺上限则该资源贡献关闭，其他资源保持。选择器和 scope-candidates 以 roleId/资源/委派 ID 解析，不能靠切换 purpose 枚举对象全集。

有效委派可补充租户 assignment read/create/update/delete 的受限入口，不写入 governedCodes，不补业务操作。提供受限候选/本人有效委派摘要接口，不要求管理员先有全量 role/member/delegation 读取权。列表、回显、详情、总数及写入都限制为该委派允许集合；管理员只管理本人该来源记录。委派不能触发私有角色编辑、再次委派或拥有者转交。

角色定义编辑本身、委派编辑本身必须有独立治理资格，防止经委派获得一个包含治理 ACTION 的角色后再次扩权。平台和租户的判断分开，拥有者转交额外验证真实拥有者，不是仅检查 ACTION 字符串。已有非委派治理分配按直接权限保留。

## 6. 实时路径与一致性

所有 TENANT 执行准入、对象范围、字段决策、授权快照、capabilities/bootstrap/诊断读取当前可变事实；不使用跨请求的允许决策缓存。RemoteAuthorizationClient 的租户分支始终在线调用 IAM，数据范围旧快照入口也必须 fresh；不得只修 IAM 而漏 SDK。平台原有缓存/固定版本分支保持。

同一次请求批量读和共享一致视图，禁止逐成员/字段/操作 RPC。角色完整快照按不可变 ID 复用 VersionedDerivedCache；当前引用先从数据库取。现有 AuthorizationChangeNotifier / 提交后清本地并广播继续使用，不增加手写缓存或授权 LKG。变更规则版本前缀隔离旧缓存；SDK 响应字段和可信身份的校验保持。

写事务采用一致锁顺序：平台治理锁（仅平台写）→ 当前租户组织锁（租户治理写）→ 按 ID 升序角色行 → 委派行 → 分配/成员行。共享角色编辑锁共享角色，不逐租户取锁或更新分配。分配写锁其角色当前引用并重读权限；并发角色变更不得用已失效的草稿落入越界授权。转交/撤权与关键授权写在组织锁内重验身份。写目标/业务约束由执行资源继续负责。

视图指纹必须覆盖拥有者/身份状态、角色当前快照、分配绑定与期限、委派/资源上限、开通、人群、目录与字段相关修订。前端只用它判断是否刷新完整 bootstrap，不当角色业务版本。版本指纹变化不得通过比较 memberId 或只比较角色 ID 代替。

正常网络下前端其他可见租户会话展示刷新上限 30 秒，按 expiresAt 调度单一去重请求；页面保存成功立即刷新。对 503 保留展示但关闭可执行能力、退避重试，不重新发送原写请求。

## 7. 兼容、迁移与退出

目标接口见 API。租户/共享的 publish 与 upgrade 业务入口退出；平台自有版本/批量任务不受影响。租户 role:publish 和平台 shared-role:publish 原 ACTION 按原 ID 改名为 update，保留原授权引用；升级操作退休，历史审计原码不重写。详细数据转换、操作码回滚和异常记录见 MIGRATION。

已有库采用增加结构、预检、迁移、统一程序切换，不用新库重建脚本。原始版本、差异、范围及分配备份保留。发布窗口不能混合旧租户客户端和新后端；退出路由显式 410、旧版本请求字段返回 400，不把 ID 自动猜成角色 ID。

## 8. 规范、测试及性能预算

实现遵循 in-framework：构造注入、命名常量/枚举、JavaDoc、UTC ISO 时间、对象存储路径和关联查询；缓存复用 ingot-cache。前端契约以本目录 API 为权威，不更新 current 直到联合验收。

自动化覆盖数据约束、真实 MyBatis 事务、有效期边界、独立来源合并、跨租户拒绝、缺失绑定、迁移重复/中断/逆转换。真实 HTTP 覆盖两个 IAM 节点、独立消费服务、禁止依靠旧缓存/消息放行；浏览器覆盖两组织普通/委派/拥有者身份和平台共享管理。证据矩阵见 ACCEPTANCE。

评审采用以下性能预算，实施测试应按相同环境给出查询数、p50/p95、吞吐、错误率、RPC 次数和数据量：

- 共享角色保存的授权 DML 数量不随关联成员/分配数量增长；影响计数使用 SQL 聚合，不把全量分配加载进内存。
- 固定角色/资源数量下，成员列表每页从 20 增至 200 时授权求值/RPC 次数不线性增长；每个逻辑资源授权请求最多一次在线 IAM 求值，组合操作批量提交。
- 隔离环境至少 100 租户、每租户 1000 成员，测试身份直接/组共 4 个角色、每角色 100 操作、10 资源；20 与 100 并发分别运行 60 秒。100 并发下 IAM 在线租户求值 p95 目标不超过 200ms，无非预期授权错误。预算保留为本次验收目标；已执行隔离测量，环境与结果见VALIDATION.md。
- 平台执行现有定向读取性能/查询预算回归；不得为了达标恢复旧租户热缓存放行。资源或 SQL 性能不足时优化批量读取并重新验证，改变实时语义须重新评审。

## 9. 交付顺序

单次受控发布，开发任务按结构/契约 → 拥有者与角色 → 分配委派/消费链 → 前端 → 迁移/性能/回滚联合验收推进，不分阶段提前启用不完整授权。TASKS 的任务级依赖和退出条件完整；评审工件通过前不创建迁移 SQL 或修改公共 DTO。
