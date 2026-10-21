# IAM 数据结构与关系持久化

manifest 管理 IAM 共库的 65 张表和 525 个字段。中文说明在权威 DDL 维护，关系保存按差量执行；委派接收成员关系使用 IAM Leaf 显式主键。下载任务表 `iam_download_task` 归 [公共下载中心](../../framework/download-center/README.md)。

- 负责人：jy；验收日期：2026-10-20。
- 关联模块：`ingot-iam-provider`、`ingot-security-credential-data`、`ingot-id`。
- [当前结构与写入规则](SPEC.md)
- [数据字典](../../../../docs/modules/iam/DATA-DICTIONARY.md)
- [主键与索引评估](../../../../docs/modules/iam/DATABASE-REVIEW.md)
- [字段说明与关系写入验收](../../../changes/archive/2026/20261020-iam-schema-relations/README.md)
- [接收成员主键与密码索引验收](../../../changes/archive/2026/20261020-iam-recipient-primary-key-password-index/README.md)
