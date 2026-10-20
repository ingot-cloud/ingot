# Validation

开发及验证均未连接实际业务数据库。数据库测试使用无网络、无端口、无挂载的临时MySQL容器；正式库升级需操作者选库、备份、停写并执行预检。

## 实现范围

- 最新结构与026前基线对比：仅`iam_delegation_recipient_member`和`password_history`发生变化；另外61张表不变，manifest为63表/495字段。
- 接收成员id为非自增BIGINT UNSIGNED，实体BigInteger/IdType.INPUT；平台及租户公共写入口均调用IAM现有Leaf发号。
- 保留关系唯一键、生成列、全部外键/CHECK及公共API。password_history只删除idx_user_id。
- 026脚本SHA-256保持`0df43bfebdbccde24c1a55e05bbbfe0f4580db539a379b002bca4247698df989`；历史基线独立于最新schema_structure.json。

## 密码历史索引对照

原始证据：[MySQL 8.4](password-index-mysql84.json)、[MySQL 8.0.44](password-index-mysql80.json)。复现命令：

```sh
python3 tools/iam/verify_password_history_index.py --output /absolute/path/index84.json
IAM_SCHEMA_MYSQL_IMAGE=docker-registry.ingotcloud.top/mysql:8.0.44 python3 tools/iam/verify_password_history_index.py --output /absolute/path/index80.json
```

两个版本分别覆盖：500用户×5历史（2500行）、10000用户×10历史（100000行）、1000用户×5历史且单个热点用户1000条（5995行）。每组均执行原索引可见/不可见/实际删除三态，共18组配置。查询包含全部历史、最近5条、普通用户及不存在用户；每项预热后执行5批，每批5次；同批验证插入、覆盖更新及按用户删除后ROLLBACK。

- 所有状态查询结果摘要一致，没有新增全表扫描。按用户读全集使用uk_user_sequence；普通历史扫描5或10行、热点全集1000行、缺失用户0行。
- 热点最近历史原本可以使用idx_created_at倒序及用户过滤，三态均保留该路径，样本实际扫描5行；不声称最近历史全部通过user_id范围访问，也不顺带增加排序索引。
- 100000行样本中，8.4的idx_user_id统计为609页，8.0.44为481页，删除后该索引消失，其他索引保留。不可见状态仍维护该索引。
- EXPLAIN ANALYZE、查询计划、索引页数及多轮耗时保存在原始JSON。墙钟耗时包含本地docker/mysql客户端开销，不能用于声称生产TPS提升或容量上限；未观察到本样本下删除导致的稳定可复现退化。

## Java验证

正常工作区（没有临时源覆盖）命令：

```sh
./gradlew :ingot-service:ingot-iam:ingot-iam-provider:test --offline --console=plain -I tools/iam/tenant_live/verification.gradle --tests '*DelegationRecipientIdTest' --tests '*PlatformDelegationConflictTest' --tests '*DelegationDurationAdmissionTest' --tests '*DelegationRepositoryTest' --tests '*PlatformDelegationRoleCandidateTest' --tests '*TenantDelegationRecipientIdTest' --tests '*PasswordHistoryIndexCompatibilityTest'
```

**18项通过，0失败/0错误**：DelegationRecipientIdTest 3、PlatformDelegationConflictTest 8、DelegationDurationAdmissionTest 1、DelegationRepositoryTest 2、PlatformDelegationRoleCandidateTest 2、TenantDelegationRecipientIdTest 1、PasswordHistoryIndexCompatibilityTest 1。

覆盖显式ID不被替换、未填ID拒绝、两域Mapper真实持久化、业务重复拒绝、事务回滚、两域公共服务独立领取关系ID、原委派冲突/撤销/权限边界；框架密码历史真实服务覆盖环形槽位原行更新、版本、历史检查及按用户删除隔离。

开始时工作区已有IamMemberDepartmentEntity未完成注解导致编译失败；用户随后自行撤销，最终18项在正常工作区通过。先前临时源验证不替代最终门禁。新密码服务测试首次缺少测试夹具Mapper注册，补齐后通过；生产密码服务未修改。

## 数据库与工具门禁

**全部通过：54项隔离MySQL、11项离线Python工具检查；生成漂移及git diff --check通过。**

| 检查 | 通过项数 |
|---|---:|
| 027/028迁移与回退，8.4 / 8.0.44 | 9 + 9 |
| 026历史注释升级，8.4 / 8.0.44 | 3 + 3 |
| IAM身份与授权结构约束 | 28 |
| 新库bundle/分片63表及种子一致、初始化重建外键 | 2 |
| 离线说明/结构工具 | 7 + 4 |

所有受资源不足影响的检查已串行重跑通过。执行入口：

```sh
python3 tools/iam/test_schema_key_upgrade_mysql.py
IAM_SCHEMA_MYSQL_IMAGE=docker-registry.ingotcloud.top/mysql:8.0.44 python3 tools/iam/test_schema_key_upgrade_mysql.py
python3 tools/iam/test_schema_comments_mysql.py
IAM_SCHEMA_MYSQL_IMAGE=docker-registry.ingotcloud.top/mysql:8.0.44 python3 tools/iam/test_schema_comments_mysql.py
python3 databases/iam/test_identity_schema.py
python3 databases/iam/test_bootstrap_seed.py BootstrapSeedTest.test_generated_bundle_matches_component_schema_and_initial_data BootstrapSeedTest.test_full_initialization_resets_linked_data_and_restores_session_foreign_keys
python3 tools/iam/test_schema_comments.py
python3 tools/iam/test_database_sources.py
python3 tools/iam/generate_database.py --check
python3 tools/iam/generate_database_review.py --check
```

迁移覆盖空表/有数据、预留区间、正向重跑、可空列/已提交填充中间态、事务失败不消费号段、数据投影不变、与新库63表定义一致、未知结构/缺标签/溢出/异常ID拒绝、入向ID外键时零DDL拒绝回退、结构回退不降低高水位、号段耗尽仍能撤销中间结构。026固定原结构后继续支持重复补注释，027后拒绝旧026。

有并行测试批次触发本机OSError 12/35资源不足；失败批次不计通过，清理本轮残留容器后改为串行复跑。用户本次未授权实际库执行，未执行部署。

## 验收状态

保持validating。部署/产品验收尚未完成，V2未勾选，不更新current、不归档。应用回退必须停写并先执行结构回退，不能在非自增新表结构上直接启动旧代码。
