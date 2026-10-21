# 租户成员表格导入导出

> 状态：completed（已更新 current，已归档）

## 完成与归档（2026-10-21）

- 完成日期：2026-10-21。用户确认成员批量处理验证成功后更新 current 并归档。
- 当前能力：[iam/tenant-members](../../../../current/iam/tenant-members/README.md)
- 导出文件归 [公共下载中心](../../../../current/framework/download-center/README.md)
- 另一仓库对应归档：`ingot-admin/specs/changes/archive/2026/20261020-org-member-batch/`
- 与原设计的差异：导出不再写入本表，改为下载中心任务；可解析导入在逐行结束后即 `SUCCEEDED`，成功数为 0 同样成功，只有执行异常才 `FAILED`

## 元数据

- Change ID：`20261020-iam-member-batch`
- 领域：`iam`
- 负责人：`jy`
- 创建日期：`2026-10-21`
- 目标发布日期：`TBD`

## 目标

在现有 JSON 成员导出之外，提供独立的 xlsx 模板、范围导出、逐行导入任务，供管理台批量导入新成员和回写资料、任职。

## 范围

包含：

- 空白创建模板、范围导出工作簿、导入任务、历史与错误报告/一次性密码下载
- EasyExcel 读写；部门路径按当前可见部门树解析
- 逐行提交，复用入职、资料更新、任职替换
- `iam_member_workbook` 表与 029 升级补丁

不包含：

- 改动现有 `POST/GET /v1/tenant/members/export*` JSON 契约
- 部门页批量导入导出
- 自动新建部门、改账号状态、重置密码、管理员二次验证

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：2026-10-21
- 关联提交或 PR：随本次提交
- 更新的 current capability：`specs/current/iam/tenant-members`；导入任务表归属记入 `specs/current/iam/database-persistence`
- 与原设计的差异：导出改为下载中心，不再写 `iam_member_workbook` 的 EXPORT 行，`kind` 仍保留该取值；逐行处理结束即成功，不要求至少一行成功
- 取消原因：
