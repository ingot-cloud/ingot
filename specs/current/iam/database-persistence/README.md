# IAM 数据结构与关系持久化

manifest 管理 IAM 共库的 59 张表和 501 个字段，其中 53 张由 IAM 维护，6 张由框架模块维护。中文说明在权威 DDL 维护，关系保存按差量执行；委派接收成员关系使用 IAM Leaf 显式主键。下载任务表 `iam_download_task` 归 [公共下载中心](../../framework/download-center/README.md)。成员导入任务表 `iam_member_workbook` 归 [租户成员](../tenant-members/README.md)。部门导入任务表 `iam_department_workbook` 归 [租户部门](../tenant-departments/README.md)。

- 负责人：jy；验收日期：2026-11-09。
- 关联模块：`ingot-iam-provider`、`ingot-security-credential-data`、`ingot-id`。
- [当前结构与写入规则](SPEC.md)
- [数据库初始化及依赖库脚本索引](../../../../databases/README.md)
- [数据字典](../../../../docs/modules/iam/DATA-DICTIONARY.md)
- [主键与索引评估](../../../../docs/modules/iam/DATABASE-REVIEW.md)
- [字段说明与关系写入验收](../../../changes/archive/2026/20261020-iam-schema-relations/README.md)
- [接收成员主键与密码索引验收](../../../changes/archive/2026/20261020-iam-recipient-primary-key-password-index/README.md)
- [租户角色分配覆盖索引实现与迁移验证](../../../changes/archive/2026/20261109-iam-post-login-read-performance/VALIDATION.md#2026-11-10-租户分配列表-10-万条分页)
- [037 运行库列顺序和分页现场验证](../../../changes/archive/2026/20261110-iam-post-login-release-acceptance/VALIDATION.md)

- [按资源存储的租户字段策略](../tenant-field-policy/SPEC.md)
