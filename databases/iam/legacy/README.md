# 可选历史结构

这些文件不属于新项目运行表清单，完整ingot_iam.sql不会创建或删除其中的表。需要历史迁移、原库核对或恢复演练时才显式选择；已有库已有相同表时不应重复导入。

| 文件 | 用途 |
|---|---|
| 001_identity_migration.sql | 历史身份迁移批次、人工映射和问题处置3表 |
| 002_tenant_live_authorization.sql | 租户即时授权迁移批次与原行映射2表 |
| 003_tenant_plan_record.sql | 已无在线消费者的旧组织套餐购买事实 |

定义从004/005原样迁入，不带数据、不删除表、不切换数据库，不执行自动升级。保留不代表仍启用旧业务实现；已有数据的保留、迁移和清除仍按专项处理。

工具通过database_sources.legacy_schema_files()显式读取manifest.legacySchemas。运行初始化仍只读取schemas/frameworkSchemas/bootstrap/initialAccounts。

新建历史迁移演练环境时，在隔离目标库完成所需运行结构后，按本表选择对应文件再运行离线工具。历史升级脚本及其固定基线不随当前初始化变更。
