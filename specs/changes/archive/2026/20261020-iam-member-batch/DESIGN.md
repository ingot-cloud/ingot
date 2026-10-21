# Design

## 方案摘要

在租户成员命令下新增 `/v1/tenant/members/workbook/*`，与共享 JSON 导出表分离。任务写入 `iam_member_workbook`，状态机复用 `ExportTaskStatus`，文件以 Base64 放进 `R` 信封。导入导出用 EasyExcel。部门路径由当前可见部门树在服务端索引。创建行走 `TenantMemberOnboarding`，修改行走现有资料补丁与任职替换。

## 数据模型与接口

### 表 `iam_member_workbook`

| 列 | 说明 |
|---|---|
| id / tenant_id / actor_member_id | 任务、租户、发起成员 |
| kind | `EXPORT` 或 `IMPORT` |
| mode | `CREATE` 或 `UPDATE` |
| status | `PENDING` / `RUNNING` / `SUCCEEDED` / `FAILED` / `EXPIRED` |
| tenant_version | 登记时组织版本 |
| member_ids | 导出范围快照，JSON 数组 |
| result | 成功数、失败数、失败行原因；不含密码明文 |
| workbook / error_report / password_file | MediumBlob；密码列仅创建导入使用 |
| password_downloaded | 一次性密码是否已取走 |
| failure_reason / created_at / completed_at / expires_at | 与 JSON 导出相同语义，UTC |

新建库写入 `004_policy_audit_migration.sql`。已有库执行 `029_member_workbook.sql`。026 注释基线不重放。

### 枚举

- `MemberWorkbookKind`：`EXPORT`、`IMPORT`
- `MemberWorkbookMode`：`CREATE`、`UPDATE`
- `MemberWorkbookArtifact`：`WORKBOOK`、`ERRORS`、`PASSWORDS`

### HTTP

| 方法 | 路径 | 权限 | 说明 |
|---|---|---|---|
| GET | `/v1/tenant/members/workbook/template` | `member:create` | 空白创建模板 |
| POST | `/v1/tenant/members/workbook/export` | `member:export` | 登记更新导出；`memberIds` 空或省略表示当前可见全部未移出成员。返回的 ID 为下载中心任务 ID，结果在「我的下载」查看，见 [20261021-framework-download-center](../../archive/2026/20261021-framework-download-center/DESIGN.md) |
| GET | `/v1/tenant/members/workbook/{id}/status` | 按任务 kind/mode | 轮询；含成功/失败数与密码是否可下载 |
| GET | `/v1/tenant/members/workbook/{id}/file` | 同上 | `artifact=WORKBOOK\|ERRORS\|PASSWORDS` |
| POST | `/v1/tenant/members/workbook/import` | 创建或更新/任职 | `multipart file` + `mode` |
| GET | `/v1/tenant/members/workbook/history` | 创建或更新/任职 | 当前操作者最近 10 条导入 |

任务只对发起成员可见。过期后标 `EXPIRED`，文件不可下载。

列名常量：`成员 ID`、`姓名`、`手机号`、`邮箱`、`部门`。路径分隔 `/`，多部门分隔 `,`。行上限 `MemberWorkbookLimits.MAX_DATA_ROWS = 1000`。保留 `Duration.ofHours(24)`。

## 数据流与失败处理

导出：校验组织版本 → 交给下载中心登记任务（`iam_download_task`，不再写 `iam_member_workbook` 的 EXPORT 行）→ 异步按范围投影未移出成员 → 按字段可见性写 xlsx → 上传对象存储。`/workbook/{id}/status` 与 `/file` 仅服务导入任务。下载时由下载中心复核导出操作。

导入：校验文件与行数 → 登记 PENDING → 异步逐行提交。创建行构造 `TenantMemberCreateInput`。更新行读当前版本后写 `MemberProfileInput`（邮箱键出现且空白即清空）和 `MemberDepartmentInput`（部门键出现且空白即空数组）。拥有者若部门列发生变化则该行失败。一行异常只记入错误报告，不回滚其他行。有任意成功行则任务 SUCCEEDED，结果带失败明细；整份无法解析则 FAILED。

密码仅写入 `password_file`。`PASSWORDS` 首次成功读取后清空并置 `password_downloaded`。历史 `result` 只有计数。

## 迁移与回滚

发布顺序：DDL → IAM 节点 → 管理台。回退应用可保留空表；不要用完整 `ingot_iam.sql` 重建已有库。

## 测试策略

- 路径解析：唯一命中、同名歧义、未知路径、根部门、多部门顺序
- 导出：隐藏列省略、脱敏值带星号、已移出排除、范围 ID 求交
- 导入：创建/加入/重新加入、更新清空、拥有者任职失败、脱敏写回失败、行上限、部分失败
- JSON 导出既有测试继续通过
