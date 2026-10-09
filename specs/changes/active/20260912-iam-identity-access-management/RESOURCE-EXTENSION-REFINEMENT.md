# 通用资源、多应用授权、字段策略与分配升级增量

> 状态：validating（开发及自动化完成，人工验收待执行）
> 批准：2026-10-05，用户「开始实施本计划」。主 change 保持 implementing。

> 历史设计说明：本文平台独立字段策略及012迁移已被角色字段增量和用户批准的测试阶段直接替换决策替代。对应旧代码、接口、专用表及页面已删除；下文旧实现证据仅作变更历史，不是当前接入/部署指南。

## 需求与边界

同一授权引擎支持 IAM 进程内与独立服务。资源适配器按 domain/applicationCode/resourceCode 注册，目录的关联元数据为唯一归属依据。自定义精确操作不要求加入 IamAction 枚举；现有 IamAccess 与旧权限注解保持兼容。平台不新增部门模型。字段策略独立于角色与分配，仅限制已获准访问的对象；本轮新增平台配置，现有租户字段策略不迁移。

## 授权与适配契约

- 公共 SDK 提供 ActionDescriptor、ResourceKey、ResourceDescriptor、ResourceObjectProvider、AuthorizationDecision、ScopeTarget、FieldPolicyDecision、RequireIamAction。操作的 MUTATING/READ_ONLY 来自服务器注册，不接受浏览器选择。
- ResourceDescriptor 声明精确操作、范围能力、对象标识、树形能力及字段默认值。对象候选、少量回显、批量存在性与目标校验统一接口，禁止客户端提供 SQL 或服务地址。内置查询可保留私有 SQL 实现，但公共解析不依赖固定资源枚举。
- 多条授权取范围并集，来源委派上限取交集。跨服务传递完整合取条款，不压缩成丢失语义的 self/dept 平铺集合。默认空范围；未知资源/能力明确拒绝，远端故障 503。
- 内部 v2 求值 RPC 只恢复认证中的 AuthorizationContext，禁止请求替代用户或域。资源服务调用求值；对象 provider RPC 只接受受信任 IAM 服务与原始身份传递。服务路由来自配置白名单。
- 读授权最长热窗口 30 秒，并限制到最近授权/委派/身份截止；写鉴权 fresh，关闭过期 LKG/地板放行。已通过鉴权的执行中事务沿用现有边界，不承诺跨服务事务追溯中断。
- 查询/count/详情/写入使用相同类型化范围。创建验证新归属，改变归属验证旧、新两端。无法给出可靠目标语义时不支持该范围。

## 平台字段策略

独立表 iam_resource_field_policy，resource_id 唯一，保存 domain、tenant_id（平台为空）、policy JSON、version、UTC created_at/updated_at；JSON 保存 defaults 与 rules。规则包含 fieldKey、allViewers、viewerMembers、viewerGroups、targetScope、scopeBindings、visibility、editable。默认策略由已注册字段提供，允许显式默认覆盖，但不得超过目录与后端能力。有效匹配规则替代默认，多匹配取 HIDDEN > MASKED > FULL，editable 取 AND，最后施加字段能力上限。

平台端入口「资源字段策略」按应用/资源选择；资源详情提供快捷入口。注册字段键与实际 DTO 投影显式绑定。后端输出前隐藏/脱敏，写入要求 FULL 且 editable，禁止回传脱敏占位。原值查询、排序、导出同样检查，不以当前页样本证明全域可见。列表按请求批量加载策略与组匹配，不逐行 RPC。首个真实资源为平台成员，独立服务示例为 iam-ops 资源。

接口（统一 IAM /v1 前缀）：

- GET /platform/applications/{applicationId}/resources/{resourceId}/execution：注册状态与能力。
- GET、PUT /platform/applications/{applicationId}/resources/{resourceId}/field-policy。
- POST 同路径 /field-policy/preview；PUT 请求 {expectedVersion, policy}。
- 新精确操作 iam-platform:resource-field-policy:read|preview|update，分别鉴权并检查目标资源范围；候选提供最小信息。
- POST /inner/authorization/v2/evaluate，返回当前身份的精确操作、完整对象范围、字段规则、版本与 expiresAt；旧 snapshot 不改变。

## 平台分配版本升级

