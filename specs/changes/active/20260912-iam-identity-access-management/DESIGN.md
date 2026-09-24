# Design

> 2026-09-15 修订：按全新系统交付，限定 IAM 授权及关联接入；框架复用、缺陷修正目标见 [REMEDIATION](./REMEDIATION.md)。下方“历史实施记录”仅说明当时实现，不覆盖当前设计。

## 1. 架构与命名

模块 ingot-pms / ingot-pms-api / ingot-pms-provider 分别更名 ingot-iam / ingot-iam-api / ingot-iam-provider。Java com.ingot.cloud.pms 更名 com.ingot.cloud.iam，RemotePms 类型更名 RemoteIam；服务注册 in-service-pms 更名 in-service-iam，网关外部前缀为 /api/iam。

Gradle、Feign contextId、服务常量、Nacos DEV/TEST/PROD dataId、镜像/Compose/环境变量、脚本和运行文档同步调整。旧路径不保留兼容代理。旧归档历史不批量替换；current 验收后按实际行为更新。所有当前运行配置中旧名均需归类检查，不把历史数据来源标识误当残留代码。

IAM 按 identity、organization、catalog、authorization、policy、audit 分域；历史 migration 工具不属于新系统运行时。Auth 保持协议及认证会话职责，Security 保持现有安全职责。字典、发号按辅助模块保留。使用本地接口/已有内部 RPC 协作，不能为了重命名扩展拆服务范围。

## 2. 逻辑数据模型

以下为持久化实体职责；DDL 的列类型、外键/逻辑引用及索引须遵守这些约束，不能恢复旧平台追加层并集模型。

| 实体 | 必要内容与约束 |
|---|---|
| Account | 全局 ID、登录标识、凭证及账号自身状态；锁定等安全事实复用既有框架端口，不重复定义其持久化；不承载租户部门 |
| PlatformMember | 独立的平台成员 ID、accountId、平台资料与状态；账号唯一；不借用默认租户成员身份 |
| Tenant / TenantMember | 组织、ownerMemberId；成员 ID、accountId、组织资料及状态；租户+账号唯一；所有者须为有效成员 |
| Department / MemberDepartment | 组织内树、成员多部门关系、主部门；禁止跨租户、循环及重复关系 |
| SubjectGroup / GroupEntry | 组明确归属 PLATFORM 或 TENANT；平台组仅引用平台成员，租户组引用本租户成员或部门及 includeDescendants；不允许嵌套或跨域引用 |
| Application / Resource / Action / Menu | 管理域、应用命名空间、合法范围/字段能力、精确操作码、导航关联；租户域应用可标记 baseline，仅这些应用在组织初始化时缺省开通 |
| TenantAppEntitlement / AppAudience | 显式开通与期限、来源；租户应用可用人群，停用强于授权 |
| RoleDefinition / RoleRevision | 归属域、SYSTEM/SHARED/PLATFORM_CUSTOM/TENANT_CUSTOM 类型、状态；不可变版本；角色分组仅展示 |
| RoleGrant | 版本中的操作与范围表达式；版本+操作唯一；范围不能脱离操作存在 |
| TenantRoleRevision / RoleDelta | 固定 sharedRevisionId、元数据覆盖、操作差异；差异类型 ADD/REMOVE/REPLACE_SCOPE |
| RoleAssignment | 主体、版本引用、范围参数、起止时间、状态、delegationGrantId、来源；同一次批量操作原子提交 |
| DelegationGrant | 授权管理员、允许角色版本、接收范围、逐操作上限及期限；由治理权限管理 |
| VisibilityPolicy / FieldPolicy | 场景、查看者、目标选择器、允许/禁止或字段规则、版本 |
| AuthorizationAudit / MigrationMapping | 可追踪变更事件；迁移批次、旧新 ID 与处置记录，均不得作为授权来源 |

角色引用采用明确的 kind + revisionId，不能用 platformRole 布尔值推测来源。逻辑 ID 在 API 中为字符串。封闭取值使用 commons 共享枚举；注解取值使用编译期命名常量。

