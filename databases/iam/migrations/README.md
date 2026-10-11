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
