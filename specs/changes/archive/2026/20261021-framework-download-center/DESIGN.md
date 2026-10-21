# Design

## 方案摘要

框架模块 `ingot-download` 负责下载任务的通用生命周期：登记、异步生成、上传 OSS、签发链接、过期清理。任务持久化通过 SPI 交给宿主服务实现，本期只有 IAM 实现（表 `iam_download_task`）。业务方只提供「生成文件」的闭包和「来源访问校验」，不接触存储与状态机。

```text
业务导出接口 ──submit(owner, source, title, job)──▶ DownloadCenter
                                                   ├─ DownloadTaskStore.insert(PENDING)      (宿主实现，IAM)
                                                   └─ executor(带认证) ─▶ job.produce()
                                                                         ─▶ OssService.uploadFile
                                                                         ─▶ store.markSucceeded(path)
我的下载接口 ─ page / get / url ─▶ DownloadCenter ─▶ store（按 owner 过滤）
                                                 └─ DownloadSourceAccess.check(source)  下载前复核
清理调度 ──────────────────────▶ DownloadCenter.cleanup：过期删对象→EXPIRED；超时→FAILED；旧记录清除
```

## 数据模型与接口

### 框架模块 `ingot-framework/ingot-download`

包 `com.ingot.framework.download`：

| 类型 | 说明 |
|---|---|
| `DownloadTaskStatus` | 枚举 `PENDING`/`RUNNING`/`SUCCEEDED`/`FAILED`/`EXPIRED`，按 `CommonStatusEnum` 形态；位于 `ingot-commons` 的 `com.ingot.framework.commons.model.download`，与 `DownloadTask`、`DownloadLink` 一起供 API 响应复用 |
| `DownloadOwner` | record：`domain`（`AuthorizationDomain`）、`tenantId`（平台域为空）、`memberId` |
| `DownloadRequest` | record：`owner`、`source`、`title` |
| `DownloadSource` | 来源编码常量载体；业务以 `public static final String` 声明，例如 `IamDownloadSources.MEMBER_WORKBOOK = "iam.member-workbook"` |
| `DownloadFile` | record：`filename`、`contentType`、`byte[] content` |
| `DownloadJob` | 函数式接口：`DownloadFile produce()`；抛 `DownloadJobException` 时其消息作为可展示失败原因 |
| `DownloadSourceAccess` | SPI：`String source()`；`void check(DownloadOwner owner)`，无权限抛业务异常 |
| `DownloadTaskStore` | SPI：`nextId`、`insert`、`markRunning`、`markSucceeded`、`markFailed`、`find(owner, id)`、`count(owner)`、`page(owner, offset, limit)`、`listExpired(now, limit)`、`markExpired`、`failStale(createdBefore, reason, completedAt)`、`purge(before)` |
| `DownloadTask` | 只读视图：id、source、title、status、filename、fileSize、failureReason、createdAt、completedAt、expiresAt（`Instant`） |
| `DownloadCenter` | 门面：`submit`、`page`、`get`、`link`、`cleanup`；查询类失败抛 `DownloadCenterException`，携带 `DownloadCenterError`（`NOT_FOUND`/`NOT_READY`/`ACCESS_DENIED`/`LINK_UNAVAILABLE`），由宿主映射为 HTTP 错误 |
| `DownloadLink` | record：`url`、`filename`、`expiresAt` |
| `DownloadProperties` | `@ConfigurationProperties("ingot.download")` |

配置（新增，字段有 JavaDoc）：

| 键 | 类型 | 默认 |
|---|---|---|
| `ingot.download.bucket` | String | `ingot-download` |
| `ingot.download.retention` | Duration | `7d` |
| `ingot.download.record-retention` | Duration | `30d` |
| `ingot.download.link-ttl` | Duration | `5m` |
| `ingot.download.run-timeout` | Duration | `30m` |
| `ingot.download.cleanup-interval` | Duration | `1h` |
| `ingot.download.cleanup-batch-size` | int | `200` |
| `ingot.download.worker-threads` | int | `2` |
| `ingot.download.queue-capacity` | int | `100` |

对象名：`{domain 小写}/{tenantId，平台域为 -}/{memberId}/{taskId}/{filename}`；bucket 需预先创建。上传使用 `OssService.uploadFile(bucket, objectName, stream)`，入库 `bucket/objectName`。

签发：`OssService.getObjectURL(path, ttlSeconds)` 在失败时回退原值；`DownloadCenter.link` 判定返回值为空或等于入参即视为签名失败，抛 `LINK_UNAVAILABLE`，fail-closed。

执行器：模块内私有有界线程池 `DownloadWorkers`（`worker-threads` 个线程、`queue-capacity` 队列，不注册为 `Executor` Bean，避免干扰应用的默认执行器选择），包装为 `DelegatingSecurityContextExecutor`。队列满时任务标 `FAILED`（「导出任务较多，请稍后重新导出」）。

自动配置：`DownloadAutoConfiguration`，排在 MinIO/RustFS 自动配置之后，条件为存在 `DownloadTaskStore` 与 `OssService` Bean；清理由 `DownloadWorkers` 内的单线程调度器按 `cleanup-interval` 执行，随 `SmartLifecycle` 启停。

