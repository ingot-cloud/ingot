# 需求与验收

- 新项目只需导入databases/ingot_iam.sql，默认包含59张运行保留表、正式目录及唯一platform管理员。11111111的BCrypt哈希与must_change_password=TRUE不变。
- 移出iam_migration_batch/mapping/issue、iam_live_authorization_batch/mapping与sys_tenant_plan_record；定义原样保存在可选legacy目录，manifest单列legacySchemas，默认初始化绝不创建或删除这些表。
- sys_social_details、sys_user_social、iam_member_export等有效表保留，不能按名称判断退役。
- 已有数据库与历史升级脚本不变；旧迁移场景通过显式导入可选DDL工作。旧版026预检按照其固定基线检查，不用新项目表集替代。
- 维护仍从权威源生成单文件，不手改bundle；字典、索引评估、结构快照和建库说明保持一致。
- 验收：生成检查、离线单测、隔离MySQL新库/重复导入/保留非清单表/种子/外键、可选历史DDL约束、历史升级及迁移回归通过。实际库操作与产品验收不包含在自动化证据内。