平台域对象和租户域对象必须在引用时验证归属。操作 code 全局唯一，资源 code 在应用内唯一，角色 code 在所属域/组织内唯一。所有映射集合去重并设数据库约束。被引用的资源、操作、版本拒绝物理删除，提供停用行为。租户所有者转交是独立治理操作，不能通过普通成员编辑或停用移除最后所有者。

### 2.1 平台成员与多身份（DESIGN D01，2026-09-13 已确认）

一个 Account 可同时关联一个 PlatformMember 和多个租户的 TenantMember，不重复创建登录账号。PLATFORM 上下文必须带平台 memberId、accountId，tenantId 为空；TENANT 上下文必须带当前租户 memberId、accountId、tenantId。成员必须属于该账号和当前域，组和委派接收者也在该域内解析。ID 相同或账号相同都不构成跨域授权依据。

平台成员暂停/移出只影响平台身份；租户成员暂停/移出只影响该租户；全局账号禁用约束全部身份。身份切换通过认证流程重新建立上下文，清除旧授权视图，权限永不跨身份叠加。平台组仅包含平台成员，不新增平台部门模型；平台角色、选择器及委派不接受租户部门参数。平台成员身份本身不自动授予治理权限。

平台成员管理及平台组管理均为平台资源，拥有各自的明确 ACTION；全局账号安全管理仍不允许读取、编辑租户组织资料。平台委派 administratorMemberId 指平台成员，租户委派指当前租户成员。内部服务调用也必须校验域及成员归属。

### 2.2 身份物理结构（T01）

`databases/iam/001_identity.sql` 定义独立目标库的 Account、PlatformMember、Tenant、TenantMember、Department、MemberDepartment，以及分表保存的 PlatformGroup/TenantGroup 和成员/部门关系，共 11 张表。ID 为正数 BIGINT UNSIGNED，API 保持字符串；成员状态使用共享 MemberStatus（ACTIVE/SUSPENDED/REMOVED）。租户关系通过包含 tenant_id 的复合外键拒绝跨租户，平台组外键只引用平台成员表。

同域成员按账号唯一，移出保留成员行与历史标识。主部门使用生成列唯一约束保证最多一个；完整主部门业务规则、部门子树检查及所有者状态检查由事务服务负责。为完成组织/成员循环引用，owner_member_id 在创建事务内可暂空，但提交前必须填入有效本租户所有者；外键只保证归属，不代替最终业务校验。

表结构不包含默认账号或隐式治理授权，不挂入旧数据库初始化流程。真实登录、授权、组影响校验及原子组织初始化仍属于 T05/T08/T09，不能以结构测试代替这些任务。

### 2.3 目录、版本及分配物理结构（T01）

`002_catalog_role.sql` 定义应用、资源、精确操作、菜单引用、显式开通、人群、套餐、角色及不可变版本/参数/操作范围/差异。应用可标记 baseline，数据库拒绝把平台应用标为基础开通。应用人群的成员、部门和组关系使用独立关联表，数据库去重并校验租户归属。角色 kind 补齐已有平台自定义能力的 PLATFORM_CUSTOM；TENANT_CUSTOM 的 baseRevisionId 非空表示定制，否则为完整租户自定义。定制的基础外键限定为 SHARED，禁止多层差异继承；发布只插入新版本，不更新既有引用。

`003_assignment_delegation.sql` 定义委派管理员、固定版本白名单、接收成员/部门、逐操作上限及分配记录。两域成员/组使用独立类型化外键列，并通过 CHECK 保证恰好一个合法主体；委派来源使用域、租户和 ID 的复合外键，不能跨域或拼接来源。JSON 仅保存类型化范围表达式和参数值，其结构、资源归属及范围包含关系须由应用事务验证。委派持续生效、版本归属、受限组扩大和授权范围不能仅靠这些外键证明。

