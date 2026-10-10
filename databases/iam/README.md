# IAM 建库与升级脚本

正式初始化的唯一来源为 [manifest.json](./manifest.json)，完整导入文件为 [ingot_iam.sql](../ingot_iam.sql)。该文件由权威DDL与正式种子生成，可在明确选定的测试库重复执行；每次都会清空并重建清单内全部表，不是保留数据的升级脚本，不保存开发数据库快照。

## 新建库脚本（001–006）

| 文件 | 内容 |
|---|---|
| `001_identity.sql` | 账号、平台/租户成员、部门、两域用户组及关联；已包含套餐plan_id列。 |
| `002_catalog_role.sql` | 应用、资源、操作、菜单、开通、人群、套餐、角色及固定版本。 |
| `003_assignment_delegation.sql` | 分配、委派、人群及逐操作上限；已包含LIMITED/UNLIMITED模式。 |
| `004_policy_audit_migration.sql` | 默认策略、租户通讯录/字段规则、导出任务、审计及迁移记录；已包含分配创建审计索引和原007导出建表内容。平台字段权限在角色版本中保存，不再建独立策略表。 |
| `005_auxiliary.sql` | 字典、发号、社会化、历史安全事件及套餐辅助结构；不带开发数据。 |
| `006_bootstrap.sql` | 唯一正式种子：3应用、36资源、138操作、30菜单、54菜单关联、2治理角色及固定版本、默认策略与发号起点。 |

平台成员的phone/email为独立联系资料；已有库按需执行014一次性增加并回填，新建库001已包含，账号登录资料不与成员资料同步。

原007–011统一归入 [migrations](./migrations/README.md)，只按已有库实际缺项选择执行。旧平台独立策略及012/013已删除。新建库不能再遍历历史ALTER补丁；编排和测试统一读取manifest，不按目录glob猜顺序。

