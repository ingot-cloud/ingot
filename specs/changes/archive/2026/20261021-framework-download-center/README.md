# 公共下载中心

> 状态：completed（已更新 current，已归档）

## 元数据

- Change ID：`20261021-framework-download-center`
- 领域：`framework`、`iam`
- 负责人：`jy`
- 创建日期：`2026-10-21`
- 目标发布日期：`TBD`

## 目标

导出改为「提交即返回」，结果统一沉淀到当前登录人的「我的下载」：可查看历史、状态，并在保留期内下载。后续所有导出都接入同一套任务与存储，不再各自建表存 BLOB。

## 范围

包含：

- 新框架模块 `ingot-framework/ingot-download`：任务登记、异步执行（携带认证）、文件上传 OSS、签发下载链接、过期清理、来源权限复核 SPI
- IAM 落地任务存储 `iam_download_task` 与租户域「我的下载」接口
- 成员表格导出（xlsx）作为第一个接入方，改为写入下载中心

不包含：

- 成员 JSON 导出（`/v1/tenant/members/export*`）迁移
- 导入任务、错误报告、一次性密码文件（仍留在 `iam_member_workbook` 与「导入历史」）
- 平台域「我的下载」；其他服务经内部接口登记任务
- 下载次数统计、手动删除、重新生成

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 关联变更

- 依赖并修改 [租户成员表格导入导出](../20261020-iam-member-batch/README.md) 的导出部分；该变更已于 2026-10-21 验收归档
- 前端：`ingot-admin/specs/changes/archive/2026/20261021-common-download-center/`

## 完成记录

- 完成日期：2026-10-21。用户确认验收后更新 current 并归档。
- 关联提交或 PR：随本次提交
- 更新的 current capability：[framework/download-center](../../../../current/framework/download-center/README.md)、[iam/database-persistence](../../../../current/iam/database-persistence/README.md)
- 与原设计的差异：任务仍在生成时返回 503「文件仍在生成，请稍后再试」，IAM 无 409 类错误码；分页参数为 `pageSize`；导出文件名沿用 `成员通讯录.xlsx`；生成使用模块私有有界线程池
- 取消原因：