授权/委派状态为 ACTIVE/REVOKED；期限到期由时间判定，不依赖异步状态更新。分配来源为 MANUAL/INITIALIZATION/MIGRATION；组或直接分配由主体类型判定，来源委派由 delegationGrantId 判定。未传 validFrom 的分配在提交时落为当前 UTC 时间，以便计算最长分配期限。委派最长时长保存秒及纳秒两列，与 Duration 精度一致。

`004_policy_audit_migration.sql` 保存不可变默认策略引用、独立默认范围与允许/禁止规则、字段规则及事务审计事实。选择器关联按租户加成员/部门 ID 唯一；字段非 FULL 时数据库也拒绝 editable=true。审计与批次表只保证结构，不替代脱敏、可靠投递、授权等价比较及验证状态机。`005_auxiliary.sql` 当时提取了9张辅助表定义，无旧数据写入；旧ID列名不能作为继续依赖旧用户模型的理由。已有DDL合计55张表属于阶段证据，不是最终表数验收指标。按全新系统部署时应复用安全框架权威schema及适配器，明确各表唯一归属；重复/仅迁移所需结构由T01/T18盘点后处置，不在本轮文档修改中执行DDL。

## 3. 角色合成与版本

系统角色不可定制，系统治理能力不意味着业务数据全权。共享业务角色版本不可变；无需租户 adoption 副本即可在列表展示并直接分配。租户按需创建差异版本，自定义角色则保存完整本地定义。

差异合成顺序：读取固定基础版本 → REMOVE → REPLACE_SCOPE → ADD。ADD 要求基础中不存在该操作；REMOVE/REPLACE_SCOPE 要求基础中存在；同版本同操作只能有一个差异。元数据覆盖为空表示继承，恢复平台设置删除对应覆盖。差异不是全局拒绝，角色间仍按允许范围并集合并。

范围表达式首版为 ALL、SELF、MEMBER_DEPARTMENTS、MANAGED_DEPARTMENTS、OBJECT_SET。后两者通过命名参数在分配时绑定；部门表达式具有 includeDescendants。能力由资源声明，不支持任意脚本或无限嵌套布尔表达式。一个操作可以使用允许范围项的并集，共享参数类型必须一致。

发布新角色版本不自动更新授权。升级使用 oldBase/newBase/delta 三方比较；变更操作缺失、资源或范围能力不兼容为冲突，必须处理。发布预览列出受影响授权和委派；选择更新的授权逐条验证，任一失败整次操作不提交。委派的可分配版本不随发布自动更新。

平台全局停用应用/操作、角色整体停用仍约束所有旧版本。被引用版本保留。合成完整定义仅作为版本派生缓存，不按租户持久化复制；VersionedDerivedCache 的 source 必须包含基础与差异来源，version 包含全部相关修订，不能仅用单个数字版本。

## 4. 授权求值

可信上下文 = domain + tenantId(租户域必填) + accountId + memberId。租户由认证上下文验证，不能信任任意 header/query。平台服务身份也不默认 bypass。

流程：
1. 验证身份、组织及成员状态，应用全局状态、开通、有效期和可用人群。
2. 展开有效直接/组授权，验证角色版本及期限。
3. 对每条授权先合成功能和范围，再与其来源委派约束求交。
4. 同一操作合并不同完整授权的允许范围。
5. 应用场景对应的字段限制及平台强制约束；普通通讯录执行专用可见性流程。

缺功能拒绝；有功能但范围为空则列表为空，详情不可见，写拒绝。无法加载授权属于 503，不能返回成功空数据。任职变化影响 MEMBER_DEPARTMENTS，不影响显式 MANAGED_DEPARTMENTS。

委派由明确治理权限创建，不以 grantor 是否亲自持有业务操作推断资格。派生授权必须在一条委派内同时满足角色、接收者、范围和时间上限，运行时持续验证。收缩约束取交集；版本不再允许、来源到期/撤销则派生授权无效。

组成员展开后逐个验证接收者资格，组被扩大不能使超限成员受益。组编辑检查引用影响，超出操作者委派能力阻止提交。普通组管理员不能借组成员管理获得权限分配能力。

