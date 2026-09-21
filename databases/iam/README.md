# IAM 目标结构

仅供 `20260912-iam-identity-access-management` 的隔离目标库使用，尚未接入服务初始化或正式切换脚本。

- `001_identity.sql`：账号、独立平台成员、租户成员、部门、多部门关系、两域组与组成员。无默认业务数据，不读取或覆盖旧表。
- `002_catalog_role.sql`：应用、资源、精确操作、菜单、显式开通、人群、套餐、角色版本与差异。租户域应用可标记 `baseline`，仅这些应用在组织初始化时缺省开通；平台应用不能标记为基础开通。
- `003_assignment_delegation.sql`：受限委派、接收人群、操作上限、固定版本白名单及角色分配。
- `004_policy_audit_migration.sql`：默认策略引用、通讯录/字段规则、审计和迁移批次/映射/处置。
- `005_auxiliary.sql`：保留字典、发号、社会化、历史安全事件和套餐记录的结构。来源为仓库 SQL 的 CREATE TABLE，仅用于目标结构，不复制任何 INSERT 数据。
- `007_member_export.sql`：租户成员导出任务状态、成员 ID 快照与过期时间，供多实例读取，不保存字段原值。
- `006_bootstrap.sql`：唯一的正式冷启动种子，写入两个治理应用、资源与字段能力、107 个精确操作、平台与组织菜单树、每域一个 SYSTEM 治理角色及其固定版本授权、默认策略版本和发号高水位。由 `python3 tools/iam/generate_bootstrap.py` 从 change 的 `contracts/routes.json` 生成，不要手工编辑。
- `seed-manual-verification.sql`：隔离环境人工认证种子，不是生产数据，不进入迁移导入。必须先执行 001–005、`007_member_export.sql`、下列框架 DDL 与 `006_bootstrap.sql`；它只补两个可登录账号、一个平台成员和一条治理授权，目录与治理角色一律复用冷启动种子，避免同一份目录出现两个来源。

`006_bootstrap.sql` 是数据而非结构，编号只表示执行顺序：`test_identity_schema.py` 只加载 DDL，把它排除在外，避免种子行占用结构夹具的标识。种子的三条硬约束：

- **不含任何账号与凭证。** 受控平台账号由 provider 的冷启动器经安全框架注册用例创建，配置开关 `ingot.iam.bootstrap` 默认关闭。
- **存在即跳过。** 每条语句按自然键（应用/资源/操作 code、菜单 route_name、角色 domain+code、策略 kind+revision）判断，父行一律由自然键解析，保留标识只用于本次新建的行。重复执行不覆盖任何人工或业务修改。
- **保留标识全部小于发号起点 1000000。** 运行时发号不会与种子标识冲突。

## 框架权威 DDL（不在本目录重复定义）

安全框架自带下列表的权威结构与持久化实现，IAM 只消费其公开端口，不新增同表实体或 Mapper。表可以部署在 IAM 所用数据库，代码所有权仍归框架：

| 表 | 权威 DDL | 持久化归属 |
|---|---|---|
| `account_lock_state` | `ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/resources/sql/account_lock_state.sql` | `LockStatePort` / `DefaultLockStatePortAdapter` |
| `password_history`、`password_expiration` | `ingot-framework/ingot-security/ingot-security-credential-data/src/main/resources/sql/add_password_history.sql` | security-credential 既有用例 |

建库顺序：001–005 → `007_member_export.sql` → 上述框架 DDL → `006_bootstrap.sql` → 可选的人工认证种子。`security_event` 暂无框架 DDL 文件，结构继续由 `005_auxiliary.sql` 提供，写入仍由 security-event-store-mysql 负责。

独立测试环境编排见 `tools/iam/test-data/`（测试数据 D01/D02）：`prepare` / `build` / `verify` / `reset`。只接受已登记的独立库，不猜测开发库；`reset` 必须 `--confirm-reset`，且不会对任意库执行 DROP。`build` 走真实 `/iam/v1` 接口，口令不进报告。联调逐步操作见 change 内 `VERIFICATION-GUIDE.md`。

运行测试：`python3 databases/iam/test_identity_schema.py` 校验结构约束，`python3 databases/iam/test_bootstrap_seed.py` 校验冷启动种子的完整性、幂等和组织初始化前置条件。均需要 Docker；不自动下载镜像。

目标数据库使用 MySQL 8.0.16+，连接时区为 UTC。ID 使用正数 BIGINT UNSIGNED，API 使用字符串传输。成员状态对应 commons 的 MemberStatus：ACTIVE/SUSPENDED/REMOVED。独立的平台成员表和平台组关联表不引用租户成员。租户关联使用包含 tenant_id 的复合外键。

数据库保证同账号可在不同域拥有成员、同域成员唯一、关系不跨域、最多一个主部门及引用不被级联删除。以下仍由应用事务实现，不能以 DDL 通过替代业务验收：

- 组织创建提交前必须绑定有效所有者并完成根部门、治理授权和基础开通；owner_member_id 只允许在创建事务中暂空。
- 所有者不能通过普通成员操作被暂停或移出；转交锁定组织及相关成员并检查版本。
- 部门移动检查完整子树循环、旧/新范围，非空部门不能删除。
- 暂停或移出成员触发授权失效；全局账号停用由认证及授权运行时检查。
- 组修改验证授权/委派引用影响；直接数据库外键只证明归属，不证明操作者权限。
- 角色版本不可变、角色与操作同域、差异 ADD/REMOVE/REPLACE_SCOPE 的基础存在性，以及授权与委派逐维度包含关系，由服务校验；DDL 只限制合法引用及结构。
- 迁移批次即使具备 verified_at，也须验证所有阻塞项和权限比较后才能标为 VERIFIED；数据库字段约束不替代流程门禁。审计 JSON 由服务白名单构造，不得写入凭证或敏感字段原值。
- 辅助表旧列 user_id/tenant_id/app_id/plan_id 分别映射新 Account/Tenant/Application/Plan；保持列名不等于可以跳过引用验证。社会化密钥和发号高水位需单独演练。

账号登录名目标约束为全局唯一。源快照中软删除账号占用同名、格式不兼容或关联不明确时，由迁移工具报告并要求显式处置，不自动丢弃账号或覆盖凭证。
