# 公共下载中心

> 能力域：`framework` / `download-center`

## 摘要

导出提交后立即返回。文件由下载中心异步生成并写入对象存储，按域、租户和成员隔离。本人在保留期内查看状态并下载，后续导出接入同一套任务，不再各自保存文件内容。

## 边界

- **含**：任务登记、异步生成、OSS 上传、短时签名链接、过期清理、来源权限复核；IAM 租户域「我的下载」；成员表格 xlsx 导出。
- **不含**：成员 JSON 导出；导入任务、错误报告和一次性密码；平台域下载接口；手动删除与重新生成。

## 所有者

- 契约：`ingot-framework/ingot-download`、`ingot-commons` 的 `com.ingot.framework.commons.model.download`
- 存储与接口：`ingot-iam-provider`（表 `iam_download_task`）

## 关联模块

- 对象存储：`OssService`，库内只保存 `bucket/objectName`
- 第一个接入方：成员表格导出 `iam.member-workbook`

## 文档索引

- [SPEC](./SPEC.md)
- 来源变更：[20261021-framework-download-center](../../../changes/archive/2026/20261021-framework-download-center/README.md)
