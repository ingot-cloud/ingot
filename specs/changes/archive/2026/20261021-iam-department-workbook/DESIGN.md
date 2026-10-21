# Design

## 方案摘要

在租户部门命令下新增 `/v1/tenant/departments/workbook/*`，与部门 JSON CRUD 分离。导入任务写入 `iam_department_workbook`，原始工作簿和错误报告存 OSS。导出交给下载中心。路径索引按当前可见部门树建立，导入过程中把本文件已成功创建的部门并入索引。

## 数据模型与接口

### 表 `iam_department_workbook`

| 列 | 说明 |
|---|---|
| id / tenant_id / actor_member_id | 任务、租户、发起成员 |
| kind | `IMPORT` |
| mode | `CREATE` 或 `UPDATE` |
| status | `PENDING` / `RUNNING` / `SUCCEEDED` / `FAILED` / `EXPIRED` |
| tenant_version | 登记时组织版本 |
| department_ids | 预留；导入可空 |
| result | 成功数、失败数、失败行原因 |
| workbook_path / error_report_path | `bucket/objectName` |
| failure_reason / created_at / completed_at / expires_at | UTC |

新建库写入 `004_policy_audit_migration.sql`。已有库执行 `031_department_workbook.sql`。

### 枚举与常量

- 复用 `MemberWorkbookMode`、`MemberWorkbookKind`、`MemberWorkbookArtifact`、`ExportTaskStatus`、`MemberWorkbookFile`、`MemberWorkbookTask`、`MemberWorkbookHistoryItem`
- `DepartmentWorkbookColumns`：列名、分隔符、文件名、MIME
- `DepartmentWorkbookLimits`：1000 行、历史 10 条、保留 24 小时
- `DepartmentWorkbookReasons`：逐行与整份失败说明
- `DepartmentWorkbookExportInput`：`expectedVersion` + 可选 `departmentIds`

### HTTP

挂在 `TenantDepartmentWorkbookAPI`，前缀 `/v1/tenant/departments`，避免与 `GET /{id}` 抢路径。

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `/workbook/template` | `department:create` |
| POST | `/workbook/export` | `department:export` |
| POST | `/workbook/import` | CREATE → create；UPDATE → update |
| GET | `/workbook/history` | 有 create 用 create，否则 update |
| GET | `/workbook/{id}/status` | 按任务 mode |
| GET | `/workbook/{id}/file?artifact=` | 只服务导入；`WORKBOOK` / `ERRORS` |

新增操作 `iam-tenant:department:export`（导出部门），授给租户治理模板角色。下载来源 `iam.department-workbook`，标题「部门导出」。

## 数据流与失败处理

导出：校验组织版本 → `DownloadCenter.submit` → 工作线程重验导出权限 → 按范围展开可见部门 → 树前序写 xlsx → 上传对象存储。

导入：同步预检 xlsx、表头、行数 → 上传工作簿到 OSS → 事务内写 `PENDING` → 提交后 `DelegatingSecurityContextExecutor` 逐行处理。创建行构造 `DepartmentDraft`。更新行读当前版本后写 `DepartmentUpdateInput`。一行 `BizException` 只记 `(Excel 行号, 原因)`。有可解析数据即 `SUCCEEDED`。到期任务标 `EXPIRED` 并删除对象。

## 迁移与回滚

发布顺序：DDL 与操作种子 → IAM 节点 → 管理台。回退应用可保留空表。

## 测试策略

- 模板列、须知行定位、数字归一化、行上限
- 路径唯一/未知/环、同级重名、根部门不能改上级、本文件前序可引用
- 范围求交、工作线程重新鉴权、他人任务 404、部分失败计数
