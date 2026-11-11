# IAM 数据结构与关系持久化

## 权威定义与说明

`databases/iam/manifest.json` 管理 59 张运行表和 501 个字段。IAM 自有 53 张表采用 `iam_` 前缀，包含 `iam_dictionary`、`iam_social_config` 和 `iam_account_social_binding`。字典覆盖平台、租户和应用三种作用域。社交绑定关联全局 IAM 账号。下载任务表 `iam_download_task` 由 IAM 实现下载框架的存储接口。

| 框架模块 | 共享表 |
|---|---|
| `ingot-security-account-adapter` | `fw_security_account_lock_state` |
| `ingot-security-credential-data` | `fw_security_password_history`、`fw_security_password_expiration` |
| `ingot-security-event-store-mysql` | `fw_security_event` |
| `ingot-id` | `fw_id_leaf_alloc` |
| `ingot-oss-common` | `fw_oss_asset_upload` |

框架模块通过 `sql/<module>/schema.sql` 发布权威建表定义。独立接入时在消费服务的数据源对应库执行，使用 `CREATE TABLE IF NOT EXISTS`。IAM、Member 和 Security 的完整初始化按各自依赖收录共享表，具体对应关系见[数据库脚本索引](../../../../databases/README.md)。

首次部署 IAM 只执行 [ingot_iam.sql](../../../../databases/ingot_iam.sql)。它固定删除并重建 `ingot_iam`，创建运行表，写入默认应用、资源、权限、角色、菜单、关联关系及平台管理员。初始账号为 `platform`，初始密码为 `11111111`，`must_change_password=TRUE`。历史迁移表、租户测试数据和运行记录不进入初始化。已有业务数据的保留需要对应版本的迁移方案。

所有表和字段的中文说明在权威 DDL 维护。通过 `tools/iam/generate_database.py` 生成完整初始化和数据字典，通过 `generate_database_review.py` 生成主键与索引评估。

## Provider 职责边界

`ingot-iam-api` 提供 Feign RPC、传输模型及消费端装配。`ingot-commons` 提供跨模块复用的 IAM 契约和枚举。IAM 专用业务模型、持久化实体和映射位于 `ingot-iam-provider`。

Provider 由 `com.ingot.cloud.iam.InIamApplication` 启动并扫描模块根包。外部 HTTP 和内部 RPC 在 `interfaces`，用例编排在 `application`，领域规则在 `domain`，数据库、缓存及外部集成适配在 `infrastructure`。固定角色版本的批量应用由 `application.roleapplication` 编排。

`MemberFacade` 聚合成员用例，读取和投影由 `MemberQueryService` 处理，成员资料、创建和角色变更由 `MemberProfileCommandService` 处理。普通持久化读取进入 `MemberQueryRepository`，写入、行锁和关系变更进入 `MemberWriteRepository`。目录仓储按应用、资源、操作、菜单、套餐和跨表清理分工。字段策略持久化由 `FieldPolicyRepository` 承担，`FieldPolicySnapshotLoader` 加载快照，`FieldAccessEvaluator` 负责授权求值。

成员主部门辅助列`primary_member_id`仅在主标记成立时取成员ID，否则为NULL；部门主负责人辅助列`primary_department_id`同理取部门ID。配套租户唯一索引保证至多一个主关系；非空集合需要恰好一个主关系等完整规则由事务服务保证，辅助列不接受应用写入。

## 关系差量保存

| 关系 | 当前写入规则 |
|---|---|
| 成员部门 | 两处保存入口共用MemberDepartmentWriteRepository，仅删除移除项、插入新增项、更新变化的主标记 |
| 平台和租户用户组成员 | 按集合差异增删，保留未变行 |
| 租户用户组部门来源 | 保留未变关系，仅原地更新变化的include_descendants |
| 菜单操作、套餐应用 | 仅增删变化的绑定；套餐变更不回写已有租户开通 |
| 部门负责人 | 按增删差量维护；主负责人未变时不清空重设主标记 |

在原事务和父对象锁内读取完整旧关系，写操作限定租户及完整关系归属。切换主关系先解除旧标记，再设置新标记；新增主部门关系可以一次INSERT携带目标主标记。不变关系不执行关系DML，保留行不删除重插。

各入口保留自己的空值、清空、重复项校验、权限与对象范围、版本冲突、审计、授权失效及回滚契约。父对象版本和审计仍按接口原规则执行，不能因关系未变而统一跳过。业务删除需要的关联清理保留。

