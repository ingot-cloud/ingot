# IAM 已有库升级补丁

原databases/iam根目录的007–011保留文件名迁入本目录；新建库使用001–006及manifest，不能把本目录全部执行。下表仅说明已有库何时需要补齐，实际执行仍由运维明确选择目标库。

| 文件 | 执行前提 | 新建库对应来源 |
|---|---|---|
| 007_member_export.sql | 还没有iam_member_export表；一次建表。 | 004 |
| 008_platform_accounts_menu.sql | 历史目录缺少全局账号菜单；按该补丁原菜单约定补齐。 | 006正式菜单 |
| 009_tenant_plan.sql | iam_tenant尚无plan_id列；一次ALTER。 | 001 |
| 010_assignment_audit_index.sql | 分配审计索引缺失；重复执行会检查并跳过。 | 004 |
| 011_delegation_duration_mode.sql | 委派表仍是旧LIMITED结构、尚无assignment_duration_mode列；一次ALTER。 | 003 |
| 015_menu_advanced_configuration.sql | iam_menu 尚无 hidden/is_cache/props/route_params 列；一次 ALTER，保留现有数据。 | 002 |
| 014_platform_member_contacts.sql | 平台成员尚无phone/email列；停IAM写入后一次增加并从账号回填，后续不再同步。 | 001 |
| 016_field_access_control.sql | 资源仍有sortable/缺mask、角色字段与FIELD默认版本仍为旧JSON，或租户策略缺operation_rules；保留数据并转换，支持重跑。 | 002/004/006 |
| 017_role_revision_application.sql | 角色版本缺名称、备注、独立元数据锁，或缺持久版本应用任务表；只增加列、表及索引，支持重跑。 | 002/003/004 |
| 018_authorization_audit_menu.sql | 两域授权审计菜单仍使用旧名称；仅调整名称，可重复执行。 | 006 |
| 020_platform_capability_completion.sql | 平台能力补齐：新增安全应用、迁移六资源十六操作及八个页面，保留 ID/固定授权；先停写备份。 | 006 |
| 021_tenant_live_authorization.sql | 当前角色授权结构扩展；仅已有库。 | 002/003 |
| 022_tenant_live_authorization_constraints.sql | 当前角色分配约束；正转换成功后安装。 | 003 |
| 024_department_leaders.sql | 新增负责人关系及独立维护操作；不回填角色或分配。 | 001/006 |
| 023_tenant_members_departments_menus.sql | 组织管理下「成员与部门」收成目录，下面挂「成员」「部门」两个子页面；重挂父级、补菜单与操作关联。 | 006 |
| 019_root_menu_icons.sql | 正式应用一级目录的icon为空；仅补齐空图标，可重复执行并保留自定义图标。 | 006 |

014执行顺序为迁移→后端→前端；仅用于缺少两列的已有库，不能重复执行或用于新建库。DDL自动提交；若在DDL后中断，保持服务停止，确认列已新增后单独完成脚本回填语句。回退应用前冻结平台成员联系资料编辑，保留新增列和备份，不将独立联系资料写回账号。已有库禁止用完整初始化SQL替代迁移。

保留原补丁语义，不将不同库状态下的ALTER拼成统一升级。011发布顺序仍为DDL→全部IAM节点→前端，回退前处理UNLIMITED委派和长期分配。

补目录/菜单可按最新006执行，但不会向已有固定治理版本追加新操作；新增治理操作通过显式发布、分配新版本获得。业务数据和授权配置不通过完整ingot_iam.sql重建。

## 平台字段模型直接替换

平台资源字段策略专用012/013迁移已删除。框架尚未投产，不保留旧平台模型运行时兼容。新建库使用最新权威DDL及正式初始化；本次用户明确要求保留既有数据时，使用下述016离线转换公共字段契约。上述其他补丁不受此决策影响。

015 发布顺序：增量 DDL → IAM 节点 → 管理台。已有路径不改写，旧菜单默认不隐藏、不缓存、不透传；回退应用时保留新增列。该脚本仅执行一次，不用于新建库。