期限采用 UTC 时间，start 包含、end 不包含；null end 表示长期，但仍受来源委派期限限制。快照截止最近身份/授权/委派/应用边界，不延长已到期授权。

## 5. 资源执行接入

功能准入、数据库范围和写归属校验保持分工。资源服务注册可信适配器，描述归属关系及字段。表结构本身不自动获得保护；每个受保护 API 必须登记操作和执行路径。

- 简单归属表用租户条件及已注册字段谓词。
- 成员等多部门资源使用关联表 EXISTS，不能假设 sys_user 有 dept_id。
- 查询、count、详情、更新、删除使用相同资源操作范围；不内存过滤，不先查裸数据再仅隐藏 UI。
- INSERT 检查目标，修改归属检查旧、新两端；批量校验后同事务写入，锁定/版本校验防止归属竞态。
- 成员基本资料任一归属命中可编辑；暂停/恢复/移出覆盖全部部门；无部门不匹配部门范围。
- 调部门只检查受影响原/目标关系，不因此获得全局账号编辑权。
- 部门移动校验子树和新父节点；非空删除拒绝。
- 导出使用独立操作及字段策略；异步任务执行、结果下载再次校验。
- 成员/部门/角色/授权/组/应用配置/策略/审计等 PMS 原入口都完成资源归类，不只接 demo。
- 安全接口按已有服务职责接入新鉴权，不重写协议引擎。

批量加载角色、差异、组和规则并建索引；优先明确 SQL 和 EXISTS，暂不实施通用闭包/UNION 策略。热点按租户+归属字段建立索引并验证执行计划。

## 6. 可见性、字段与诊断

通讯录按查看者匹配：存在允许规则则取其目标并集，否则取默认范围；随后扣除禁止目标，恢复本人基础资料。必要祖先仅为导航骨架，不能提供隐藏分支人数或详情。通讯录授权不授予后台成员管理 ACTION。

字段匹配键为场景+字段+查看者+目标。无匹配用固定默认策略版本；多匹配 HIDDEN 严于 MASKED 严于 FULL，editable 取逻辑与，再受平台上限限制。修改还要求 FULL、写操作及对象范围。手机号/邮箱默认 MASKED，基础资料默认 FULL；规则只限制字段，不独立开放对象。导出采用后台字段策略，未完整可见的字段禁止原值筛选/排序/检索。

FieldAccess 由服务端计算；MASKED 只返回脱敏值，HIDDEN 不返回业务值。服务器拒绝提交不可编辑字段，不忽略以免误报成功；客户端不回传脱敏占位值。

诊断及预览复用真实引擎，结果仅返回操作者可获知的成员、对象和授权信息；不返回任意隐藏原值。无权查看详细来源时给出概括原因，不能通过诊断绕过资料范围。

## 7. 事务、缓存、审计与错误

配置写带 expectedVersion；版本冲突 409。预览只读、无授权凭证效力，提交重新计算，不能信任客户端 allowed。授权版本与数据读取得到一致快照，生成时检查相关修订，防止混合新旧配置。

使用统一 LayeredCacheBuilder，授权热缓存最长 30 秒，关闭过期 LKG 放行；事务后清本地并广播。关键治理写操作验证最新状态和版本。广播失败不会延长 expiresAt；已完成鉴权的执行中事务不承诺被追溯中断。

审计事实与业务变更同事务可靠保存，现有事件基础设施负责后续投递；记录 actor/context/target/before/after/revision/delegation/traceId，不记录凭证明文或敏感字段原值。内部 RPC 另有服务身份及目标上下文校验。

401 身份无效；403 缺操作或写越界；404 对象不存在或不可见；409 版本冲突；503 授权不可用。参数/冲突差异校验使用 400 及稳定业务错误码。

## 8. 初始化、迁移与测试

新建组织原子写入组织、所有者成员、根部门关系、共享治理角色引用和基础应用开通；默认策略只引用固定版本。创建失败不留下半组织。其他成员不批量生成重复基础角色授权。