应用人群保存、整体开通重置、委派子关系及角色/策略快照沿自身并发和生命周期规则维护，不套用上述集合差量逻辑。

## 委派接收成员主键

`iam_delegation_recipient_member.id`为非自增BIGINT UNSIGNED显式主键，Java使用BigInteger及`IdType.INPUT`。平台实体写入与租户手写SQL均通过现有IAM发号入口取得Leaf标识，并在插入前显式填写。该标识不进入公共API，不表示业务顺序或事务提交时间。

`UNIQUE(delegation_id,member_id)`继续保证接收者不重复。`member_id`、`tenant_key`生成列、平台/租户外键及身份CHECK保留；新增行ID不替代管理域和租户归属条件，不改变委派接收资格和派生授权重验。

其他关系表保留现有联合主键和反向索引。复合归属索引承担租户/应用外键校验时，即使单列ID全局唯一，也不能直接移除。键值非递增不等于已发生性能瓶颈，结构评估与实际负载测量分开记录。

## 租户分配列表覆盖索引

`iam_role_assignment.idx_iam_assignment_tenant_list` 按 `tenant_id, domain, id, role_id, subject_type, delegation_grant_id, tenant_member_id, tenant_group_id` 排列。新库由初始化脚本创建，已有库用 [037 在线迁移](../../../../databases/iam/migrations/037_tenant_assignment_list_index.sql) 补齐。列表按可见范围保留精确计数及页码跳转。深页先取本页分配 ID，再读取本页详情。

## 动态范围与 SQL 防火墙

成员所属组的计数和分页使用同一套直接绑定、部门展开与去重逻辑。全范围计数省略可选的组条件，受限范围用绑定参数生成组 ID 的 `IN` 条件。组范围为空时仓库直接返回空页。租户与成员约束在来源查询中生效。

授权候选、角色、分配、委派及负责人等查询中需要在 SQL 内表达的空对象范围，使用目标列的 `IN (NULL)`，使该范围匹配不到任何行。该谓词保持所在分支的空范围语义，其他合法 `OR` 分支仍按自身条件求值。负责人查询按实际范围组合条件。查询兼容现行 Druid MySQL Wall 规则，沿用现有防护配置和数据库结构。

## 密码历史索引

`fw_security_password_history`保留原自增id主键、`UNIQUE(user_id,sequence_number)`及`idx_created_at`；不再维护与联合唯一键左前缀重叠的`idx_user_id`。按用户读取、槽位覆盖、历史检查和删除的服务行为不变，最近历史仍按created_at倒序及limit查询。

框架权威DDL、IAM共库初始化和数据字典保持一致。密码过期表及其他框架表结构不随该索引精简改变。

## 审计游标索引与上传暂存

`iam_authorization_audit` 按租户、发生时间和 ID 排序。平台域使用 `tenant_id IS NULL`。操作者和具体对象查询分别使用 `(tenant_id, actor_member_id, occurred_at, id)`、`(tenant_id, target_type, target_id, occurred_at, id)` 索引。已有库通过 032 增量安装两条索引，支持重复执行及回滚。

IAM 与 Member 在业务所属库的 `fw_oss_asset_upload` 记录短时图片凭证，业务绑定与凭证消费同事务提交。清理按到期索引定位超时记录，并在锁定后复核状态。

## 维护与兼容

已有库升级必须显式选库并先核对结构；未知类型、默认值、生成列、索引或约束漂移应停止，不兼做自动结构修复。历史注释迁移固定其脚本和结构基线，不能随当前DDL增加字段或修改索引。详细执行与回退依据见[迁移说明](../../../../databases/iam/migrations/README.md)。

Leaf高水位只向前推进，历史数据回填和结构回退均不得降低。非自增接收成员表要求所有写入端填写ID，不能直接启动仍省略该字段的旧写入端；应用回退与表结构兼容性必须一并核对。

## 平台全局社会化配置与精确状态

社会化配置 tenant_id 允许 NULL，表示平台全局记录；既有记录保留租户值。正式初始化直接提供此结构，038 仅供适用已有库补齐并支持重跑，不引入历史数据迁移门禁。

授权和分配状态判断采用 UTC 及 CURRENT_TIMESTAMP(6)，有效起点包含、截止排除，列表筛选/count/详情一致。任务到期清理按候选主键定位并复核，不在登记事务中以无界范围更新造成跨节点死锁；对象删除在事务提交后执行。
