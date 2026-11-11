# IAM 数据库初始化

首次部署只执行 [ingot_iam.sql](../ingot_iam.sql)，即可建立 `ingot_iam` 数据库、59张运行表及全部基础数据，无需预建库或另外导入账号、应用、角色和菜单。默认平台账号为 `platform`，初始密码为 `11111111`，首次登录必须改密。

文件固定删除并重建 `ingot_iam` 库；重新执行也会清空该库全部表及数据，历史表和其他遗留表随之清除。其他数据库不受影响。执行账号须具备该库的建库、删库、建表及写入权限。

内部维护来源为 [manifest.json](./manifest.json)，完整文件由权威DDL和正式种子生成，不保存开发数据库快照。

## 新建库脚本（001–007）

| 文件 | 内容 |
|---|---|
| `001_identity.sql` | 账号、平台/租户成员、部门、两域用户组及关联；已包含套餐plan_id列。 |
| `002_catalog_role.sql` | 应用、资源、操作、菜单、开通、人群、套餐、角色及固定版本。 |
| `003_assignment_delegation.sql` | 分配、委派、人群及逐操作上限；已包含LIMITED/UNLIMITED模式。 |
| `004_policy_audit_migration.sql` | 默认策略、租户资源字段规则、成员导出/工作簿、公共下载任务、授权审计及角色版本应用任务；已包含分配创建审计索引和原007导出建表内容。平台字段权限在角色版本中保存，不再建独立策略表。 |
| `005_auxiliary.sql` | IAM字典、社交渠道配置、账号社交绑定3表；6张框架表由所属library脚本提供。 |
| `006_bootstrap.sql` | 治理目录种子，包含 5 个应用、44 个资源、181 项操作、41 个菜单、80 条菜单关联、2 个治理角色及固定版本、默认策略与发号起点。 |
| `007_initial_platform_account.sql` | 在空账号表中创建 `platform`、平台成员和平台系统管理员分配。 |

平台成员的phone/email为独立联系资料；已有库按需执行014一次性增加并回填，新建库001已包含，账号登录资料不与成员资料同步。

原007–011统一归入 [migrations](./migrations/README.md)，只按已有库实际缺项选择执行。旧平台独立策略及012/013已删除。新建库不能再遍历历史ALTER补丁；编排和测试统一读取manifest，不按目录glob猜顺序。

