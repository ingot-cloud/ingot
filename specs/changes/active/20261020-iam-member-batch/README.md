# 租户成员表格导入导出

> 状态：implementing

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

- 完成日期：
- 关联提交或 PR：
- 更新的 current capability：
- 与原设计的差异：
- 取消原因：