## 016 公共字段契约升级

本次用户要求保留已有测试身份及授权数据，新增离线契约转换；不恢复012/013专用模型，不增加旧协议运行时兼容。006的已有记录仍然跳过，不能替代016。

停止全部IAM节点及其他目标库写入，备份整个所选IAM库，停机至少30秒使字段分层缓存到期。确认库名后执行（连接凭证按本机配置提供；不要使用`--force`忽略SQL错误）：

```sh
mysql --default-character-set=utf8mb4 --database=ingot_iam < databases/iam/migrations/016_field_access_control.sql
```

该脚本不写死库名、不执行006、不删除业务身份或分配、不追加操作授权；需要CREATE ROUTINE、ALTER、CREATE及DML权限。它将资源sortable移除并补PHONE/EMAIL/ALL脱敏、补平台只读时间能力，转换所有角色固定版本及所有FIELD默认版本，补operation_rules并移除旧editable列/约束。保持可见性、角色ID/版本ID/固定版本引用、原角色操作范围及租户隐私目标规则。既有自定义角色不自动获得新时间字段权限，原MASKED字段的不可编辑保持关闭；筛选由原目录声明、实际绑定及完整查询范围FULL门禁继续约束。

原租户编辑规则的目标范围与新版全局操作规则不能完全等价。仅原FULL/可编辑且目标为ALL或空范围的MANAGEMENT规则允许迁移为UPDATE可编辑，其他规则对同一查看者关闭编辑；不从行级规则新增CREATE授权。原查看者成员/部门及下级标志完整转换。原规则与转换结果留存于`iam_field_access_016_legacy_rule`，执行结果第二个结果集列出需要重新配置的有限目标规则；管理员在新字段策略页面审核后再调整。DIRECTORY可见性保留且不转编辑授权。

同批部署新IAM、SDK消费服务及前端，迁移完成后启动全部节点，再验证资源列表、成员context、角色读取及租户策略保存。SQL直接转换冻结版本的数据格式，停机期间完成，不能一边运行旧节点一边迁移。重复执行不改已有新JSON、不重复追加操作规则；留档表验收前保留。

数据转换处于一个事务内，异常会回滚；DDL会自动提交，不能整体ROLLBACK。中断时保持服务停止，修正原因后完整重跑；整体回退需要恢复迁移前数据库备份并回退整批应用。不要用`ingot_iam.sql`回退，它会重建并清空业务表。

隔离验证：`python3 databases/iam/test_field_access_migration.py`，只使用自动清理且不联网的MySQL容器，不连接已有库。

## 017 角色版本信息与批量应用

适用于已完成现有 IAM 契约升级的库，MySQL 8.0.16 及以上。备份并明确选择目标库后执行；连接凭证使用本机配置，不用 `--force` 忽略错误：

```sh
mysql --default-character-set=utf8mb4 --database=ingot_iam < databases/iam/migrations/017_role_revision_application.sql
```

脚本需要 CREATE ROUTINE、ALTER、CREATE 权限，增加 `iam_role_revision.version_name/remark/metadata_version`、分配查询索引及 `iam_role_application_task/item`。历史权限 JSON、角色和版本 ID、系统版本号、分配和范围不改写；未命名版本继续显示 vN。DDL 自动提交，中断后修正原因再完整重跑。006 是目录初始化，不能补齐这里的表结构；新建库使用最新权威 DDL 或生成的 `ingot_iam.sql`，不再执行此补丁。

发布顺序为数据库增量 → 全部后端节点 → 前端。不要在旧表结构上先启动新后端。新接口沿用现有角色更新和分配升级资格，不需要向治理角色自动追加操作授权。

