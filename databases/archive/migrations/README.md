# 旧版数据库迁移脚本归档

原顶层 `databases/migrations/` 的 50 份 SQL 已迁入本目录，文件名、内容和升级与回滚配对原样保留。检查仓库内的构建、部署、初始化生成器和自动化测试后，未发现这些流程读取该目录。

| 脚本编号 | 历史用途 |
|---|---|
| 001–004 | Core/PMS 和 Member 账号字段、平台资源前缀、旧字典结构 |
| 005–021 | 旧安全策略中心、账号保护、安全事件、会话与策略种子，以及 Core/PMS 应用授权和菜单路径 |
| 022–026 | Core/PMS 的 RBAC、菜单权限和数据范围 |
| `rollback_*.sql` | 对应历史补丁的回滚 |

首次部署按[数据库初始化说明](../../README.md)选择服务脚本。复现历史升级时，按对应版本核对目标库和前置结构并备份，再选择具体脚本。部分脚本通过 `USE` 固定目标库，或包含删表与删列操作。目录编号跨多个服务，不能按编号批量执行。

以往验收的 SHA-256 清单保留记录时的路径。核对其中的 `databases/migrations/<文件名>` 时，对照本目录中的同名文件。

旧 PMS 文档曾计划提供 `008_application_authorization_cleanup.sql`，该文件未纳入仓库。这里的 `rollback_008.sql` 对应凭证过期字段补丁，见 [008_add_force_change_password_expiration.sql](008_add_force_change_password_expiration.sql)。
