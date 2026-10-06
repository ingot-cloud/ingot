# IAM 已有库升级补丁

原databases/iam根目录的007–011保留文件名迁入本目录；新建库使用001–006及manifest，不能把本目录全部执行。下表仅说明已有库何时需要补齐，实际执行仍由运维明确选择目标库。

| 文件 | 执行前提 | 新建库对应来源 |
|---|---|---|
| 007_member_export.sql | 还没有iam_member_export表；一次建表。 | 004 |
| 008_platform_accounts_menu.sql | 历史目录缺少全局账号菜单；按该补丁原菜单约定补齐。 | 006正式菜单 |
| 009_tenant_plan.sql | iam_tenant尚无plan_id列；一次ALTER。 | 001 |
| 010_assignment_audit_index.sql | 分配审计索引缺失；重复执行会检查并跳过。 | 004 |
| 011_delegation_duration_mode.sql | 委派表仍是旧LIMITED结构、尚无assignment_duration_mode列；一次ALTER。 | 003 |

保留原补丁语义，不将不同库状态下的ALTER拼成统一升级。011发布顺序仍为DDL→全部IAM节点→前端，回退前处理UNLIMITED委派和长期分配。

补目录/菜单可按最新006执行，但不会向已有固定治理版本追加新操作；新增治理操作通过显式发布、分配新版本获得。业务数据和授权配置不通过完整ingot_iam.sql重建。

## 平台字段模型直接替换

平台资源字段策略专用012/013迁移已删除。框架尚未投产，平台角色字段结构只维护最新权威DDL及正式初始化，测试库按上级README由用户重建；不保留旧平台模型兼容。上述其他补丁与租户字段策略不受此决策影响。
