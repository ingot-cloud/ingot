# 接口

权威来源为 Java DTO 导出及控制器路由，生成在 [IAM 契约](../../../active/20260912-iam-identity-access-management/contracts/openapi.json)，前端镜像在 `20260913-base-iam-admin/sources/contracts/`。以下路径是 IAM 服务路径，管理台通过 `/api/iam` 网关前缀调用。所有响应沿用 R、ResourceDetail 和 PageResponse；ID、版本锁及大计数字段沿用字符串契约，时间为 UTC ISO-8601。

## 版本信息

管理根路径为 `/v1/platform/roles`、`/v1/platform/shared-roles`、`/v1/tenant/roles`。各自新增：

| 方法与相对路径 | 输入/结果 | 资格 |
|---|---|---|
| GET `/{id}/revisions/{revisionId}` | 固定版本详情，包含名称、备注、metadataVersion、publishedAt、字段权限及固定共享基础 | 既有角色 READ |
| GET `/{id}/revisions/{revisionId}/grants` | 该固定版本的完整授权展示和编辑快照 | 既有角色 READ |
| PATCH `/{id}/revisions/{revisionId}/metadata` | expectedMetadataVersion、versionName、remark；返回 CreatedResource 的新元数据锁 | 既有角色 STATUS 更新资格 |

创建、权限发布、租户共享基础升级增加可选 `versionName`（最多128）、`remark`（最多512）。权限发布增加可选 `sourceRevisionId`，预览接口增加同名可选查询参数；必须属于当前角色，租户发布采用该来源的固定共享基础。空名称和旧版本回退 vN，名称不唯一；系统角色不允许元数据写入。PATCH 只更新元数据锁及审计，权限版本、角色权限内容、分配和授权缓存不变；锁冲突返回既有冲突错误，不覆盖其他管理员修改。

## 平台版本应用

根路径 `/v1/platform/roles/{roleId}/version-applications`，仅平台自定义角色。全部能力要求独立 `iam-platform:assignment:upgrade`，同时验证角色 READ 和对象边界；发布组合接口还校验既有角色发布资格。系统/共享/租户角色不接入此执行入口。

| 方法与相对路径 | 输入/结果 |
|---|---|
| POST 根路径 | RoleApplicationPrepareInput：targetRevisionId、selection、sourceRevisionIds/assignmentIds、idempotencyKey；返回 RoleApplicationTask |
| POST `/publish` | RolePublishApplicationInput：publish（完整发布输入）、selection、sourceRevisionIds/assignmentIds、idempotencyKey；同事务发布并准备，返回任务 id 和 targetRevisionId |
| GET 根路径 | page/pageSize；分页任务记录 |
| GET `/candidates` | 同一角色下受治理边界限制的分配候选分页 |
| GET `/distribution` | ACTIVE、未到期（含未来生效）分配的固定版本分布 |
| GET `/{id}` | 状态、计划锁、目标版本、各明细状态数量、暂停原因和时间 |
| GET `/{id}/items` | page/pageSize；原分配、配置范围、预览和错误明细 |
| PATCH `/{id}/items` | expectedVersion、all 或 itemIds、scopeBindings 部分覆盖、可选 excluded；返回重新预检的计划 |
| POST `/{id}/preflight` | expectedVersion；仅重新读取未完成项并预检，返回最新计划，等待确认 |
| POST `/{id}/confirm` | expectedVersion、idempotencyKey；再次 fresh 全部预检，READY 时进入异步 RUNNING |

分页默认1/20，最大200；执行批次固定最多100。selection 为 ALL、SOURCE_REVISIONS、ASSIGNMENTS；后两者分别要求来源版本或分配 ID 集合。集合由后端游标快照，直接与组分配都是原分配记录，不展开组成员。ALL/来源选择必须覆盖所选有效集合的治理范围；准备键按角色去重，身份、目标及选择不同不能复用。确认键只允许同一次计划确认幂等重放，锁冲突不强制覆盖。

all 与 itemIds 必须明确二选一；scopeBindings 只覆盖明确给出的参数，兼容旧值保留，目标不使用的键移除，新增/类型或关联资源变化的参数必须补齐。不填范围不等于授权全部。excluded=true 明确缩小集合；重新纳入仍经过预检。

任务状态 PREPARING、READY、BLOCKED、RUNNING、PAUSED、SUCCEEDED；明细状态 PENDING、BLOCKED、SUCCEEDED、EXCLUDED、REVOKED、EXPIRED、ALREADY_TARGET。预检冲突返回 BLOCKED 任务，分配零更新；发布组合时新版本保留。工作故障/撤权/并发冲突回滚当前批次并 PAUSED，已提交批次保留；重新预检未完成项后必须再次确认。所有计数及明细采用当前读取治理边界，不泄露隐藏对象。

预览比较操作、范围表达式、参数绑定及字段权限，高低版本都经过同一门禁。旧“升级分配”接口保持最多100条且仅向更高版本升级。新任务更新与审计同事务，提交后复用既有授权和字段缓存通知，其他角色贡献的权限继续参与授权合并。