回退前先停止新任务确认入口，并在所有 IAM 节点设置 `ingot.iam.role-applications.worker-enabled=false`，重启配置生效后确认旧进程全部退出、当前批次事务已结束。由运维在备份后将剩余 RUNNING 任务标记 PAUSED，清除租约并递增任务乐观锁；记录回退原因，保留已提交明细、所有新增表列及审计，然后回退应用。关闭浏览器不会停止任务，不得依赖租约到期代替停止全部领取器。需要恢复已经完成的授权调整时，使用新应用流程选择原固定版本与范围，不直接修改历史版本或删除任务记录。

隔离验证：`python3 databases/iam/test_role_revision_application_migration.py`。测试使用自动清理、无网络的 MySQL 8.4 容器，验证重复迁移保留数据以及同一角色最多一个 RUNNING 任务；不连接现有库。


## 018 授权审计菜单名称

`018_authorization_audit_menu.sql` 将平台和组织的同一授权审计菜单名称改为“授权审计”，可重复执行。仅更新限定应用与路由的 name，不改权限、路径或历史审计，无结构迁移。新库种子已包含新名称。按目标库执行该脚本后重新 bootstrap/登录加载菜单；回退名称可分别恢复平台“事件与审计”及租户“审计”。审计格式兼容由新后端读取和写入代码完成，无需修改历史记录。

## 019 一级目录 Iconify 图标

适用于 `iam-platform`、`iam-tenant`、`platform:develop` 三个正式应用的9个一级目录。按应用编码、路由名、`parent_id IS NULL` 与 `kind = 'DIRECTORY'` 定位，只填充NULL、空字符串或纯空格的icon，保留已有图标、子菜单、其他应用、路径及权限关联。实际补齐的菜单version递增一次，重复执行无额外修改。图标均来自两管理台已离线打包的ep集合。

确认目标库并留存目标菜单原始icon/version后执行：

```sh
mysql --default-character-set=utf8mb4 --database=ingot_iam < databases/iam/migrations/019_root_menu_icons.sql
```

完成后重新bootstrap或登录加载菜单。需要回退时按菜单ID恢复留存的原始icon，并递增当前version；不重建数据库。新建库使用最新006或完整初始化文件，已有库不能用完整初始化文件代替本补齐脚本。定向隔离验证使用 `python3 databases/iam/test_bootstrap_seed.py BootstrapSeedTest.test_root_menu_icons BootstrapSeedTest.test_root_menu_icon_patch_is_scoped_and_preserves_custom_icons`。


## 020 平台能力与页面归属

本补丁仅用于已有 IAM 库；新库使用最新001–006或初始化 bundle，不重放所有补丁。停止所有写入并完整备份目标库，禁止 mysql --force。脚本输出页面新增/迁移清单及资源移动 ID；应用域、重复资源、路径/注册键冲突、未知操作、跨应用自定义菜单关联会 SIGNAL，整笔业务更新回滚。临时外键桥接不关闭检查，普通角色与固定版本不变。

先用同一脚本预演（事务最后回滚），再检查清单执行：

```sh
{ printf 'SET @iam_capability_dry_run=TRUE;\n'; cat databases/iam/migrations/020_platform_capability_completion.sql; } | mysql --default-character-set=utf8mb4 --database=ingot_iam
mysql --default-character-set=utf8mb4 --database=ingot_iam < databases/iam/migrations/020_platform_capability_completion.sql
```

直接切换新 URL，不注册旧入口。发布顺序：备份停写→补丁→同批部署 IAM/Security/两管理台→广播授权失效、至少等待30秒并重新 bootstrap。回滚停止新版写入，按备份和输出 ID 恢复目录及菜单操作关联并回退应用，不重建库、不改历史角色版本；升级后已新增业务写入时先审查备份恢复范围。

隔离验证：`python3 databases/iam/test_capability_migration.py`，使用无网络自动清理 MySQL 容器，不访问现有库。脚本由 `tools/iam/generate_database.py` 生成；`--check` 同时检查种子、迁移和 bundle。

## 023 成员与部门拆为目录下的两个子页面