### IAM 表 `iam_download_task`

| 列 | 类型 | 说明 |
|---|---|---|
| id | bigint PK | 任务 ID |
| domain | varchar(16) | `TENANT`（预留 `PLATFORM`） |
| tenant_id | bigint | 租户 |
| actor_member_id | bigint | 发起成员 |
| source | varchar(64) | 来源编码 |
| title | varchar(128) | 展示名称，例如「成员导出」 |
| status | varchar(16) | `DownloadTaskStatus` |
| filename | varchar(255) | 下载文件名 |
| content_type | varchar(128) | MIME |
| file_size | bigint | 字节数 |
| object_path | varchar(512) | `bucket/objectName` |
| failure_reason | varchar(255) | 可展示原因 |
| created_at / started_at / completed_at / expires_at | datetime(6) | UTC |

索引：`(tenant_id, actor_member_id, created_at)`；`(status, expires_at)`。

新建库写入 IAM 初始化脚本；已有库执行 `030_download_task.sql`，附 `030_download_task.rollback.sql`。

### IAM HTTP（租户域）

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/v1/tenant/downloads?page&pageSize` | 已登录租户身份 | 当前成员任务分页，创建时间倒序；返回 `PageResponse<DownloadTask>` |
| GET | `/v1/tenant/downloads/{id}` | 同上 | 单个任务状态 |
| POST | `/v1/tenant/downloads/{id}/link` | 同上 + 来源复核 | 返回 `DownloadLink`；错误见下表 |

响应时间字段为 `Instant`，序列化 UTC `Z`。

错误映射（`TenantDownloadService`）。IAM 没有 409 类错误码，「仍在生成」复用 503 类码并给出明确提示：

| `DownloadCenterError` | IAM 错误 | 提示 |
|---|---|---|
| `NOT_FOUND`（他人、失败、过期、无文件） | `OBJECT_NOT_FOUND`（404） | 默认 |
| `NOT_READY`（排队或生成中） | `AUTHORIZATION_UNAVAILABLE`（503） | 文件仍在生成，请稍后再试 |
| `ACCESS_DENIED`（来源复核拒绝或来源未注册） | `ACTION_DENIED`（403） | 默认 |
| `LINK_UNAVAILABLE`（签名失败） | `AUTHORIZATION_UNAVAILABLE`（503） | 暂时无法生成下载链接，请稍后重试 |

### 成员表格导出改造

- `POST /v1/tenant/members/workbook/export`：请求不变；返回 `CreatedResource.id` 改为下载任务 ID。生成逻辑抽成 `DownloadJob`，source 为 `iam.member-workbook`，title「成员导出」，文件名沿用既有 `成员通讯录.xlsx`（每个任务对象路径含任务 ID，不会互相覆盖）。导出前在同一事务内校验租户设置版本，登记任务并写审计；生成阶段权限或业务失败转为 `DownloadJobException`，原因为「没有成员导出权限，请确认权限后重新导出」或「成员导出失败，请重新导出」。
- `MemberWorkbookDownloadAccess implements DownloadSourceAccess`：要求 `TENANT_MEMBER_EXPORT`。
- `iam_member_workbook` 不再写入 `EXPORT` 行；`/workbook/{id}/status` 与 `/file` 仅服务导入任务，传入导出 ID 返回 404。029 尚未发布，`member_ids` 列与 `MemberWorkbookKind.EXPORT` 保留不动，后续清理另起变更。
- 同步修订 `20261020-iam-member-batch/DESIGN.md` 中导出相关描述。

## 数据流与失败处理

1. `submit`：校验输入 → 事务内 `insert(PENDING)` 并写审计 → 提交后交执行器。执行器拒绝时标 `FAILED`。
2. 执行：`markRunning`（CAS，非 PENDING 直接返回）→ `job.produce()` → 上传 → `markSucceeded(path, size, expiresAt)`。任一步异常记 `FAILED` 与可展示原因；若上传成功但落库失败，删除对象。
3. `link`：按 owner 查任务 → 状态与过期校验 → `DownloadSourceAccess.check` → 签发。
4. `cleanup`：按批取到期 `SUCCEEDED` → 删除对象（删除失败下轮重试）→ `EXPIRED`；`PENDING/RUNNING` 且 `created_at < now - run-timeout` → `FAILED`；`created_at < now - record-retention` 的终态记录物理删除。多节点重复执行幂等。

## 迁移与回滚

发布顺序：DDL 030 → 创建 bucket → IAM 节点 → 管理台。回滚：回退应用版本即可，表可保留；需要时执行 rollback 脚本。前端旧版本仍会轮询 `/workbook/{id}/status`，因此管理台需与 IAM 同批发布。

## 测试策略

- 框架单测：状态流转、执行器拒绝、上传失败、落库失败删对象、签名回退判定失败、来源复核拒绝、清理三类分支幂等
- IAM 仓储测试：owner 过滤、分页倒序、CAS 状态更新
- 成员导出：提交返回下载任务 ID；异步线程清空上下文仍能完成（沿用既有测试手法）；失去导出权限后 `link` 被拒
- 回归：导入、导入历史、错误报告、一次性密码测试继续通过