验收遵循 [ACCEPTANCE](./ACCEPTANCE.md)，冷启动与修正目标遵循 [REMEDIATION](./REMEDIATION.md)。本次不实施历史数据迁移；MIGRATION 保留为已退出范围的历史方案。初始化只作用于显式指定的新环境，不覆盖既有业务库。

Java 新增及变更公共契约按仓库 JavaDoc 规范落盘；业务枚举、注解常量、统一缓存门禁在实施任务中执行。本次 Spec 不提前修改 current。


## 历史实施记录（2026-09-15 复评前，非最终设计）

> 以下保留过程证据，其中旧表双读、重复锁定态持久化、仅直接授权门闩及接口完成描述均须按 REMEDIATION 纠正，不授权继续保留。

### 命名与身份基础

源码、RPC 与构建已使用 IAM 名称。独立数据库由 IAM_DATABASE 显式指定，不再默认连接旧 ingot_core；IAM 连接使用 UTC 并强制数据库会话时区。此配置仅准备切换，尚未发布或迁移实际环境。

IdentityRepository 对新 iam_account / iam_platform_member / iam_tenant_member / iam_tenant 执行参数化联表查询，按可信身份限制账号、成员、租户并同时检查状态；不读取密码、不回退旧表、不合并域。ActiveIdentityService 区分身份无效与数据库不可用。标识在绑定前按目标数据库无符号整数校验，避免数据库数值隐式转换。

TenantInitializer 是内部事务流程：调用方先验证创建组织 ACTION，再从服务器基础目录生成 TenantInitializationPlan。计划不是 HTTP DTO，不接受客户端决定系统治理版本或默认策略。InitializationCatalog 读取唯一启用的租户域系统角色最新版本、最新默认策略，并由 EntitlementResolver 计算开通并集：`planApps ∪ 自选应用`，两者皆空时用 baseline，缺必开 baseline 时自动补齐。HTTP 只接收 TenantCreateInput（可带 planId 与自选 applications）。TenantInitializationService 在平台域恢复身份并校验 `iam-platform:tenant:preview|create` 后调用目录与初始化事务。预览无写入；目录表缺少展示名列时预览名称回退为应用 code。事务检查所有者账号、租户域系统治理版本、应用与默认策略版本，写入组织（含 planId）、所有者成员、根部门关系、单条治理授权、开通（PLAN/MANUAL/INITIALIZATION 及期限）与人群、固定默认引用及审计。没有复制角色/默认条目；审计失败同样回滚。


## 当前实施落点：成员上下文与生命周期（2026-09-14）

认证结果可以显式携带 AuthorizationContext，InUser 构造时检查账号、租户与成员上下文一致，禁止平台带部门及任意 IAM 身份带多租户部门映射。OnlineToken 持久化该上下文，OAuth2 自定义 JSON 恢复器和资源服务器从会话恢复；不会从 JWT 扩展字段、角色或 header 推断成员。已有 sid 带 IAM 上下文时不能换身份；新身份不能使用旧预授权部门切片，也不能进入旧账号/租户授权快照合并。非 IAM 用户保留其原协议。

ActiveIdentityService.selectAuthenticated 只供凭证认证成功后的 Auth 编排选择单一域成员；CurrentIdentityService 只读取已认证 SecurityContext，再校验新模型状态。二者均不授予 ACTION。UsernameIdentityResolver 优先加载 `iam_account`：命中后按请求 tenant（空=平台）选择成员，不合并旧授权快照；未命中或目标表不可用时回退 SysUser，旧会话没有 AuthorizationContext，不能调用新管理接口。账号安全端口与内部用户查询同样双读。社交登录在社交表的 `user_id` 能对应 `iam_account.id` 时走新账号，否则回退旧用户表。平台会话 Redis 索引对无租户会话使用租户位 `0`。