适用于已有「成员与部门」单页菜单的 IAM 库，也兼容曾把「成员」「部门」直接挂在组织管理下的中间态。新增目录 `org.iam.members.root`（`view_path=layout.simple`），把 `org.iam.members` 收成「成员」、补齐 `org.iam.departments`「部门」，并改挂到该目录下。目录绑定 `member:read` 与 `department:read`，成员页只保留 `member:read`。用户组、组织设置仍挂在组织管理下，排序回到 2、3。按应用编码和路由名定位，不改已有菜单主键。可重复执行。

确认目标库并留存目标菜单后执行：

```sh
mysql --default-character-set=utf8mb4 --database=ingot_iam < databases/iam/migrations/023_tenant_members_departments_menus.sql
```

完成后重新 bootstrap 或登录加载菜单。新建库使用最新 006 或完整初始化文件，已有库不能用完整初始化文件代替本补齐脚本。回退时按菜单 ID 恢复原名称、父级、排序与 `iam_menu_action`，并删除部门页与「成员与部门」目录。

## 025 组织成员选择器姓名搜索能力

仅将 `iam-tenant/member` 目录中 `displayName.filterable` 补为 true，并在实际变化时递增资源版本；按字段 key 定位，不依赖 JSON 数组顺序，可重复执行。不改通讯录、平台资源、角色/分配、历史默认策略版本或租户字段规则。该值是已接入能力上限，普通用户仍需字段策略允许筛选且整个查询范围内姓名完整可见。真实拥有者也受平台显式操作上限限制。

已有库执行 `025_member_picker_name_filter.sql`，与含 `OrganizationMemberNameFilter` 执行清单的后端同批部署；广播既有配置失效或等待最长元数据缓存窗口后验证。未补齐目录或清单时继续拒绝姓名筛选。新库使用最新006/bundle。回退应用时可保留目录能力，由旧清单继续收紧；若回退目录，将该字段 filterable 恢复备份值并递增资源版本。不要重建现有库，也不要通过删除字段筛选门禁解决403。

## 026 字段说明升级

`026_schema_comments.sql`只补齐或完善63张表的中文COMMENT。脚本及`026_schema_comments_baseline.json`固定在027/028之前的版本，生成入口只校验其摘要，不按最新DDL重写。完整保留该版本的类型、可空性、默认值、生成表达式、自增属性及约束。框架共库表仍归各框架模块所有。

先完成目标库到026基线的既有升级（尚未执行027/028），备份后选择维护窗口、停止写入并禁止并行DDL。使用mysql客户端可读取的连接配置文件；不要把口令放入命令行或报告。下面的数据库名只是明确选库的示例：

```sh
# 全部表只读结构核对，不创建任何数据库、不执行ALTER
python3 tools/iam/schema_comments.py --database ingot_iam --defaults-file /absolute/path/mysql-client.cnf
# 同样先完成全量核对，再执行注释ALTER
python3 tools/iam/schema_comments.py --database ingot_iam --defaults-file /absolute/path/mysql-client.cnf --apply
```

026预检读取SHOW CREATE，对比固定历史基线；当前最新结构另保存在schema_structure.json，不参与026匹配。历史脚本摘要不匹配时拒绝运行。预检仅忽略COMMENT、自增高水位、无语义的约束排序，以及MySQL MODIFY后ASCII检查常量的等价字符集引导符（_utf8mb4/_ascii）；不忽略列字符集、COLLATE、非ASCII文字或表达式变化。缺表、缺列、类型/默认/生成列/约束/索引漂移或历史脚本摘要不匹配时，在任何ALTER之前退出。不会自动补结构，不操作清单外表，不依赖默认数据库，不关闭外键。已在隔离MySQL 8.0.44和8.4验证。其他MySQL版本产生的未识别结构差异也保守拒绝，需先在对应隔离版本核对，不能跳过预检。

MySQL DDL不具备整批事务回滚，ALTER可能等待元数据锁；失败后按错误处理再重跑，不使用`--force`。已完成的注释不影响应用回退；如需恢复旧注释，应从先前版本权威源在隔离库生成并验证专用脚本，禁止重建目标表。