- POST /v1/platform/assignments/upgrade/preview 与 /upgrade；一次批次同角色，输入 targetRevisionRef、items[{id,expectedVersion,scopeBindings}]。新版本必须同角色、已发布、比当前版本新。
- 预览输出操作与范围差异、缺失参数、每条结果及阻止原因；预览无授权凭证效力，草稿改变后失效。
- 保留分配 ID、主体、createdAt、来源、validFrom/validUntil。ACTIVE 且未到期、来源有效的记录可以升级；未来生效可保留，已到期/撤销不能恢复。
- 精确操作 iam-platform:assignment:upgrade；完整治理来源可直接升级，纯受限管理员自动获得限定升级入口，仅可处理本人单条委派派生记录。目标固定版本须已在同一委派白名单，逐操作、主体及期限全部重验。
- 委派版本升级分步：先保留旧并加入新及上限，升级分配，再移除旧；不自动修改委派。
- 按现有锁序事务重验，expectedVersion 冲突 409，任一失败整批回滚；审计记录前后版本和范围，不记录敏感原值，提交后失效缓存。
- 角色工作区入口与角色分配单条/批量入口，共享左侧步骤抽屉和统一实体选择器。目标默认最新已发布版本，提交固定该 ID，不能临时改成后来发布的版本。

## 兼容、实施与验收

新增表和版本化 RPC；先服务端后消费端。不改写不可变历史角色版本，新治理操作通过正式目录及显式版本发布获得。现有租户字段与共享基础升级保持兼容。开发完成与人工验收分别标注，不提前更新 current，不提交代码。

- RE01：公共 SDK、操作准入与完整范围契约，编译/单元/身份及故障回归。
- RE02：适配器注册与内置资源迁移，跨应用同名隔离、树分页、回显、校验与诊断。
- RE03：内部 v2、本地/远程实现、缓存与批量求值，对比结果及失效/过期。
- RE04：字段存储、治理接口、成员执行接入，HTTP/MySQL、读写、脱敏、筛选、组变化回归。
- RE05：平台字段策略页面与统一选择器，组件、类型、lint、依赖边界与构建。
- RE06：分配升级、资格与事务审计，真实 HTTP/MySQL、委派/并发/原子性回归。
- RE07：升级步骤页面与差异/参数/预览交互，组件与两管理台构建。
- RE08：自定义资源参考、iam-ops 独立样板及冷启动目录/契约同步。
- RE09：三类管理员人工验收、性能及页面证据；通过后更新 current 并归档。


## 最终接口与实现记录（2026-10-05）

| 能力 | 实现与边界 |
|---|---|
| 通用资源执行 | `ingot-access-control` SDK；ResourceRegistry 使用完整资源键。IAM 内置 SQL 是私有 provider，平台编辑器、绑定校验、诊断及关系回显统一按目录关联解析；独立应用可注册本地 Bean 或服务器白名单中的远程 provider。 |
| 跨服务求值 | 版本化内部 v2 返回完整 OR/AND 范围及字段结论；写操作 fresh，读缓存最长30秒且每次检查绝对截止和身份。统一分层缓存接收既有授权失效事件；无 LKG/地板放行。 |
| 对象 RPC | 固定 `/inner/iam/resource-objects/query`；服务发现名和描述来自部署配置。INNER 认证、原身份、覆盖原始 JSON 的 HMAC-SHA256 同时验证，时间窗30秒，密钥至少32字节。候选页最大200；故障503。 |
| 字段执行 | 目录和注册描述共同收紧可见、编辑、原值搜索及排序能力；查看者匹配规则替代默认，多匹配取严格值。平台成员列表、详情、组成员、导出及写入实际接入；独立服务示例使用同一处理器。 |
| 分配升级 | 单条及同角色批量命令最多100条；未来生效可升级，已撤销、到期或来源失效不可恢复。事务内按授权→角色→分配顺序锁定重验，条件更新与审计、失效通知统一。候选在 SQL 中先筛更高版本再计数分页。 |
| 前端 | 新资源字段策略页面和资源详情入口；升级四步抽屉、逐记录参数、差异、预览与保存，保留原绑定。角色分配列表单条/批量及角色关联来源入口按逐条能力显示。 |
| 扩展资料 | `examples/iam-ops` 可编译 UUID 工单服务样板及部署配置；扩展现有 in-framework 的独立 reference，不创建新 skill。 |