MemberLifecycle 在租户行与成员写锁下进行资格或关系变更。暂停/恢复/移出把完整部门集合交给 MemberMutationGuard；任职替换分别交付受影响旧、新关系（主部门切换同时视为两端变化）。生产路径使用 GrantPresenceMemberGuard：在同一事务可见的直接 MEMBER 分配中检查精确 ACTION 是否存在，拒绝空实现。成员与部门写路径另用 ResourceAccess 检查范围覆盖。完整引擎按分配合成操作后绑定范围并与委派上限求交。随后校验 expectedVersion、禁止移出后经 status 恢复、保护当前所有者，事务提交成员版本和审计；审计失败整体回滚。移出保留成员行和引用，不改账号及其他身份。HTTP 由 MemberCommandService 接入平台/租户 status、remove 及租户任职 PUT。

## 当前实施落点：管理命令契约与目录计划（2026-09-14）

管理请求已补齐成员/组织/部门/应用/资源/操作/菜单/套餐/开通及角色创建、发布、升级命令。升级冲突必须显式选择 ACCEPT_BASE、KEEP_DELTA 或 REPLACE_SCOPE。目标 OpenAPI 覆盖 API 第 3、4 节管理面路径；组织创建/预览与成员写入 7 个操作标记为已实现，其余仍为 false。InitializationCatalog 从目录与 EntitlementResolver 生成初始化计划；HTTP 只接收 TenantCreateInput。


## 2026-09-14 已批准的持久化与注入统一调整

用户确认本次 IAM 生产数据库操作统一迁移到 MyBatis Plus，并确认 Lambda 优先、复杂 SQL 必要时放具名 Mapper/XML。普通查询/更新使用实体字段引用；联表优先复用现有 MPJ。Service 不拼接表列名或 SQL，不引入任意 SQL 执行 Mapper。范围条件改为类型化对象，列表/count/详情/导出保持相同权限谓词。独立迁移工具、DDL、测试数据准备不属于生产访问替换范围。

新实体位于 provider 持久化包，不暴露为 API。新 Mapper 明确隔离旧租户/权限改写，由 Repository 强制可信域与租户条件，不全局关闭其他模块拦截器。保留原版本加一、事务与行锁、审计原子性、提交后失效、UTC 和字段转换；不直接使用将 Long 版本替换为时间戳的旧 @Version 行为。

Bean 默认使用 private final 与 @RequiredArgsConstructor。限定注入、继承构造或必要初始化无法可靠由 Lombok 表达时保留显式构造器并说明原因；禁止自有字段/Setter 注入以及为测试便利保留多套生产注入入口。遵循新增 spring-constructor-injection skill。

本调整已由用户以“开始实施这个计划”授权，状态为 implementing。生产访问已迁到 Mapper/Repository，业务类不再使用 `Jdbc*` 前缀。研发期间编译，整体完成后集中回归与分布式验证。

## 9. 当前修订的实施约束