开发期刷新结构快照使用`python3 tools/iam/refresh_schema_snapshot.py`，只启动本地已有`mysql:8.4`镜像的无网络、无端口、无卷临时容器，结束清理；它不接受实际数据库连接。正常生成和`--check`不启动MySQL。历史迁移脚本原样保留，新库依然只按manifest构建。


## 027/028 接收成员主键与密码历史索引

027为`iam_delegation_recipient_member`新增非自增id，应用统一用IAM Leaf/IdType.INPUT；保留原关系唯一键、生成列、外键及身份检查。028删除`password_history.idx_user_id`，保留原自增主键、`uk_user_sequence(user_id,sequence_number)`及`idx_created_at`。删除已通过隔离MySQL 8.0.44/8.4三态对照；合成负载及执行计划见[验证记录](../../../specs/changes/active/20261020-iam-recipient-primary-key-password-index/VALIDATION.md)，不代表生产容量测量。

新库使用最新manifest/bundle，跳过026/027/028。已有库尚未补注释时先按上一节在026基线执行026；027之后不要重放026。不要直接导入027/028或回退SQL：统一工具先核对两表完整定义及Leaf表定义，缺表、约束/类型/索引漂移、缺少iam发号标签、ID异常或号段溢出时，在任何DDL前退出。

先备份目标库，停止全部IAM节点及其他目标库写入，禁止并发DDL。连接文件须为mysql客户端可读配置，显式选库，不把口令写在命令行：

```sh
# 只读预检，不预留ID，不执行DDL
python3 tools/iam/schema_key_upgrade.py --database ingot_iam --defaults-file /absolute/path/mysql-client.cnf
# 备份并停止全部写入后执行；标志表示操作者已完成停写检查
python3 tools/iam/schema_key_upgrade.py --database ingot_iam --defaults-file /absolute/path/mysql-client.cnf --apply --writers-stopped
```

工具需目标库SHOW/SELECT、ALTER、CREATE ROUTINE/ALTER ROUTINE/EXECUTE、CREATE TEMPORARY TABLES及UPDATE权限。027先添加可空id，再锁定`biz_leaf_alloc.biz_tag='iam'`，按待填行数N原子预留`[原max_id, 原max_id+N)`，高水位与ID填充同事务提交，最后收紧非空并建立主键。仅增加iam发号高水位，不修改其他标签；空表不消费号段。新ID不是时间或业务顺序。两表业务字段及数据量不变。

MySQL DDL会自动提交，不能整体ROLLBACK；新增主键涉及重建，需按实际表量预留维护窗口。中断后保持停写，修正错误再完整重跑：工具识别原结构、可空id中间态、已完成结构及索引已删除状态；已填ID不重新分配，号段不重复消费。工具显式禁止force及自动重连。完成后重启全部新后端（丢弃旧进程发号缓存），验证委派创建/更新、跨租户拒绝、密码历史读取/覆盖/删除。无前端/API升级，不运行006重写业务授权。

**回退必须先停止全部新写入，不能只回退应用。** 旧代码不填写非自增id。完整备份后使用同一工具恢复索引、删除新增主键及id，再启动旧后端：

```sh
python3 tools/iam/schema_key_upgrade.py --database ingot_iam --defaults-file /absolute/path/mysql-client.cnf --rollback --writers-stopped
```

回退预检拒绝新增id外键引用或其他未知结构；保留原唯一键、外键、CHECK及全部关系/密码历史，**永不降低Leaf高水位**。回退可重跑，重新升级从新的高水位预留。若维护期间已有不在本方案内的DDL，先核对并制定专项处理，不能绕过预检。026注释无需随结构回退撤销。

隔离演练：`python3 tools/iam/test_schema_key_upgrade_mysql.py`；通过`IAM_SCHEMA_MYSQL_IMAGE`选择本地已有8.0.44镜像。索引验证入口`python3 tools/iam/verify_password_history_index.py --output /absolute/path/evidence.json`只启动无网络临时容器，不接受业务库连接。