本次公共字段契约的已有库升级使用 [016_field_access_control.sql](./migrations/016_field_access_control.sql)，保留现有数据，转换字段能力/角色/默认策略JSON并补表结构。单独重跑006不会更新已有JSON；执行前提、旧目标范围编辑的保守转换及回退见 [迁移说明](./migrations/README.md#016-公共字段契约升级)。

生成及只读检查：

```sh
python3 tools/iam/generate_database.py
python3 tools/iam/generate_database.py --check
```

生成命令同时更新006和ingot_iam.sql，检查模式不写文件。底层 `generate_bootstrap.py` 仅生成正式种子；推荐用上述完整入口避免两个初始化文件不同步。修改DDL或目录生成源后重新生成，不手改生成结果。

停止使用目标库的服务，确认目标库并备份需要保留的数据，再导入：

```sh
mysql --database=ingot_iam < databases/ingot_iam.sql
```

库须事先创建，连接/凭证按本机配置提供。完整SQL先按逆序对manifest内56张表执行 `DROP TABLE IF EXISTS`，然后建表并写入正式治理目录；不创建/删除数据库、不切换数据库、不删除清单外表。循环外键要求仅删除阶段临时关闭外键检查，建表与种子阶段开启，结束后恢复原会话设置。DDL无法整体回滚，导入失败后修正原因重新执行；需要恢复原数据时使用备份。

001–005权威结构源本身不执行重置，仅完整文件负责删除编排。分片仅适用于空库，按manifest的001–005→框架CREATE→006顺序；框架源中的固定USE不能原样执行到其他库，优先使用已投影到当前库的完整文件。

种子约束：

- 不含账号、凭证、业务组织、角色分配、审计或登录运行状态。受控平台首账号通过 `ingot.iam.bootstrap` 启动器及安全框架注册用例创建，开关默认关闭。
- 单独执行006时存在即跳过：按自然键判断并解析父行，保留既有业务修改。只给本次新建的治理版本初始化授权，不向已有不可变版本追加权限。完整文件会先删除旧表，不能用其保留业务修改。
- 新建的DIRECTORY菜单默认 `view_path = 'layout.main'`，PAGE使用对应页面注册键；完整初始化和正式种子保持一致。
- 平台治理应用 `iam-platform` 的“平台管理”包含全局账号、平台人员、角色与授权。独立平台应用 `platform:develop` 为开发者平台，包含生成二维码、客户端管理、社交管理、业务ID管理，沿用 `platform.develop.*` 页面键。租户治理仍为 `iam-tenant`，不包含平台开发者功能。
- 社交/业务ID目录位于开发者应用，API仍使用原 `iam-platform:social-config:*` / `iam-platform:id-allocation:*` 的8个精确操作码；客户端沿用Auth注解的6个 `platform:develop:client:*` 精确操作码；星号仅是文档概括，种子不存通配码。菜单和操作同应用关联。新建平台治理版本覆盖全部平台正式应用，租户治理仅覆盖租户域，不因菜单存在自动授予其他成员权限。
- 保留标识小于发号起点1000000。原ingot_iam.sql开发快照已移出正式初始化，可从Git历史查阅。
- `seed-manual-verification.sql` 仅供隔离人工测试；每次完整初始化后按需单独执行一次，不进入完整文件或默认编排。它创建platform/owner测试账号、平台成员及治理分配；也可改用受控平台首账号初始化器，两种方式选择一种。

运行验证（Docker使用本地MySQL镜像，不下载、不连接已有库）：

```sh
python3 tools/iam/test_database_sources.py
python3 databases/iam/test_identity_schema.py
python3 databases/iam/test_bootstrap_seed.py
python3 tools/iam/test-data/test_iam_test_data.py
```

MySQL回归比较完整文件与分片的所有表定义及种子行数，验证首次、部分初始化后及带循环外键数据的重复导入、目录默认布局、无关表保留和会话外键设置恢复；初始化结果不带账号或运行状态。单独执行006的种子幂等及权限版本不变检查继续保留。

## 框架权威 DDL（不在本目录重复定义）

安全框架自带下列表的权威结构与持久化实现，IAM 只消费其公开端口，不新增同表实体或 Mapper。表可以部署在 IAM 所用数据库，代码所有权仍归框架：

| 表 | 权威 DDL | 持久化归属 |
|---|---|---|
| `account_lock_state` | `ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/resources/sql/account_lock_state.sql` | `LockStatePort` / `DefaultLockStatePortAdapter` |
| `password_history`、`password_expiration` | `ingot-framework/ingot-security/ingot-security-credential-data/src/main/resources/sql/add_password_history.sql` | security-credential 既有用例 |

建库顺序以manifest为准：重建清单表 → 001–005 → 上述框架表CREATE → 006 → 可选人工认证种子；导出表已合并在004。`security_event` 暂无框架 DDL 文件，结构继续由 `005_auxiliary.sql` 提供，写入仍由 security-event-store-mysql 负责。

独立测试环境编排见 `tools/iam/test-data/`（测试数据 D01/D02）：`prepare` / `build` / `verify` / `reset`。只接受已登记的独立库，不猜测开发库；`reset` 必须 `--confirm-reset`，且不会对任意库执行 DROP。`build` 走真实 `/iam/v1` 接口，口令不进报告。联调逐步操作见 change 内 `VERIFICATION-GUIDE.md`。

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

已有库升级脚本保留在migrations子目录；菜单、套餐列、审计索引及期限模式的最新结果已纳入权威结构/正式种子。详细前置条件见migrations/README.md，不能把整个目录无差别用于新库。

## 平台角色字段部署

框架未投产，平台角色字段权限是唯一模型，不保留平台独立成员/组策略表、API或迁移开关。用户在确认目标库后重建测试库，使用最新 `databases/ingot_iam.sql` 并导入测试身份，部署相同版本的IAM、SDK和前端；本次修改未操作实际业务库。

角色版本字段JSON为非空对象。正式治理版本显式冻结成员字段默认：phone/email脱敏，displayName/avatar完整可见及可编辑；目录和注册能力仍可收窄。字段完整可见须发布并分配角色，治理身份不绕过。未声明字段隐藏，缺失快照拒绝求值；角色发布不自动升级已有分配。租户字段策略与通讯录结构保留。

[人工验收](../../specs/changes/active/20260912-iam-identity-access-management/RESOURCE-EXTENSION-VERIFICATION.md) 和 [接入样板](../../examples/iam-ops/README.md) 已按唯一角色字段闭环更新。