补充候选接口：

- `GET /v1/platform/resource-field-policies/candidates`：kind=APPLICATION/RESOURCE/MEMBER/GROUP/OBJECT；资源字段策略 READ 独立鉴权，应用按可治理资源过滤，不能借候选泄露其他资源。资源上下文也提供 `/field-policy/candidates`。
- `GET /v1/platform/assignments/upgrade/role-candidates`：assignmentId 必填，roleId 展开版本；keyword 搜索，ids 为少量回显，page/pageSize 分页。同角色且比当前记录更高的已发布版本，受限来源再取白名单交集。
- `GET /v1/platform/assignments/upgrade/candidates`：assignmentId、kind 必填；ROLE_REVISION 用 ids 读取本角色完整版本资料；OBJECT 用 revisionId、parameterKey 定位目标参数。revisionId 与共享范围选择器一致。支持 keyword、ids、page/pageSize、tree、parentId。旧版本资料仅用于差异核对，不代表可提交为目标。
- 内部契约单独生成 `contracts/internal-openapi.json`；管理面维持133路径/201操作，内部2操作不进入浏览器权限目录。

`HasAnyAuthority` 保留原 authority 门禁；`IamAccess` 是 IAM 进程内身份、治理/委派资格与发号门面。自定义业务接口使用 `AuthorizationAccess` / `@RequireIamAction`，精确注解只负责操作准入。对象 SQL、真实目标与字段投影/写入必须显式执行，不由注解自动完成。

### 交付与兼容

新增 `migrations/012_resource_field_policy.sql`，先迁移表和补目录、部署 IAM，再部署 SDK 业务服务与管理台。正式目录为2应用/35资源/134操作/25菜单；重复执行006只补目录，不往已有不可变角色版本追加新权限。新库初始化角色包含新操作；已有库需显式发布并分配含字段 read/preview/update 和 assignment:upgrade 的版本。旧RPC及租户字段执行路径保持兼容；本轮未修改开发库或线上库。

### 自动化证据

- 后端全量：SDK11、commons56、IAM provider339，共406项，0失败/0跳过；开启 `IAM_MYSQL_HTTP_TEST=true`。含升级保留记录及原子冲突、委派目标白名单/预览后来源变化、跨应用隔离、字段 HTTP/MySQL 保存与投影、版本分页、HMAC身份及故障测试。
- 收尾后33项升级/HTTP/MySQL定向回归通过，补充验证共享选择器revisionId正确传入升级对象查询；无失败/跳过。
- 全量 provider 夹具在同一 JVM 会超过默认512m堆。本轮使用临时 Gradle init 将测试堆设为1024m、每6个类重新 fork；未修改业务或仓库构建配置绕过检查。SDK样板编译通过。
- 隔离 MySQL 结构22项、冷启动种子10项通过，覆盖新增表、重复执行及历史角色版本不变。
- 前端 admin-common91、platform114，共205项；组件覆盖草稿预览失效、迟到响应、升级结果按记录ID对齐及字段上限。两包类型检查、改动文件只读lint、依赖边界、平台及组织管理台构建通过。
- Java契约夹具、管理面生成检查、内部生成检查、7项公开契约检查通过；前端权威副本及SHA-256同步。
- HTTP/MySQL测试使用可信身份/资格测试替身驱动真实控制器、服务与数据库，不能代替完整登录链路或已部署跨服务联调。

RE01–RE08 标记开发完成；RE09 保持待验收。具体命令和三类管理员步骤见 [RESOURCE-EXTENSION-VERIFICATION](./RESOURCE-EXTENSION-VERIFICATION.md)。页面视觉、部署后的跨服务/多节点失效、实际请求性能及真实身份人工验收尚未执行，不提前更新 current 或归档。


## 2026-10-05 平台字段策略替代说明

本记录保留原独立策略实现及测试历史；RE04/RE05 的平台成员/组字段策略配置被 [角色固定版本字段权限增量](ROLE-FIELD-AUTHORIZATION-REFINEMENT.md) 替代。新模型同一操作/目标采用 FULL > MASKED > HIDDEN，权限必须保持本来源范围，不再按成员/组另设覆盖。验收以 [调整后的人工清单](RESOURCE-EXTENSION-VERIFICATION.md) 为准，租户流程仍用原限制语义。RE09 继续未验收。
