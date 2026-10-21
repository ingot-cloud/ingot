# Tasks

## 实施任务

- [x] T1：新建 `ingot-framework/ingot-download` 模块并在 `settings.gradle`、`config/ingot.gradle` 登记（仓库无 BOM）
  - 依赖：无
  - 验收：模块可编译；`DownloadTaskStatus`、SPI、`DownloadProperties` 带 JavaDoc
- [x] T2：实现 `DownloadCenter`（submit/page/get/link/cleanup）与自动配置
  - 依赖：T1
  - 验收：框架单测覆盖 DESIGN 测试策略中的框架项
- [x] T3：IAM 新增 `iam_download_task`：初始化脚本、`030_download_task.sql` 与 rollback、实体、Mapper、`DownloadTaskStore` 实现
  - 依赖：T1
  - 验收：仓储测试通过；字段注释齐全
- [x] T4：IAM 新增 `/v1/tenant/downloads` 列表、详情、链接接口
  - 依赖：T2、T3
  - 验收：按人隔离、他人 404、时间 UTC `Z`
- [x] T5：成员表格导出接入下载中心，新增 `MemberWorkbookDownloadAccess`
  - 依赖：T4
  - 验收：导出返回下载任务 ID；导入链路与测试不变
- [x] T6：修订 `20261020-iam-member-batch/DESIGN.md` 导出描述，配置示例补 `ingot.download.*`
  - 依赖：T5
  - 验收：文档与实现一致

## 验证任务

- [x] V1：`ingot-download`、IAM 模块测试通过（`-I tools/iam/tenant_live/verification.gradle`：download 13、commons 70、IAM 592 项，28 项条件跳过，0 失败）
- [x] V2：用户确认验收完成

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新（新增 `framework/download-center`）
- [x] Change 已记录完成信息并归档
