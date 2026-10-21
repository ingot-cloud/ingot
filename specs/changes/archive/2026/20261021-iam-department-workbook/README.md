# 租户部门表格导入导出

> 状态：completed（已更新 current，已归档）

## 完成与归档（2026-10-21）

- 完成日期：2026-10-21。用户确认测试通过后更新 current 并归档。
- 当前能力：[iam/tenant-departments](../../../../current/iam/tenant-departments/README.md)
- 导出文件归 [公共下载中心](../../../../current/framework/download-center/README.md)
- 导入任务表记入 [数据结构](../../../../current/iam/database-persistence/README.md)
- 另一仓库对应归档：`ingot-admin/specs/changes/archive/2026/20261021-org-department-workbook/`
- 与原设计的差异：无。环路在工作簿层拒绝该行，单条更新仍保留自身环校验。

## 元数据

- Change ID：`20261021-iam-department-workbook`
- 领域：`iam`
- 负责人：`jy`
- 创建日期：`2026-10-21`
- 目标发布日期：`TBD`

## 目标

在部门资源下提供独立的 xlsx 模板、范围导出和逐行导入，供管理台批量新建部门和回写名称、上级、排序。导出接入下载中心。

## 范围

包含：

- 空白创建模板、按部门范围导出、导入任务、历史与错误报告
- 列：部门名称、上级部门路径、排序；更新导出多「部门 ID」
- 逐行复用 `DepartmentService.create` / `update`
- `iam-tenant:department:export`、`iam_department_workbook` 与 031 升级补丁
- 导入产物存 OSS，库内只留 `bucket/objectName`

不包含：

- 导入导出负责人、人数、导航骨架
- 自动创建中间部门、改单条创建接口的同级重名规则
- 飞书改文件名与短信验证

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：2026-10-21
- 关联提交或 PR：随本次提交
- 更新的 current capability：`specs/current/iam/tenant-departments`、`specs/current/framework/download-center`、`specs/current/iam/database-persistence`
- 与原设计的差异：无
- 取消原因：