1. 成熟框架只做本次必要的边界适配。账号锁定读写复用安全框架端口与用例；IAM 的 UserAccountPort 适配仅负责新 Account 数据，不反向写入 LockStatePort 造成递归或第二次状态转换。IAM 新增的重复锁定态实体/Mapper 及直接 upsert 分支退出。失败传播沿用既有框架契约，不能将查询故障伪装成无账号或吞掉锁定失败。
2. 平台认证不返回允许访问租户，兼容共享 DTO 时 allows=[]；复用 Auth 授权码/PKCE、挑战、加密、凭证检查和 Member 登录逻辑。租户切换须重建身份，不从平台管理租户列表取得业务授权。管理域由认证入口显式声明的 `domain` 传入，IAM 不再以 `tenant` 是否为空推断域；`TENANT` 且 tenant 为空是成员资格选择阶段，只返回候选而不建立 AuthorizationContext。域标识随既有 principal URI 与 `UserDetailsRequest` 传递，缺省仅用于其他既有 Auth 客户端兼容；管理台新增固定域 BFF 路由、事务和会话交接，见第10节，不改凭证校验规则。
3. 完整角色以 baseRevisionId 是否为空区分独立自定义与共享定制。发布和升级验证操作域、资源能力和参数；升级选中授权必须属于当前租户及被升级角色，逐条锁定/版本校验并重验委派和范围，任一失败全部回滚。
4. 治理授权与受限委派分开准入：无完整治理资格时必须绑定属于当前操作者的单一有效委派，不得省略来源绕过。运行时持续检查角色白名单、接收人、逐操作范围及期限；组/部门变化也须受同一约束。
5. 所有者治理授权必须与 ownerMemberId 同步。转交原子移动由所有者身份产生的治理授权，撤销旧所有者对应来源并赋予新所有者；其他独立合法授权不自动删除。锁定组织及相关成员，保护治理入口并在提交后失效。
6. 通讯录按“匹配允许目标并集替代默认 → 扣除全部禁止 → 恢复本人基础资料”求值，不依赖规则顺序。默认读取固定版本；字段合并平台最终上限；原值查询不能以本人字段权限代替所有待查询目标的权限。
7. 到期上限取授权、委派、开通等最近时间边界及最长 30 秒的最小值，每次读取检查 expiresAt；关键治理写重新求值并控制并发。修正在 IAM 消费层完成，不改变其他缓存消费者的 LKG、地板或 TTL 语义。
8. IAM HTTP 错误按 API 映射；使用 IAM 范围异常处理或明确类型映射，不能为 IAM 改写所有服务 BizException 的既有返回行为。Security 只更换必要的功能/对象授权接入，保留原业务用例与协议。
9. 预览、对象能力和诊断复用真实求值，草稿参与预览，来源披露受操作者范围限制。导出完整遍历授权数据，异步及下载重新校验身份和权限，支持多实例与状态/失败处理。
10. 具体缺陷、保留功能、清理范围和可复现验收以 REMEDIATION 为任务级清单；声明一个 DTO、Mapper 或控制器不构成该项完成证据。

## 10. 双入口 BFF 与四站点会话（2026-09-16 增量，implementing）

完整设计、接口和安全边界统一维护在 [BFF-LOGIN](./BFF-LOGIN.md)，取代此前“仅由前端传 domain、不改会话”的管理台限制。两组入口共享服务编排；LoginTransaction 与正式 BffSession 分离；Auth 扩展平台无 tenant 授权并核对绑定域；Gateway 只转发匹配应用和身份的正式会话。平台和租户分别使用独立 OAuth client 与 Auth sid。

一个 admin 源码两份部署，两个独立登录应用，四个同主域 HTTPS 子域。Gateway 按 Host 注入 appId；BFF 用该 appId 读 Nacos 注册表构造 loginUrl/completionUrl，Origin 只做一致性校验。登录结果通过短期 ticket 回到目标管理台原子完成，Cookie 不设置父域 Domain。临时事务与正式会话 Redis/Cookie 命名空间分离；绑定 Cookie 不得触发 JWT 中继。

实施使用既有安全/加密/挑战能力；新增共享枚举、公开注释及 Bean 注入遵循仓库门禁。后端按 B01–B05 实施。原已完成的底层 domain 及人工 Auth 测试保留，不视为 BFF 完成证据。

## 11. 独立测试环境与进展记录（2026-09-19）

测试数据工具、运行清单、真实接口构建和TD场景以 [TEST-DATA](./TEST-DATA.md) 为唯一方案；不新增生产测试API，不更改既有JSON/授权契约。前端真实验收使用同一runId与TD编号，数据准备和产品验收分别记录。TASKS **测试数据 D01–D05** 与本节及 §2.1 的 **DESIGN D01（平台成员与多身份，2026-09-13 已确认）** 不是同一编号。

TASKS把已有开发子项与父任务验收分开标记，并分列开发状态与自动化/真实接口/端到端；历史实施记录中的旧双读/旧入口说明不恢复为当前设计。当前完成程度依据代码与对应日期证据，不依据早期“尚未实施”标题或控制器implemented数量。
