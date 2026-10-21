# 公共下载中心 SPEC

> 记录当前已验收的下载任务事实。不复述实施过程。

## 1. 任务

一条任务属于一个 `DownloadOwner`：授权域、租户（平台域为空）、成员。状态为 `PENDING`、`RUNNING`、`SUCCEEDED`、`FAILED`、`EXPIRED`。已成功但 `expiresAt` 已过的任务，查询时按 `EXPIRED` 返回。

列表和详情不返回对象路径。时间点为 UTC `Instant`，接口序列化为带 `Z` 的 ISO-8601。

## 2. 生命周期

`submit` 在事务内登记 `PENDING`。事务提交后交给模块私有有界线程池执行，并传播提交时的认证上下文。该线程池不是应用的 `Executor` Bean。

执行顺序：`markRunning`（仅 `PENDING` 可进入）→ 生成文件 → 上传 → `markSucceeded`。生成失败、保存失败、队列已满、执行超时分别记可展示原因：「文件生成失败，请重新导出」「文件保存失败，请重新导出」「导出任务较多，请稍后重新导出」「任务执行超时，请重新导出」。业务可通过 `DownloadJobException` 写入自己的原因。上传已成功但结果落库失败时删除对象。

对象名：`{domain 小写}/{tenantId，平台域为 -}/{memberId}/{taskId}/{filename}`。bucket 需预先存在，默认 `ingot-download`。

## 3. 下载与清理

签发前确认任务属于当前成员、状态为 `SUCCEEDED`、未过期且有对象路径，再按来源复核权限。签名结果为空或等于原路径时拒绝签发。

IAM 租户接口：

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/v1/tenant/downloads?page&pageSize` | 当前成员，创建时间倒序 |
| GET | `/v1/tenant/downloads/{id}` | 单个任务；他人为 404 |
| POST | `/v1/tenant/downloads/{id}/link` | 短时链接，默认 5 分钟 |

错误：不存在、失败、过期或无文件为 `ObjectNotFound`（404）；仍在生成为 `AuthorizationUnavailable`（503），提示「文件仍在生成，请稍后再试」；来源无权限为 `ActionDenied`（403）；签名失败同样为 503，提示「暂时无法生成下载链接，请稍后重试」。

清理周期默认 1 小时：到期成功任务先删对象再标 `EXPIRED`；创建超过 30 分钟仍未结束的标失败；失败和过期记录默认保留 30 天后删除。成功文件默认保留 7 天。

## 4. 成员表格导出

`POST /v1/tenant/members/workbook/export` 请求不变。返回的 ID 是下载任务 ID，来源 `iam.member-workbook`，标题「成员导出」，文件名 `成员通讯录.xlsx`。登记前在同一事务内校验租户设置版本并写审计。

`/workbook/{id}/status` 与 `/file` 只服务导入。导入、错误报告和一次性密码仍在 `iam_member_workbook`。