本次公共字段契约的已有库升级使用 [016_field_access_control.sql](./migrations/016_field_access_control.sql)，保留现有数据，转换字段能力/角色/默认策略JSON并补表结构。单独重跑006不会更新已有JSON；执行前提、旧目标范围编辑的保守转换及回退见 [迁移说明](./migrations/README.md#016-公共字段契约升级)。

已有库发布平台全局社会化配置前执行 [038_social_config_global_scope.sql](./migrations/038_social_config_global_scope.sql)。新库 005 已允许空租户归属。执行与回退见 [038 迁移说明](./migrations/README.md#038-平台全局社会化配置)。

生成及只读检查：

```sh
python3 tools/iam/generate_database.py
python3 tools/iam/generate_database.py --check
```

生成命令同时更新006和ingot_iam.sql，检查模式不写文件。底层 `generate_bootstrap.py` 仅生成正式种子；推荐用上述完整入口避免两个初始化文件不同步。修改DDL或目录生成源后重新生成，不手改生成结果。

停止使用IAM库的服务，首次部署执行：

```sh
mysql --host=127.0.0.1 --port=3306 --user=root --password < databases/ingot_iam.sql
```

连接地址和执行账号按环境调整，密码通过客户端提示输入。完整SQL自行执行 `DROP DATABASE IF EXISTS ingot_iam`、建库和选库，再建立59张运行表并写入正式目录及平台账号。仅删除阶段临时关闭外键检查，建表与种子阶段开启，结束后恢复原会话设置。DDL自动提交，首次部署失败时修正原因重跑；不能用这个文件保留或升级已有业务数据。

001–007及框架DDL是内部维护和测试来源，部署只执行完整文件。分片适用于明确选定的空库，按manifest的001–005、框架CREATE、006、007顺序使用；框架SQL只投影CREATE段。验证应用启动器或历史人工夹具时，分片导入到006即可。

种子约束：

- 完整文件只预置平台账号 `platform`，初始密码为 `11111111`，首次登录必须改密。租户及其账号由管理员手动创建。单独重跑 007 时，已有账号即跳过。
- 单独执行006时存在即跳过：按自然键判断并解析父行，保留既有业务修改。只给本次新建的治理版本初始化授权，不向已有不可变版本追加权限。完整文件会先删除旧表，不能用其保留业务修改。
- 一级 DIRECTORY 默认 `view_path = 'layout.main'`；有子菜单的二级 DIRECTORY 默认 `layout.simple`。PAGE 使用对应页面注册键；完整初始化和正式种子保持一致。
- 正式应用的10个一级目录在 `iam_menu.icon` 保存 `ep:*` Iconify名称，两管理台已离线打包该图标集。已有目录的空图标通过 [019_root_menu_icons.sql](./migrations/019_root_menu_icons.sql) 补齐；单独重跑006仍跳过既有记录。
- 正式应用编码为 `iam-platform`、`iam-tenant`、`platform-developer`、`platform-security` 和 `platform-member`。操作采用 `应用:资源:操作` 编码。既有管理页面 URL、路由名和视图键保留。
- 此目录面向新建库。用完整脚本重建隔离测试库，测试角色按新操作码重新授予。历史补丁对应其发布时的旧目录。
- IAM 与 Member 的图片暂存需要可用的私有对象存储 bucket。通过 `INGOT_OSS_ASSET_BUCKET` 配置，默认 `ingot-assets`。Member 库使用自己的 `databases/ingot_member.sql` 初始化暂存表。

运行验证（Docker使用本地MySQL镜像，不下载、不连接已有库）：

```sh
python3 tools/iam/test_database_sources.py
python3 databases/iam/test_identity_schema.py
python3 databases/iam/test_bootstrap_seed.py
python3 tools/iam/test-data/test_iam_test_data.py
```

MySQL回归比较完整文件与分片的所有表定义及种子行数，验证无库直接导入、部分初始化后及带循环外键数据的重复重建、目录默认布局、历史/额外表清除、其他数据库保留和会话外键设置恢复；初始化仅带platform管理员账号及其必须的成员/治理分配，不带测试租户、测试成员、业务运行状态或迁移元数据表。单独执行006的种子幂等及权限版本不变检查继续保留。

## 可选历史结构（不进入新项目初始化）

[legacy/README.md](./legacy/README.md) 列出显式选择的历史DDL：身份迁移批次/映射/问题3表、即时授权迁移批次/映射2表，以及没有当前运行消费者的旧套餐购买记录表。定义原样保留，manifest的legacySchemas仅供离线工具和历史验证主动读取。完整初始化不创建这些表；执行整库重建会清除原库中的这些表及016字段规则备份等遗留结构。

历史升级SQL、026固定基线和pre_021冻结夹具不变，历史迁移操作按对应手册显式选择目标和必要DDL。首次上线无需执行这些文件；不能把legacy或migrations目录加入默认初始化循环。

IAM单文件只初始化IAM及其共库框架表。启用Auth、Security或Member独立数据库时，仍需各模块自己的初始化，不能用ingot_iam.sql代替整个框架部署。

## 框架权威 DDL

共享表使用fw_前缀，每个执行SQL的library自带src/main/resources/sql/schema.sql。完整初始化已投影下列全部6表，部署时不用另行导入。

| 模块 | 表 |
|---|---|
| ingot-security-account-adapter | fw_security_account_lock_state |
| ingot-security-credential-data | fw_security_password_history、fw_security_password_expiration |
| ingot-security-event-store-mysql | fw_security_event |
| ingot-id | fw_id_leaf_alloc |
| ingot-oss-common | fw_oss_asset_upload |

路径、消费服务和独立接入方式统一见[databases/README.md](../README.md)。IAM005只维护iam_dictionary、iam_social_config和iam_account_social_binding；iam_download_task仍是IAM对下载框架存储接口的实现。不能根据“在IAM库中”推断代码所有权。

完整文件按重建ingot_iam → IAM结构 → 5份框架DDL → 正式目录 → 默认平台账号的顺序生成。框架脚本本身不选库、不删表、不写运行数据。

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

[人工验收](../../specs/changes/archive/2026/20260912-iam-identity-access-management/RESOURCE-EXTENSION-VERIFICATION.md) 和 [接入样板](../../examples/iam-ops/README.md) 已按唯一角色字段闭环更新。

## 字段字典与主键评估

正式运行清单当前为59张表、501个字段（包含6张框架共库表）。字段说明只在权威DDL维护，完整初始化与[数据字典](../../docs/modules/iam/DATA-DICTIONARY.md)由`generate_database.py`同步生成；[逐表主键索引评估](../../docs/modules/iam/DATABASE-REVIEW.md)由`generate_database_review.py`生成。接收成员表新增Leaf显式主键；密码历史精简重复索引，公共API保持原样。已有库见[027/028升级说明](./migrations/README.md#027028-接收成员主键与密码历史索引)。

已有库只补注释时，先阅读[026升级说明](./migrations/README.md#026-字段说明升级)。不要导入完整初始化文件，也不要直接绕过预检导入026。注释工具不连接任何默认库，必须显式选择目标并提供mysql配置文件；默认仅只读核对全部表，`--apply`才修改注释。

```sh
python3 tools/iam/generate_database.py --check
python3 tools/iam/generate_database_review.py --check
python3 tools/iam/test_schema_comments.py
python3 tools/iam/test_schema_comments_mysql.py
```
