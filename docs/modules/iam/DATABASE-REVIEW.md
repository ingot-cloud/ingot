# IAM 主键、索引与关系写入评估

<!-- 生成：python3 tools/iam/generate_database_review.py；审查结论在生成源维护。 -->

覆盖 manifest 中全部63张表。结构来自权威DDL及隔离MySQL 8.4的有效索引快照；没有连接实际业务数据库，没有测量业务容量、页分裂率、缓存命中率或延迟。

## 评估口径

- 结论分为保留、候选优化、需要运行数据验证。P1为模型明确性候选，P2为成本验证候选，P3为保留现状。
- 联合主键按最左列依次排序；为已有成员/组添加关系可能写入旧页。局部性收益与页分裂成本应一并比较，不把非递增视为错误。
- 加递增ID仍须保留业务唯一索引，原关系键仍可能中间插入，并增加一棵索引；反向索引已经包含主键列时不得重复计算全部主键附加成本。
- BIGINT键值按8字节估算；字符串按定义字符集最大字节估算，不是磁盘测量。业务非自增ID由服务端发号或目录预留，不承诺严格提交时序。
- 有效索引包括MySQL为外键隐式补建的索引；复合唯一键常用于验证所属租户/应用，不能仅因id全局唯一就删除。
- 纯键投影可能覆盖查询；读取JSON、状态等其他属性通常仍需聚簇行。是否发生回表以具体SQL和EXPLAIN为准。

依据：[InnoDB聚簇与二级索引](https://dev.mysql.com/doc/refman/8.0/en/innodb-index-types.html)、[索引页结构](https://dev.mysql.com/doc/refman/8.0/en/innodb-physical-structure.html)。

## 本轮处理与后续候选

成员部门、两域用户组关系、菜单操作、套餐应用已实施差量；负责人未变主标记不再清空重设。公共API、版本、审计及授权失效规则保留；后续独立结构变更见下文。

接收成员表已新增Leaf显式主键；密码历史经隔离对照删除重叠索引，原主键保留。后续候选仍为宽字符串主键iam_migration_mapping，尚未调整；详见[验证记录](../../../specs/changes/archive/2026/20261020-iam-recipient-primary-key-password-index/VALIDATION.md)。

人群独立保存仍全量重写，整体开通替换会重建人群；两者需要统一并发和重置语义后另行设计。委派重建涉及派生授权重验，角色/策略新快照是首次写入，业务删除需要清关联；这些路径保留。

## 逐表结论

### iam_account — 保留（P3）

全局登录账号

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[AccountCredentialRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/identity/AccountCredentialRepository.java), [AccountQueryRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/AccountQueryRepository.java), [AccountWriteRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/AccountWriteRepository.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_account_email` (`email`)``；``KEY `idx_iam_account_phone` (`phone`)``；``UNIQUE KEY `uk_iam_account_username` (`username`)``。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：``iam_platform_member(account_id) → (id)``；``iam_tenant_member(account_id) → (id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：全局账号按ID及登录名定位；手机号/邮箱索引服务匹配检索。账号、成员和外部用户标识不可混用。
- DDL证据：[001_identity.sql](../../../databases/iam/001_identity.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_account)。

### iam_platform_member — 保留（P3）

平台成员资格与资料

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[AssignmentRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/AssignmentRepository.java), [DelegationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/DelegationRepository.java), [GroupRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/GroupRepository.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_platform_member_status` (`status`,`id`)``；``UNIQUE KEY `uk_iam_platform_member_account` (`account_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_platform_member_account` FOREIGN KEY (`account_id`) REFERENCES `iam_account` (`id`)``。
- 被其他表引用：``iam_platform_group_member(member_id) → (id)``；``iam_delegation_grant(platform_administrator_id) → (id)``；``iam_delegation_recipient_member(platform_member_id) → (id)``；``iam_role_assignment(platform_member_id) → (id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按账号唯一定位平台资格；状态列表使用status/id。account_id唯一索引同时防止重复资格。
- DDL证据：[001_identity.sql](../../../databases/iam/001_identity.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_platform_member)。

### iam_tenant — 保留（P3）

租户组织

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[AccountCredentialRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/identity/AccountCredentialRepository.java), [EntitlementRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/EntitlementRepository.java), [MemberLifecycleRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/MemberLifecycleRepository.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_tenant_owner` (`id`,`owner_member_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_tenant_owner` FOREIGN KEY (`id`, `owner_member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`)``。
- 被其他表引用：``iam_tenant_member(tenant_id) → (id)``；``iam_department(tenant_id) → (id)``；``iam_tenant_group(tenant_id) → (id)``；``iam_tenant_app_entitlement(tenant_id) → (id)``；``iam_role_definition(tenant_id) → (id)``；``iam_policy_selector(tenant_id) → (id)``；``iam_directory_policy(tenant_id) → (id)``；``iam_field_policy(tenant_id) → (id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：组织ID是组织事务的锁定对象；(id,owner_member_id)支持所属组织拥有者复合引用，不因id已有主键就删除。
- DDL证据：[001_identity.sql](../../../databases/iam/001_identity.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_tenant)。

### iam_tenant_member — 保留（P3）

租户成员资格与通讯录资料

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[AccountCredentialRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/identity/AccountCredentialRepository.java), [AssignmentRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/AssignmentRepository.java), [DelegationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/DelegationRepository.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_tenant_member_account` (`account_id`,`status`)``；``KEY `idx_iam_tenant_member_status` (`tenant_id`,`status`,`id`)``；``UNIQUE KEY `uk_iam_tenant_member_account` (`tenant_id`,`account_id`)``；``UNIQUE KEY `uk_iam_tenant_member_domain` (`tenant_id`,`id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_tenant_member_account` FOREIGN KEY (`account_id`) REFERENCES `iam_account` (`id`)``；``CONSTRAINT `fk_iam_tenant_member_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`)``。
- 被其他表引用：``iam_tenant(id, owner_member_id) → (tenant_id, id)``；``iam_member_department(tenant_id, member_id) → (tenant_id, id)``；``iam_department_leader(tenant_id, member_id) → (tenant_id, id)``；``iam_tenant_group_member(tenant_id, member_id) → (tenant_id, id)``；``iam_audience_member(tenant_id, member_id) → (tenant_id, id)``；``iam_delegation_grant(tenant_id, tenant_administrator_id) → (tenant_id, id)``；``iam_delegation_recipient_member(tenant_id, tenant_member_id) → (tenant_id, id)``；``iam_role_assignment(tenant_id, tenant_member_id) → (tenant_id, id)``；``iam_policy_selector_member(tenant_id, member_id) → (tenant_id, id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按组织/账号唯一定位资格，状态列表及跨域账号反查有独立索引；(tenant_id,id)用于同租户复合外键。
- DDL证据：[001_identity.sql](../../../databases/iam/001_identity.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_tenant_member)。

### iam_department — 保留（P3）

租户部门树

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[DepartmentQueryRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/DepartmentQueryRepository.java), [EntitlementRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/EntitlementRepository.java), [GroupRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/GroupRepository.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_department_parent` (`tenant_id`,`parent_id`,`id`)``；``UNIQUE KEY `uk_iam_department_domain` (`tenant_id`,`id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_department_parent` FOREIGN KEY (`tenant_id`, `parent_id`) REFERENCES `iam_department` (`tenant_id`, `id`)``；``CONSTRAINT `fk_iam_department_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`)``。
- 被其他表引用：``iam_department(tenant_id, parent_id) → (tenant_id, id)``；``iam_member_department(tenant_id, department_id) → (tenant_id, id)``；``iam_department_leader(tenant_id, department_id) → (tenant_id, id)``；``iam_tenant_group_department(tenant_id, department_id) → (tenant_id, id)``；``iam_audience_department(tenant_id, department_id) → (tenant_id, id)``；``iam_delegation_recipient_department(tenant_id, department_id) → (tenant_id, id)``；``iam_policy_selector_department(tenant_id, department_id) → (tenant_id, id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按ID定位，部门树按tenant_id/parent_id访问；(tenant_id,id)承担租户归属约束，属于有意的复合引用键。
- DDL证据：[001_identity.sql](../../../databases/iam/001_identity.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_department)。

### iam_member_department — 保留（P3）

成员任职部门关系

- 主键：`tenant_id / member_id / department_id`；宽度：24 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[AccountCredentialRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/identity/AccountCredentialRepository.java), [DepartmentQueryRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/DepartmentQueryRepository.java), [GroupRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/GroupRepository.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_department_members` (`tenant_id`,`department_id`,`member_id`)``；``UNIQUE KEY `uk_iam_member_primary_department` (`tenant_id`,`primary_member_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_member_department_department` FOREIGN KEY (`tenant_id`, `department_id`) REFERENCES `iam_department` (`tenant_id`, `id`)``；``CONSTRAINT `fk_iam_member_department_member` FOREIGN KEY (`tenant_id`, `member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：已改为共享差量写入。
- 判断：按tenant_id/member_id聚集，反向tenant_id/department_id/member_id支持部门成员；主部门辅助列唯一约束独立保留。本轮两入口共用差量写入。
- DDL证据：[001_identity.sql](../../../databases/iam/001_identity.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_member_department)。

### iam_department_leader — 保留（P3）

部门负责人关系

- 主键：`tenant_id / department_id / member_id`；宽度：24 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[DepartmentLeaderSql.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/DepartmentLeaderSql.java), [IamDepartmentLeaderMapper.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/mapper/IamDepartmentLeaderMapper.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_member_led_departments` (`tenant_id`,`member_id`,`department_id`)``；``UNIQUE KEY `uk_iam_department_primary_leader` (`tenant_id`,`primary_department_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_department_leader_department` FOREIGN KEY (`tenant_id`, `department_id`) REFERENCES `iam_department` (`tenant_id`, `id`)``；``CONSTRAINT `fk_iam_department_leader_member` FOREIGN KEY (`tenant_id`, `member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：已为差量；本轮跳过不变主标记。
- 判断：按tenant_id/department_id聚集，成员反查有反向索引；负责人关系与任职关系独立。本轮保留未变主标记，增删仍按差量。
- DDL证据：[001_identity.sql](../../../databases/iam/001_identity.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_department_leader)。

### iam_platform_group — 保留（P3）

平台静态用户组

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[AssignmentRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/AssignmentRepository.java), [GroupRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/GroupRepository.java), [IamPlatformGroupEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamPlatformGroupEntity.java)。
- 有效二级索引/业务唯一键：无。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：``iam_platform_group_member(group_id) → (id)``；``iam_role_assignment(platform_group_id) → (id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：用户组主行按ID加锁及检查版本，成员关系独立维护；删除时清子关系是必要操作。
- DDL证据：[001_identity.sql](../../../databases/iam/001_identity.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_platform_group)。

### iam_platform_group_member — 保留（P3）

平台用户组显式成员

- 主键：`group_id / member_id`；宽度：16 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[AssignmentRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/AssignmentRepository.java), [GroupRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/GroupRepository.java), [IamMembershipSql.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/IamMembershipSql.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_platform_member_groups` (`member_id`,`group_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_platform_group_entry_group` FOREIGN KEY (`group_id`) REFERENCES `iam_platform_group` (`id`)``；``CONSTRAINT `fk_iam_platform_group_entry_member` FOREIGN KEY (`member_id`) REFERENCES `iam_platform_member` (`id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：已改为差量写入。
- 判断：group_id/member_id自然唯一，支持整组读取；member_id/group_id支持成员反查。本轮保存按集合差量，保留原行。
- DDL证据：[001_identity.sql](../../../databases/iam/001_identity.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_platform_group_member)。

### iam_tenant_group — 保留（P3）

租户静态用户组

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[AssignmentRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/AssignmentRepository.java), [EntitlementRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/EntitlementRepository.java), [GroupRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/GroupRepository.java)。
- 有效二级索引/业务唯一键：``UNIQUE KEY `uk_iam_tenant_group_domain` (`tenant_id`,`id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_tenant_group_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`)``。
- 被其他表引用：``iam_tenant_group_member(tenant_id, group_id) → (tenant_id, id)``；``iam_tenant_group_department(tenant_id, group_id) → (tenant_id, id)``；``iam_audience_group(tenant_id, group_id) → (tenant_id, id)``；``iam_role_assignment(tenant_id, tenant_group_id) → (tenant_id, id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：用户组主行按ID锁定；(tenant_id,id)支持关系同租户约束，不改为只校验group_id。
- DDL证据：[001_identity.sql](../../../databases/iam/001_identity.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_tenant_group)。

### iam_tenant_group_member — 保留（P3）

租户用户组显式成员

- 主键：`tenant_id / group_id / member_id`；宽度：24 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[GroupRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/GroupRepository.java), [IamMembershipSql.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/IamMembershipSql.java), [OrganizationMemberRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/OrganizationMemberRepository.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_tenant_member_groups` (`tenant_id`,`member_id`,`group_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_tenant_group_entry_group` FOREIGN KEY (`tenant_id`, `group_id`) REFERENCES `iam_tenant_group` (`tenant_id`, `id`)``；``CONSTRAINT `fk_iam_tenant_group_entry_member` FOREIGN KEY (`tenant_id`, `member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：已改为差量写入。
- 判断：tenant_id/group_id/member_id支持读取整组，反向成员索引支撑授权组求值；本轮差量增删。
- DDL证据：[001_identity.sql](../../../databases/iam/001_identity.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_tenant_group_member)。

### iam_tenant_group_department — 保留（P3）

租户用户组部门来源

- 主键：`tenant_id / group_id / department_id`；宽度：24 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[GroupRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/GroupRepository.java), [IamMembershipSql.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/IamMembershipSql.java), [TenantLiveOptionSql.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/TenantLiveOptionSql.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_tenant_department_groups` (`tenant_id`,`department_id`,`group_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_tenant_group_dept_department` FOREIGN KEY (`tenant_id`, `department_id`) REFERENCES `iam_department` (`tenant_id`, `id`)``；``CONSTRAINT `fk_iam_tenant_group_dept_group` FOREIGN KEY (`tenant_id`, `group_id`) REFERENCES `iam_tenant_group` (`tenant_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：已改为差量写入及属性UPDATE。
- 判断：按组读取部门来源，反向部门索引支撑展开；include_descendants是关系属性，本轮仅属性变化时UPDATE。
- DDL证据：[001_identity.sql](../../../databases/iam/001_identity.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_tenant_group_department)。

### iam_application — 保留（P3）

应用目录

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[CatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/CatalogRepository.java), [InitializationCatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/InitializationCatalogRepository.java), [SessionRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/SessionRepository.java)。
- 有效二级索引/业务唯一键：``UNIQUE KEY `uk_iam_application_code` (`code`)``。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：``iam_resource(application_id) → (id)``；``iam_menu(application_id) → (id)``；``iam_tenant_app_entitlement(application_id) → (id)``；``iam_plan_application(application_id) → (id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：应用按ID或全局code查找，治理目录含预留ID；应用域及baseline有业务约束，ID顺序不等于创建时间。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_application)。

### iam_resource — 保留（P3）

应用资源及范围字段能力

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[CatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/CatalogRepository.java), [TenantLiveOptionSql.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/TenantLiveOptionSql.java), [IamResourceEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamResourceEntity.java)。
- 有效二级索引/业务唯一键：``UNIQUE KEY `uk_iam_resource_application` (`application_id`,`id`)``；``UNIQUE KEY `uk_iam_resource_code` (`application_id`,`code`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_resource_application` FOREIGN KEY (`application_id`) REFERENCES `iam_application` (`id`)``。
- 被其他表引用：``iam_action(application_id, resource_id) → (application_id, id)``；``iam_role_parameter(resource_id) → (id)``；``iam_delegation_resource_ceiling(resource_id) → (id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按应用/code查询资源；(application_id,id)用于同应用操作外键，不能按逻辑冗余直接删除。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_resource)。

### iam_action — 保留（P3）

资源精确操作目录

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[CatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/CatalogRepository.java), [SessionRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/SessionRepository.java), [IamActionEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamActionEntity.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_action_resource` (`application_id`,`resource_id`,`id`)``；``UNIQUE KEY `uk_iam_action_application` (`application_id`,`id`)``；``UNIQUE KEY `uk_iam_action_code` (`code`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_action_resource` FOREIGN KEY (`application_id`, `resource_id`) REFERENCES `iam_resource` (`application_id`, `id`)``。
- 被其他表引用：``iam_menu_action(application_id, action_id) → (application_id, id)``；``iam_role_grant(action_id) → (id)``；``iam_role_delta(action_id) → (id)``；``iam_delegation_action_ceiling(action_id) → (id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：操作码全局唯一且精确；按应用/资源列举操作，复合应用引用键保护菜单与操作归属。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_action)。

### iam_menu — 保留（P3）

应用菜单与前端路由

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[CatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/CatalogRepository.java), [SessionRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/SessionRepository.java), [IamMenuEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamMenuEntity.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_menu_parent` (`application_id`,`parent_id`,`sort_order`,`id`)``；``UNIQUE KEY `uk_iam_menu_application` (`application_id`,`id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_menu_application` FOREIGN KEY (`application_id`) REFERENCES `iam_application` (`id`)``；``CONSTRAINT `fk_iam_menu_parent` FOREIGN KEY (`application_id`, `parent_id`) REFERENCES `iam_menu` (`application_id`, `id`)``。
- 被其他表引用：``iam_menu(application_id, parent_id) → (application_id, id)``；``iam_menu_action(application_id, menu_id) → (application_id, id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按应用/父节点/排序构建菜单树；菜单操作绑定本轮按差量维护；路由键不承担数据库行标识。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_menu)。

### iam_menu_action — 保留（P3）

菜单操作绑定

- 主键：`application_id / menu_id / action_id`；宽度：24 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[CatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/CatalogRepository.java), [SessionRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/SessionRepository.java), [IamMenuActionEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamMenuActionEntity.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_action_menus` (`application_id`,`action_id`,`menu_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_menu_action_action` FOREIGN KEY (`application_id`, `action_id`) REFERENCES `iam_action` (`application_id`, `id`)``；``CONSTRAINT `fk_iam_menu_action_menu` FOREIGN KEY (`application_id`, `menu_id`) REFERENCES `iam_menu` (`application_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：已改为差量写入。
- 判断：application_id/menu_id/action_id支持菜单操作读取；反向索引服务操作引用检查。本轮仅绑定差异增删。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_menu_action)。

### iam_tenant_app_entitlement — 保留（P3）

租户应用开通

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[CatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/CatalogRepository.java), [EntitlementRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/EntitlementRepository.java), [TenantInitializationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/TenantInitializationRepository.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_entitlement_app` (`application_id`)``；``UNIQUE KEY `uk_iam_entitlement` (`tenant_id`,`application_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_entitlement_app` FOREIGN KEY (`application_id`) REFERENCES `iam_application` (`id`)``；``CONSTRAINT `fk_iam_entitlement_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`)``。
- 被其他表引用：``iam_app_audience(tenant_id, application_id) → (tenant_id, application_id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按tenant_id/application_id唯一配置，时效是行属性。整体开通替换会删除并重建人群，属于独立业务语义，本轮保留并列为后续评审。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_tenant_app_entitlement)。

### iam_app_audience — 保留（P3）

租户应用可用人群配置

- 主键：`tenant_id / application_id`；宽度：16 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[CatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/CatalogRepository.java), [EntitlementRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/EntitlementRepository.java), [TenantInitializationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/TenantInitializationRepository.java)。
- 有效二级索引/业务唯一键：无。
- 本表引用外键：``CONSTRAINT `fk_iam_audience_entitlement` FOREIGN KEY (`tenant_id`, `application_id`) REFERENCES `iam_tenant_app_entitlement` (`tenant_id`, `application_id`)``。
- 被其他表引用：``iam_audience_member(tenant_id, application_id) → (tenant_id, application_id)``；``iam_audience_department(tenant_id, application_id) → (tenant_id, application_id)``；``iam_audience_group(tenant_id, application_id) → (tenant_id, application_id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：tenant_id/application_id就是配置身份；开通引用使用同一组合。独立保存仍清空重写子选择，且并发保护需与开通重置共同设计，本轮不改。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_app_audience)。

### iam_audience_member — 保留（P3）

应用人群显式成员

- 主键：`tenant_id / application_id / member_id`；宽度：24 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[CatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/CatalogRepository.java), [EntitlementRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/EntitlementRepository.java), [IamAudienceMemberEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamAudienceMemberEntity.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_audience_member` (`tenant_id`,`member_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_audience_member_config` FOREIGN KEY (`tenant_id`, `application_id`) REFERENCES `iam_app_audience` (`tenant_id`, `application_id`)``；``CONSTRAINT `fk_iam_audience_member` FOREIGN KEY (`tenant_id`, `member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按租户/应用读取所选成员，成员复合外键所需的反向索引由MySQL补建；本轮不将人群重建机械替换为差量。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_audience_member)。

### iam_audience_department — 保留（P3）

应用人群部门来源

- 主键：`tenant_id / application_id / department_id`；宽度：24 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[CatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/CatalogRepository.java), [EntitlementRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/EntitlementRepository.java), [IamAudienceDepartmentEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamAudienceDepartmentEntity.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_audience_dept` (`tenant_id`,`department_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_audience_dept_config` FOREIGN KEY (`tenant_id`, `application_id`) REFERENCES `iam_app_audience` (`tenant_id`, `application_id`)``；``CONSTRAINT `fk_iam_audience_dept` FOREIGN KEY (`tenant_id`, `department_id`) REFERENCES `iam_department` (`tenant_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按租户/应用读取部门及include_descendants，反向部门归属由外键索引支持；需连同人群并发及模式切换评估。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_audience_department)。

### iam_audience_group — 保留（P3）

应用人群用户组来源

- 主键：`tenant_id / application_id / group_id`；宽度：24 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[CatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/CatalogRepository.java), [EntitlementRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/EntitlementRepository.java), [IamAudienceGroupEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamAudienceGroupEntity.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_audience_group` (`tenant_id`,`group_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_audience_group_config` FOREIGN KEY (`tenant_id`, `application_id`) REFERENCES `iam_app_audience` (`tenant_id`, `application_id`)``；``CONSTRAINT `fk_iam_audience_group` FOREIGN KEY (`tenant_id`, `group_id`) REFERENCES `iam_tenant_group` (`tenant_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按租户/应用读取用户组来源，反向组索引由外键补建；需和人群保存事务一起评估。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_audience_group)。

### iam_plan — 保留（P3）

套餐目录

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[CatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/CatalogRepository.java), [InitializationCatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/InitializationCatalogRepository.java), [IamPlanEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamPlanEntity.java)。
- 有效二级索引/业务唯一键：无。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：``iam_plan_application(plan_id) → (id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按ID锁定套餐版本；修改套餐应用目录不回写已有租户的开通事实。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_plan)。

### iam_plan_application — 保留（P3）

套餐包含的应用

- 主键：`plan_id / application_id`；宽度：16 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[CatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/CatalogRepository.java), [InitializationCatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/InitializationCatalogRepository.java), [IamPlanApplicationEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamPlanApplicationEntity.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_plan_application_app` (`application_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_plan_application_app` FOREIGN KEY (`application_id`) REFERENCES `iam_application` (`id`)``；``CONSTRAINT `fk_iam_plan_application_plan` FOREIGN KEY (`plan_id`) REFERENCES `iam_plan` (`id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：已改为差量写入。
- 判断：plan_id/application_id支持整套餐读取并自然去重；应用外键的反向索引由MySQL补建。本轮保存按集合差量。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_plan_application)。

### iam_role_definition — 保留（P3）

角色身份与当前快照指针

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[InitializationCatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/InitializationCatalogRepository.java), [RoleRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/RoleRepository.java), [TenantCurrentRoleRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/TenantCurrentRoleRepository.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_role_current` (`current_revision_id`,`id`)``；``KEY `fk_iam_role_tenant` (`tenant_id`)``；``UNIQUE KEY `uk_iam_role_code` (`domain`,`tenant_key`,`code`)``；``UNIQUE KEY `uk_iam_role_kind` (`id`,`kind`)``；``UNIQUE KEY `uk_iam_role_owner` (`domain`,`tenant_key`,`id`,`kind`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_role_current` FOREIGN KEY (`current_revision_id`, `id`) REFERENCES `iam_role_revision` (`id`, `role_id`)``；``CONSTRAINT `fk_iam_role_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`)``。
- 被其他表引用：``iam_role_revision(role_id, kind) → (id, kind)``；``iam_role_assignment(domain, role_tenant_key, role_id, role_kind) → (domain, tenant_key, id, kind)``；``iam_delegation_role(domain, role_tenant_key, role_id, role_kind) → (domain, tenant_key, id, kind)``；``iam_role_application_task(role_id) → (id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：角色身份与快照分开；current_revision_id用于共享/租户当前权限，平台保留固定版本。domain/tenant_key/id/kind约束角色域归属。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_role_definition)。

### iam_role_revision — 保留（P3）

角色不可变权限快照与可编辑版本元数据

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[AuthorizationEvaluationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/evaluation/AuthorizationEvaluationRepository.java), [DelegationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/DelegationRepository.java), [InitializationCatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/InitializationCatalogRepository.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_revision_base` (`base_revision_id`,`base_kind`)``；``KEY `fk_iam_revision_role` (`role_id`,`kind`)``；``UNIQUE KEY `uk_iam_revision_kind` (`id`,`kind`)``；``UNIQUE KEY `uk_iam_revision_number` (`role_id`,`revision`)``；``UNIQUE KEY `uk_iam_revision_owner` (`id`,`role_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_revision_base` FOREIGN KEY (`base_revision_id`, `base_kind`) REFERENCES `iam_role_revision` (`id`, `kind`)``；``CONSTRAINT `fk_iam_revision_role` FOREIGN KEY (`role_id`, `kind`) REFERENCES `iam_role_definition` (`id`, `kind`)``。
- 被其他表引用：``iam_role_definition(current_revision_id, id) → (id, role_id)``；``iam_role_revision(base_revision_id, base_kind) → (id, kind)``；``iam_role_parameter(revision_id) → (id)``；``iam_role_grant(revision_id) → (id)``；``iam_role_delta(revision_id) → (id)``；``iam_delegation_role_revision(revision_id, revision_kind) → (id, kind)``；``iam_role_assignment(revision_id, revision_kind) → (id, kind)``；``iam_role_application_task(target_revision_id) → (id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按角色/发布序号唯一保存不可变权限；id/role_id、id/kind服务复合引用。发布创建新快照，不是同一关系的无效删插。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_role_revision)。

### iam_role_parameter — 需要运行数据验证（P2）

角色快照范围参数定义

- 主键：`revision_id / parameter_key`；宽度：520 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[RoleRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/RoleRepository.java), [TenantCurrentRoleRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/TenantCurrentRoleRepository.java), [IamRoleParameterEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamRoleParameterEntity.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_parameter_resource` (`resource_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_parameter_resource` FOREIGN KEY (`resource_id`) REFERENCES `iam_resource` (`id`)``；``CONSTRAINT `fk_iam_parameter_revision` FOREIGN KEY (`revision_id`) REFERENCES `iam_role_revision` (`id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按revision_id加载参数定义；parameter_key是版本内自然键。128字符utf8mb4参数名放大聚簇键，但当前缺少实际参数数和缓存命中证据。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_role_parameter)。

后续验证：采集该表行数、主键/二级索引体积、写入频率、代表SQL的EXPLAIN及缓冲池命中；使用匿名等量数据比较原结构与候选结构的总写入、查询和空间。结构变更必须单独批准并提供外键、数据校验、发布及回滚方案。

### iam_role_grant — 保留（P3）

角色快照操作授权

- 主键：`revision_id / action_id`；宽度：16 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[CatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/CatalogRepository.java), [RoleRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/RoleRepository.java), [TenantCurrentRoleRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/TenantCurrentRoleRepository.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_grant_action` (`action_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_grant_action` FOREIGN KEY (`action_id`) REFERENCES `iam_action` (`id`)``；``CONSTRAINT `fk_iam_grant_revision` FOREIGN KEY (`revision_id`) REFERENCES `iam_role_revision` (`id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按版本一次读取操作/范围JSON，不可变发布创建新版本；action_id外键隐式索引支持引用校验。不得跨版本原地差量覆盖。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_role_grant)。

### iam_role_delta — 保留（P3）

历史租户角色定制差异

- 主键：`revision_id / action_id`；宽度：16 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[CatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/CatalogRepository.java), [RoleRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/RoleRepository.java), [TenantCurrentRoleRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/TenantCurrentRoleRepository.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_delta_action` (`action_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_delta_action` FOREIGN KEY (`action_id`) REFERENCES `iam_action` (`id`)``；``CONSTRAINT `fk_iam_delta_revision` FOREIGN KEY (`revision_id`) REFERENCES `iam_role_revision` (`id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：历史固定基础的角色差异按版本/操作唯一；租户当前快照模型另有归属。本轮保留历史记录及快照生命周期。
- DDL证据：[002_catalog_role.sql](../../../databases/iam/002_catalog_role.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_role_delta)。

### iam_delegation_grant — 保留（P3）

受限委派及期限上限

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[AuthorizationEvaluationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/evaluation/AuthorizationEvaluationRepository.java), [AssignmentRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/AssignmentRepository.java), [DelegationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/DelegationRepository.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_delegation_platform_admin` (`platform_administrator_id`,`status`,`valid_until`)``；``KEY `idx_iam_delegation_tenant_admin` (`tenant_id`,`tenant_administrator_id`,`status`,`valid_until`)``；``UNIQUE KEY `uk_iam_delegation_domain` (`domain`,`tenant_key`,`id`)``；``UNIQUE KEY `uk_iam_delegation_tenant` (`tenant_id`,`id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_delegation_platform_admin` FOREIGN KEY (`platform_administrator_id`) REFERENCES `iam_platform_member` (`id`)``；``CONSTRAINT `fk_iam_delegation_tenant_admin` FOREIGN KEY (`tenant_id`, `tenant_administrator_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`)``。
- 被其他表引用：``iam_delegation_role_revision(delegation_id) → (id)``；``iam_delegation_recipient_member(domain, tenant_key, delegation_id) → (domain, tenant_key, id)``；``iam_delegation_recipient_department(tenant_id, delegation_id) → (tenant_id, id)``；``iam_delegation_action_ceiling(delegation_id) → (id)``；``iam_role_assignment(domain, tenant_key, delegation_grant_id) → (domain, tenant_key, id)``；``iam_delegation_role(tenant_id, delegation_id) → (tenant_id, id)``；``iam_delegation_resource_ceiling(delegation_id) → (id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按管理员/status/有效期寻找委派；domain/tenant_key/id及tenant_id/id用于所属域复合外键。撤销与重建子关系不能拆掉派生授权校验。
- DDL证据：[003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_delegation_grant)。

### iam_delegation_role_revision — 保留（P3）

平台委派允许的固定角色版本

- 主键：`delegation_id / revision_id`；宽度：16 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[AssignmentRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/AssignmentRepository.java), [DelegationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/DelegationRepository.java), [IamMembershipSql.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/IamMembershipSql.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_delegation_role_revision` (`revision_id`,`revision_kind`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_delegation_role_revision` FOREIGN KEY (`revision_id`, `revision_kind`) REFERENCES `iam_role_revision` (`id`, `kind`)``；``CONSTRAINT `fk_iam_delegation_role_source` FOREIGN KEY (`delegation_id`) REFERENCES `iam_delegation_grant` (`id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：平台委派按委派ID读取固定版本白名单；复合版本种类外键校验有效引用。委派替换重建子行本轮只审查。
- DDL证据：[003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_delegation_role_revision)。

### iam_delegation_recipient_member — 保留（P3）

委派允许的接收成员

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：IAM Leaf号段发号，应用显式填入BigInteger/IdType.INPUT。
- 主要访问/写入：[DelegationRecipientRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/DelegationRecipientRepository.java), [DelegationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/DelegationRepository.java), [IamMembershipSql.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/IamMembershipSql.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_recipient_delegation` (`domain`,`tenant_key`,`delegation_id`)``；``KEY `fk_iam_recipient_platform` (`platform_member_id`)``；``KEY `fk_iam_recipient_tenant` (`tenant_id`,`tenant_member_id`)``；``UNIQUE KEY `uk_iam_delegation_recipient` (`delegation_id`,`member_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_recipient_delegation` FOREIGN KEY (`domain`, `tenant_key`, `delegation_id`) REFERENCES `iam_delegation_grant` (`domain`, `tenant_key`, `id`)``；``CONSTRAINT `fk_iam_recipient_platform` FOREIGN KEY (`platform_member_id`) REFERENCES `iam_platform_member` (`id`)``；``CONSTRAINT `fk_iam_recipient_tenant` FOREIGN KEY (`tenant_id`, `tenant_member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：已补显式id主键，应用复用IAM Leaf发号并使用IdType.INPUT；原delegation_id/member_id业务唯一键、生成列及全部外键/CHECK保留。两域入口显式赋值，历史回填从iam高水位预留号段，不用ID解释业务顺序。
- DDL证据：[003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_delegation_recipient_member)。

### iam_delegation_recipient_department — 保留（P3）

委派允许的接收部门

- 主键：`delegation_id / department_id`；宽度：16 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[DelegationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/DelegationRepository.java), [IamMembershipSql.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/IamMembershipSql.java), [TenantLiveOptionSql.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/TenantLiveOptionSql.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_recipient_dept_delegation` (`tenant_id`,`delegation_id`)``；``KEY `fk_iam_recipient_dept_department` (`tenant_id`,`department_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_recipient_dept_delegation` FOREIGN KEY (`tenant_id`, `delegation_id`) REFERENCES `iam_delegation_grant` (`tenant_id`, `id`)``；``CONSTRAINT `fk_iam_recipient_dept_department` FOREIGN KEY (`tenant_id`, `department_id`) REFERENCES `iam_department` (`tenant_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按delegation_id读取部门白名单；tenant_id主要用于复合所属租户外键。include_descendants是属性，整体替换涉及委派上限，本轮保留。
- DDL证据：[003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_delegation_recipient_department)。

### iam_delegation_action_ceiling — 保留（P3）

平台委派逐操作范围上限

- 主键：`delegation_id / action_id`；宽度：16 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[DelegationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/DelegationRepository.java), [IamDelegationActionCeilingEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamDelegationActionCeilingEntity.java), [AuthorizationCandidateSql.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/mapper/AuthorizationCandidateSql.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_ceiling_action` (`action_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_ceiling_action` FOREIGN KEY (`action_id`) REFERENCES `iam_action` (`id`)``；``CONSTRAINT `fk_iam_ceiling_delegation` FOREIGN KEY (`delegation_id`) REFERENCES `iam_delegation_grant` (`id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：平台委派按委派ID/操作读取范围及绑定；与角色白名单、接收者、派生分配一致性共事务，本轮保留子集重建。
- DDL证据：[003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_delegation_action_ceiling)。

### iam_role_assignment — 需要运行数据验证（P2）

成员或用户组角色分配

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[AssignmentRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/AssignmentRepository.java), [DelegationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/DelegationRepository.java), [GroupRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/GroupRepository.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_assignment_current_role` (`domain`,`role_tenant_key`,`role_id`,`role_kind`)``；``KEY `fk_iam_assignment_delegation` (`domain`,`tenant_key`,`delegation_grant_id`)``；``KEY `fk_iam_assignment_revision` (`revision_id`,`revision_kind`)``；``KEY `idx_iam_assignment_current_role` (`role_id`,`status`)``；``KEY `idx_iam_assignment_delegation` (`delegation_grant_id`,`status`)``；``KEY `idx_iam_assignment_platform_group` (`platform_group_id`,`status`,`valid_until`)``；``KEY `idx_iam_assignment_platform_member` (`platform_member_id`,`status`,`valid_until`)``；``KEY `idx_iam_assignment_tenant_group` (`tenant_id`,`tenant_group_id`,`status`,`valid_until`)``；``KEY `idx_iam_assignment_tenant_member` (`tenant_id`,`tenant_member_id`,`status`,`valid_until`)``；``KEY `ix_iam_assignment_revision_status` (`revision_id`,`status`,`id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_assignment_current_role` FOREIGN KEY (`domain`, `role_tenant_key`, `role_id`, `role_kind`) REFERENCES `iam_role_definition` (`domain`, `tenant_key`, `id`, `kind`)``；``CONSTRAINT `fk_iam_assignment_delegation` FOREIGN KEY (`domain`, `tenant_key`, `delegation_grant_id`) REFERENCES `iam_delegation_grant` (`domain`, `tenant_key`, `id`)``；``CONSTRAINT `fk_iam_assignment_platform_group` FOREIGN KEY (`platform_group_id`) REFERENCES `iam_platform_group` (`id`)``；``CONSTRAINT `fk_iam_assignment_platform_member` FOREIGN KEY (`platform_member_id`) REFERENCES `iam_platform_member` (`id`)``；``CONSTRAINT `fk_iam_assignment_revision` FOREIGN KEY (`revision_id`, `revision_kind`) REFERENCES `iam_role_revision` (`id`, `kind`)``；``CONSTRAINT `fk_iam_assignment_tenant_group` FOREIGN KEY (`tenant_id`, `tenant_group_id`) REFERENCES `iam_tenant_group` (`tenant_id`, `id`)``；``CONSTRAINT `fk_iam_assignment_tenant_member` FOREIGN KEY (`tenant_id`, `tenant_member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`)``。
- 被其他表引用：``iam_role_application_item(assignment_id) → (id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：独立分配实体含状态/有效期/版本和历史引用，适合稳定ID；成员/组/角色/委派/版本批处理索引分别服务不同入口。多个索引的真实成本需负载数据验证。
- DDL证据：[003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_role_assignment)。

后续验证：采集该表行数、主键/二级索引体积、写入频率、代表SQL的EXPLAIN及缓冲池命中；使用匿名等量数据比较原结构与候选结构的总写入、查询和空间。结构变更必须单独批准并提供外键、数据校验、发布及回滚方案。

### iam_delegation_role — 保留（P3）

租户委派允许的角色身份

- 主键：`delegation_id / role_id`；宽度：16 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[TenantLiveOptionSql.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/TenantLiveOptionSql.java), [TenantLiveMapper.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/mapper/TenantLiveMapper.java), [migrate.py](../../../tools/iam/tenant_live/migrate.py)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_live_delegation_role` (`domain`,`role_tenant_key`,`role_id`,`role_kind`)``；``KEY `fk_iam_live_delegation` (`tenant_id`,`delegation_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_live_delegation_role` FOREIGN KEY (`domain`, `role_tenant_key`, `role_id`, `role_kind`) REFERENCES `iam_role_definition` (`domain`, `tenant_key`, `id`, `kind`)``；``CONSTRAINT `fk_iam_live_delegation` FOREIGN KEY (`tenant_id`, `delegation_id`) REFERENCES `iam_delegation_grant` (`tenant_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：租户委派按委派ID/角色身份读取，角色归属复合外键不能因role_id全局唯一而省略；清子行与派生授权重验有关，本轮保留。
- DDL证据：[003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_delegation_role)。

### iam_delegation_resource_ceiling — 保留（P3）

租户委派逐资源范围上限

- 主键：`delegation_id / resource_id`；宽度：16 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[TenantLiveOptionSql.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/TenantLiveOptionSql.java), [TenantLiveMapper.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/mapper/TenantLiveMapper.java), [migrate.py](../../../tools/iam/tenant_live/migrate.py)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_live_resource` (`resource_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_live_resource_delegation` FOREIGN KEY (`delegation_id`) REFERENCES `iam_delegation_grant` (`id`)``；``CONSTRAINT `fk_iam_live_resource` FOREIGN KEY (`resource_id`) REFERENCES `iam_resource` (`id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：租户当前模型按委派ID/资源读取范围上限，绑定JSON不构成主键；不把此路径改成平台固定版本语义。
- DDL证据：[003_assignment_delegation.sql](../../../databases/iam/003_assignment_delegation.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_delegation_resource_ceiling)。

### iam_default_policy_revision — 保留（P3）

默认通讯录或字段策略固定版本

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[InitializationCatalogRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/InitializationCatalogRepository.java), [PolicyWriteRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/policy/PolicyWriteRepository.java), [IamDefaultPolicyRevisionEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamDefaultPolicyRevisionEntity.java)。
- 有效二级索引/业务唯一键：``UNIQUE KEY `uk_iam_default_policy_kind` (`id`,`kind`)``；``UNIQUE KEY `uk_iam_default_policy_revision` (`kind`,`revision`)``。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：``iam_directory_policy(default_revision_id, default_kind) → (id, kind)``；``iam_field_policy(default_revision_id, default_kind) → (id, kind)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：策略kind/revision唯一，引用固定默认版本；发布为新快照，和关系原地替换不同。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_default_policy_revision)。

### iam_policy_selector — 保留（P3）

租户策略人员选择器

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[PolicyWriteRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/policy/PolicyWriteRepository.java), [IamPolicySelectorEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamPolicySelectorEntity.java)。
- 有效二级索引/业务唯一键：``UNIQUE KEY `uk_iam_selector_tenant` (`tenant_id`,`id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_selector_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`)``。
- 被其他表引用：``iam_policy_selector_member(tenant_id, selector_id) → (tenant_id, id)``；``iam_policy_selector_department(tenant_id, selector_id) → (tenant_id, id)``；``iam_directory_policy(tenant_id, default_selector_id) → (tenant_id, id)``；``iam_directory_rule(tenant_id, viewer_selector_id) → (tenant_id, id)``；``iam_directory_rule(tenant_id, target_selector_id) → (tenant_id, id)``；``iam_field_rule(tenant_id, viewer_selector_id) → (tenant_id, id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：选择器是租户内独立对象，成员及部门来源通过(tenant_id,id)引用；策略保存构造新选择器，不作为低风险差量目标。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_policy_selector)。

### iam_policy_selector_member — 保留（P3）

策略选择器显式成员

- 主键：`tenant_id / selector_id / member_id`；宽度：24 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[PolicyWriteRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/policy/PolicyWriteRepository.java), [IamPolicySelectorMemberEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamPolicySelectorMemberEntity.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_selector_member_member` (`tenant_id`,`member_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_selector_member_member` FOREIGN KEY (`tenant_id`, `member_id`) REFERENCES `iam_tenant_member` (`tenant_id`, `id`)``；``CONSTRAINT `fk_iam_selector_member_selector` FOREIGN KEY (`tenant_id`, `selector_id`) REFERENCES `iam_policy_selector` (`tenant_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：tenant_id/selector_id/member_id支持选择器展开；成员反向索引由外键补建。新选择器快照的首次插入应保留。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_policy_selector_member)。

### iam_policy_selector_department — 保留（P3）

策略选择器部门来源

- 主键：`tenant_id / selector_id / department_id`；宽度：24 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[PolicyWriteRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/policy/PolicyWriteRepository.java), [IamPolicySelectorDepartmentEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamPolicySelectorDepartmentEntity.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_selector_dept_dept` (`tenant_id`,`department_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_selector_dept_dept` FOREIGN KEY (`tenant_id`, `department_id`) REFERENCES `iam_department` (`tenant_id`, `id`)``；``CONSTRAINT `fk_iam_selector_dept_selector` FOREIGN KEY (`tenant_id`, `selector_id`) REFERENCES `iam_policy_selector` (`tenant_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按选择器展开部门范围及下级，部门反向索引由外键补建；必须保留选择器与租户一致性。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_policy_selector_department)。

### iam_directory_policy — 保留（P3）

租户通讯录策略及默认版本引用

- 主键：`tenant_id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[TenantInitializationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/TenantInitializationRepository.java), [PolicyWriteRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/policy/PolicyWriteRepository.java), [IamDirectoryPolicyEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamDirectoryPolicyEntity.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_directory_default_selector` (`tenant_id`,`default_selector_id`)``；``KEY `fk_iam_directory_default` (`default_revision_id`,`default_kind`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_directory_default_selector` FOREIGN KEY (`tenant_id`, `default_selector_id`) REFERENCES `iam_policy_selector` (`tenant_id`, `id`)``；``CONSTRAINT `fk_iam_directory_default` FOREIGN KEY (`default_revision_id`, `default_kind`) REFERENCES `iam_default_policy_revision` (`id`, `kind`)``；``CONSTRAINT `fk_iam_directory_policy_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`)``。
- 被其他表引用：``iam_directory_rule(tenant_id) → (tenant_id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：一租户一配置，以tenant_id直接作为主键；默认版本/选择器引用与本地版本锁分别承担不同语义。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_directory_policy)。

### iam_directory_rule — 保留（P3）

租户通讯录允许或拒绝规则

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[PolicyWriteRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/policy/PolicyWriteRepository.java), [IamDirectoryRuleEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamDirectoryRuleEntity.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_directory_rule_target` (`tenant_id`,`target_selector_id`)``；``KEY `fk_iam_directory_rule_viewer` (`tenant_id`,`viewer_selector_id`)``；``KEY `idx_iam_directory_rule` (`tenant_id`,`effect`,`id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_directory_rule_policy` FOREIGN KEY (`tenant_id`) REFERENCES `iam_directory_policy` (`tenant_id`)``；``CONSTRAINT `fk_iam_directory_rule_target` FOREIGN KEY (`tenant_id`, `target_selector_id`) REFERENCES `iam_policy_selector` (`tenant_id`, `id`)``；``CONSTRAINT `fk_iam_directory_rule_viewer` FOREIGN KEY (`tenant_id`, `viewer_selector_id`) REFERENCES `iam_policy_selector` (`tenant_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：规则有独立ID，tenant_id/effect/id用于租户规则匹配；允许和拒绝不是简单双向多对多边。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_directory_rule)。

### iam_field_policy — 保留（P3）

租户字段策略及操作规则

- 主键：`tenant_id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[TenantInitializationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/TenantInitializationRepository.java), [PolicyWriteRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/policy/PolicyWriteRepository.java), [IamFieldPolicyEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamFieldPolicyEntity.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_field_default` (`default_revision_id`,`default_kind`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_field_default` FOREIGN KEY (`default_revision_id`, `default_kind`) REFERENCES `iam_default_policy_revision` (`id`, `kind`)``；``CONSTRAINT `fk_iam_field_policy_tenant` FOREIGN KEY (`tenant_id`) REFERENCES `iam_tenant` (`id`)``。
- 被其他表引用：``iam_field_rule(tenant_id) → (tenant_id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：一租户一字段策略，tenant_id主键即业务身份；operation_rules为数组，本轮不改变默认及合并规则。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_field_policy)。

### iam_field_rule — 保留（P3）

租户场景字段可见性规则

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[PolicyWriteRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/policy/PolicyWriteRepository.java), [IamFieldRuleEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamFieldRuleEntity.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_field_rule_viewer` (`tenant_id`,`viewer_selector_id`)``；``KEY `idx_iam_field_rule_match` (`tenant_id`,`scenario`,`field_key`,`id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_field_rule_policy` FOREIGN KEY (`tenant_id`) REFERENCES `iam_field_policy` (`tenant_id`)``；``CONSTRAINT `fk_iam_field_rule_viewer` FOREIGN KEY (`tenant_id`, `viewer_selector_id`) REFERENCES `iam_policy_selector` (`tenant_id`, `id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按租户/场景/字段读取可见性规则；viewer_selector及target_scope是不同维度，不并入主键。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_field_rule)。

### iam_member_export — 需要运行数据验证（P2）

租户成员导出任务与成员快照

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[MemberExportRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/MemberExportRepository.java), [IamMemberExportEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamMemberExportEntity.java), [IamMemberExportMapper.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/mapper/IamMemberExportMapper.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_member_export_tenant_status` (`tenant_id`,`status`,`expires_at`)``。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：任务按ID定位，租户/status/到期时间索引支持状态及清理；JSON成员快照可能较大，需要实际任务量及保留周期。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_member_export)。

后续验证：采集该表行数、主键/二级索引体积、写入频率、代表SQL的EXPLAIN及缓冲池命中；使用匿名等量数据比较原结构与候选结构的总写入、查询和空间。结构变更必须单独批准并提供外键、数据校验、发布及回滚方案。

### iam_member_workbook — 需要运行数据验证（P2）

租户成员表格导入导出任务

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[MemberWorkbookRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/MemberWorkbookRepository.java), [IamMemberWorkbookEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamMemberWorkbookEntity.java), [IamMemberWorkbookMapper.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/mapper/IamMemberWorkbookMapper.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_member_workbook_actor` (`tenant_id`,`actor_member_id`,`created_at`)``；``KEY `idx_iam_member_workbook_status` (`tenant_id`,`status`,`expires_at`)``。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：任务按ID定位，操作者与状态索引支持历史和过期清理；工作簿BLOB与一次性密码需按24小时保留期清理。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_member_workbook)。

后续验证：采集该表行数、主键/二级索引体积、写入频率、代表SQL的EXPLAIN及缓冲池命中；使用匿名等量数据比较原结构与候选结构的总写入、查询和空间。结构变更必须单独批准并提供外键、数据校验、发布及回滚方案。

### iam_download_task — 需要运行数据验证（P2）

下载中心导出任务

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：受控迁移/建库工具，未发现在线业务写入入口。
- 有效二级索引/业务唯一键：``KEY `idx_iam_download_task_owner` (`tenant_id`,`actor_member_id`,`created_at`)``；``KEY `idx_iam_download_task_status` (`status`,`expires_at`)``。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：任务按ID定位，租户/成员/创建时间支持我的下载分页，状态/到期索引支持清理；文件在对象存储，7天删除对象、30天删除记录。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_download_task)。

后续验证：采集该表行数、主键/二级索引体积、写入频率、代表SQL的EXPLAIN及缓冲池命中；使用匿名等量数据比较原结构与候选结构的总写入、查询和空间。结构变更必须单独批准并提供外键、数据校验、发布及回滚方案。

### iam_authorization_audit — 需要运行数据验证（P2）

授权与组织操作脱敏审计及投递状态

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[IamAuthorizationAuditEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamAuthorizationAuditEntity.java), [IamRoleAssignmentMapper.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/mapper/IamRoleAssignmentMapper.java)。
- 有效二级索引/业务唯一键：``KEY `idx_iam_audit_actor` (`actor_account_id`,`occurred_at`,`id`)``；``KEY `idx_iam_audit_assignment_create` (`assignment_id`,`change_type`,`occurred_at`,`id`)``；``KEY `idx_iam_audit_delivery` (`delivered_at`,`id`)``；``KEY `idx_iam_audit_tenant_time` (`tenant_id`,`occurred_at`,`id`)``；``UNIQUE KEY `uk_iam_audit_event` (`event_id`)``。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：审计按租户时间、操作者、分配创建及待投递分别检索；event_id唯一承担幂等。需实际保留量、写入速率和执行计划才能判断索引增删。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_authorization_audit)。

后续验证：采集该表行数、主键/二级索引体积、写入频率、代表SQL的EXPLAIN及缓冲池命中；使用匿名等量数据比较原结构与候选结构的总写入、查询和空间。结构变更必须单独批准并提供外键、数据校验、发布及回滚方案。

### iam_migration_batch — 保留（P3）

历史身份迁移批次及验收状态

- 主键：`id`；宽度：256 字节（最大键值，不含记录头及变长开销）。
- 标识来源：历史迁移工具显式指定字符串批次标识。
- 主要访问/写入：受控迁移/建库工具，未发现在线业务写入入口。
- 有效二级索引/业务唯一键：无。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：``iam_migration_mapping(batch_id) → (id)``；``iam_migration_issue(batch_id) → (id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：迁移工具显式指定字符串批次标识，不是在线高频写入表；状态与verified_at共同限制验收过程。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_migration_batch)。

### iam_migration_mapping — 候选优化（P2）

历史标识到目标实体的人工处置映射

- 主键：`batch_id / source_table / source_id / target_type / target_id`；宽度：1792 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：受控迁移/建库工具，未发现在线业务写入入口。
- 有效二级索引/业务唯一键：无。
- 本表引用外键：``CONSTRAINT `fk_iam_mapping_batch` FOREIGN KEY (`batch_id`) REFERENCES `iam_migration_batch` (`id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：五个utf8mb4字符串构成宽自然主键，最大键值约1792字节（不含变长/页开销）；历史迁移已退出常规运行范围。若恢复大批导入，比较短代理键+原唯一键的总写入/空间，先采样再决定。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_migration_mapping)。

后续验证：采集该表行数、主键/二级索引体积、写入频率、代表SQL的EXPLAIN及缓冲池命中；使用匿名等量数据比较原结构与候选结构的总写入、查询和空间。结构变更必须单独批准并提供外键、数据校验、发布及回滚方案。

### iam_migration_issue — 保留（P3）

历史迁移阻塞项及处置记录

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：受控迁移/建库工具，未发现在线业务写入入口。
- 有效二级索引/业务唯一键：``KEY `idx_iam_issue_unresolved` (`batch_id`,`disposition`,`id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_issue_batch` FOREIGN KEY (`batch_id`) REFERENCES `iam_migration_batch` (`id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：独立问题ID，batch_id/disposition/id定位未解决问题；处置说明保留，不能将删除问题记录视作优化。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_migration_issue)。

### iam_role_application_task — 保留（P3）

平台角色固定版本批量应用任务

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[RoleApplicationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/RoleApplicationRepository.java), [IamRoleApplicationTaskEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamRoleApplicationTaskEntity.java), [IamRoleDefinitionMapper.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/mapper/IamRoleDefinitionMapper.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_role_application_target` (`target_revision_id`)``；``KEY `ix_iam_role_application_claim` (`status`,`lease_until`,`id`)``；``KEY `ix_iam_role_application_role` (`role_id`,`id`)``；``UNIQUE KEY `uk_iam_role_application_prepare` (`role_id`,`prepare_key`)``；``UNIQUE KEY `uk_iam_role_application_running` (`running_role_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_role_application_role` FOREIGN KEY (`role_id`) REFERENCES `iam_role_definition` (`id`)``；``CONSTRAINT `fk_iam_role_application_target` FOREIGN KEY (`target_revision_id`) REFERENCES `iam_role_revision` (`id`)``。
- 被其他表引用：``iam_role_application_item(task_id) → (id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按角色/准备键幂等；running_role_id唯一限制同角色一个RUNNING；状态/租约/id服务多实例领取。状态变化触发辅助索引维护是必要成本。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_role_application_task)。

### iam_role_application_item — 保留（P3）

角色版本应用的分配明细

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[RoleApplicationRepository.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/RoleApplicationRepository.java), [IamRoleApplicationItemEntity.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/entity/IamRoleApplicationItemEntity.java), [IamRoleApplicationItemMapper.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/mapper/IamRoleApplicationItemMapper.java)。
- 有效二级索引/业务唯一键：``KEY `fk_iam_role_application_item_assignment` (`assignment_id`)``；``KEY `ix_iam_role_application_items` (`task_id`,`status`,`id`)``；``UNIQUE KEY `uk_iam_role_application_assignment` (`task_id`,`assignment_id`)``。
- 本表引用外键：``CONSTRAINT `fk_iam_role_application_item_assignment` FOREIGN KEY (`assignment_id`) REFERENCES `iam_role_assignment` (`id`)``；``CONSTRAINT `fk_iam_role_application_item_task` FOREIGN KEY (`task_id`) REFERENCES `iam_role_application_task` (`id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：task_id/assignment_id唯一去重，task_id/status/id支持批执行；独立ID承载明细状态和配置，不能按纯关联表处理。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_role_application_item)。

### iam_live_authorization_batch — 保留（P3）

租户即时授权模型迁移批次

- 主键：`batch_id`；宽度：64 字节（最大键值，不含记录头及变长开销）。
- 标识来源：即时授权迁移工具显式指定字符串批次标识。
- 主要访问/写入：[migrate.py](../../../tools/iam/tenant_live/migrate.py)。
- 有效二级索引/业务唯一键：无。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：``iam_live_authorization_mapping(batch_id) → (batch_id)``。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：受控迁移工具显式批次标识与摘要，低频；正反迁移状态不等价于日常业务版本。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_live_authorization_batch)。

### iam_live_authorization_mapping — 保留（P3）

租户即时授权迁移原行快照与目标映射

- 主键：`batch_id / entity_type / entity_id`；宽度：104 字节（最大键值，不含记录头及变长开销）。
- 标识来源：组合关系/业务配置键，由绑定对象或受控工具提供。
- 主要访问/写入：[migrate.py](../../../tools/iam/tenant_live/migrate.py)。
- 有效二级索引/业务唯一键：无。
- 本表引用外键：``CONSTRAINT `iam_live_authorization_mapping_ibfk_1` FOREIGN KEY (`batch_id`) REFERENCES `iam_live_authorization_batch` (`batch_id`)``。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：批次/实体类型/实体ID自然唯一，保存回退原行；键宽约104字节，工作量取决于实际迁移规模，未恢复在线写路径。
- DDL证据：[004_policy_audit_migration.sql](../../../databases/iam/004_policy_audit_migration.sql)；列定义见[数据字典](DATA-DICTIONARY.md#iam_live_authorization_mapping)。

### biz_leaf_alloc — 保留（P3）

Leaf业务号段分配高水位

- 主键：`biz_tag`；宽度：512 字节（最大键值，不含记录头及变长开销）。
- 标识来源：配置的业务标签字符串，原行更新号段高水位。
- 主要访问/写入：[BizLeafAlloc.java](../../../ingot-service/ingot-iam/ingot-iam-api/src/main/java/com/ingot/cloud/iam/api/model/domain/BizLeafAlloc.java), [IDAllocMapper.java](../../../ingot-framework/ingot-id/src/main/java/com/ingot/framework/id/leaf/segment/dao/IDAllocMapper.java)。
- 有效二级索引/业务唯一键：无。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按业务标签锁定并领取号段，高水位增长不代表新增记录。biz_tag短字符串业务主键合适；禁止为改善顺序插入而重置高水位。
- DDL证据：[005_auxiliary.sql](../../../databases/iam/005_auxiliary.sql)；列定义见[数据字典](DATA-DICTIONARY.md#biz_leaf_alloc)。

### platform_dict — 保留（P3）

多作用域字典及字典项

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[PlatformDict.java](../../../ingot-service/ingot-iam/ingot-iam-api/src/main/java/com/ingot/cloud/iam/api/model/domain/PlatformDict.java)。
- 有效二级索引/业务唯一键：``KEY `idx_dict_code` (`code`) USING BTREE``；``KEY `idx_dict_pid` (`pid`) USING BTREE``；``KEY `idx_dict_scope` (`scope_type`,`tenant_id`,`app_id`) USING BTREE``；``KEY `idx_dict_type_status` (`type`,`status`) USING BTREE``。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：树父节点、编码、类型状态和作用域各有读取索引；code当前不是数据库唯一键，不能无业务确认直接加唯一约束。
- DDL证据：[005_auxiliary.sql](../../../databases/iam/005_auxiliary.sql)；列定义见[数据字典](DATA-DICTIONARY.md#platform_dict)。

### security_event — 需要运行数据验证（P2）

统一安全事件（canonical）

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：数据库AUTO_INCREMENT。
- 主要访问/写入：[MySqlSecurityEventQueryRepository.java](../../../ingot-framework/ingot-security/ingot-security-event-store-mysql/src/main/java/com/ingot/framework/security/recording/store/mysql/MySqlSecurityEventQueryRepository.java), [RemoteSecurityPolicyService.java](../../../ingot-service/ingot-security/ingot-security-api/src/main/java/com/ingot/cloud/security/api/rpc/RemoteSecurityPolicyService.java), [CompositeSecurityEventPort.java](../../../ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/java/com/ingot/framework/security/account/adapter/port/CompositeSecurityEventPort.java)。
- 有效二级索引/业务唯一键：``KEY `idx_event_type` (`event_type`,`received_at`)``；``KEY `idx_received_id` (`received_at`,`id`)``；``KEY `idx_tenant_user` (`tenant_id`,`user_id`)``；``KEY `idx_trace` (`trace_id`)``；``UNIQUE KEY `uk_event_id` (`event_id`)``。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：数据库自增聚簇键，事件幂等、时间、用户、追踪索引分别服务入口；事件量和保留策略决定成本。event_id允许NULL，因此缺失幂等ID不互斥。
- DDL证据：[005_auxiliary.sql](../../../databases/iam/005_auxiliary.sql)；列定义见[数据字典](DATA-DICTIONARY.md#security_event)。

后续验证：采集该表行数、主键/二级索引体积、写入频率、代表SQL的EXPLAIN及缓冲池命中；使用匿名等量数据比较原结构与候选结构的总写入、查询和空间。结构变更必须单独批准并提供外键、数据校验、发布及回滚方案。

### sys_social_details — 保留（P3）

社交登录渠道配置

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[SysSocialDetails.java](../../../ingot-service/ingot-iam/ingot-iam-api/src/main/java/com/ingot/cloud/iam/api/model/domain/SysSocialDetails.java)。
- 有效二级索引/业务唯一键：无。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：按ID及渠道配置定位，第三方app_id为字符串客户端标识；旧列名不意味着当前IAM数字应用ID。
- DDL证据：[005_auxiliary.sql](../../../databases/iam/005_auxiliary.sql)；列定义见[数据字典](DATA-DICTIONARY.md#sys_social_details)。

### sys_user_social — 需要运行数据验证（P2）

全局账号社交身份绑定

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[SysUserSocial.java](../../../ingot-service/ingot-iam/ingot-iam-api/src/main/java/com/ingot/cloud/iam/api/model/domain/SysUserSocial.java)。
- 有效二级索引/业务唯一键：``KEY `idx_unique_type_user` (`unique_id`,`type`,`user_id`) USING BTREE``。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：独立绑定ID，unique_id/type/user_id反查渠道身份；是否需要其他方向查询和业务唯一约束需实际入口与历史重复数据共同验证。
- DDL证据：[005_auxiliary.sql](../../../databases/iam/005_auxiliary.sql)；列定义见[数据字典](DATA-DICTIONARY.md#sys_user_social)。

后续验证：采集该表行数、主键/二级索引体积、写入频率、代表SQL的EXPLAIN及缓冲池命中；使用匿名等量数据比较原结构与候选结构的总写入、查询和空间。结构变更必须单独批准并提供外键、数据校验、发布及回滚方案。

### sys_tenant_plan_record — 保留（P3）

历史组织套餐购买记录

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：服务端提供标识；IAM目录含预留值，历史辅助记录沿既有发号。
- 主要访问/写入：[SysTenantPlanRecord.java](../../../ingot-service/ingot-iam/ingot-iam-api/src/main/java/com/ingot/cloud/iam/api/model/domain/SysTenantPlanRecord.java)。
- 有效二级索引/业务唯一键：无。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：历史购买记录是事实实体，保留稳定ID；INT plan_id与当前BIGINT套餐并非可直接迁移的外键，本轮仅说明。
- DDL证据：[005_auxiliary.sql](../../../databases/iam/005_auxiliary.sql)；列定义见[数据字典](DATA-DICTIONARY.md#sys_tenant_plan_record)。

### account_lock_state — 保留（P3）

账号锁定状态表

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：数据库AUTO_INCREMENT。
- 主要访问/写入：[SysUser.java](../../../ingot-service/ingot-iam/ingot-iam-api/src/main/java/com/ingot/cloud/iam/api/model/domain/SysUser.java), [IamRoleAssignmentMapper.java](../../../ingot-service/ingot-iam/ingot-iam-provider/src/main/java/com/ingot/cloud/iam/persistence/mapper/IamRoleAssignmentMapper.java), [MemberUser.java](../../../ingot-service/ingot-member/ingot-member-api/src/main/java/com/ingot/cloud/member/api/model/domain/MemberUser.java)。
- 有效二级索引/业务唯一键：``KEY `idx_locked` (`locked`,`locked_until`) USING BTREE``；``UNIQUE KEY `uk_user_id_type` (`user_id`,`user_type`)``。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：数据库自增ID，user_id/user_type业务唯一；locked/locked_until服务解锁扫描。框架持久化归属保留，不迁入IAM自有Mapper。
- DDL证据：[account_lock_state.sql](../../../ingot-framework/ingot-security/ingot-security-account/ingot-security-account-adapter/src/main/resources/sql/account_lock_state.sql)；列定义见[数据字典](DATA-DICTIONARY.md#account_lock_state)。

### password_history — 保留（P3）

密码历史记录（环形缓冲）

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：数据库AUTO_INCREMENT。
- 主要访问/写入：[PasswordHistoryServiceImpl.java](../../../ingot-framework/ingot-security/ingot-security-credential-data/src/main/java/com/ingot/framework/security/credential/data/service/PasswordHistoryServiceImpl.java)。
- 有效二级索引/业务唯一键：``KEY `idx_created_at` (`created_at`)``；``UNIQUE KEY `uk_user_sequence` (`user_id`,`sequence_number`)``。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：保留原自增主键及user_id/sequence_number业务唯一键；经隔离MySQL三态对照后删除重叠idx_user_id。实际PasswordHistoryServiceImpl按用户查询/覆盖槽位/删除；最近历史仍按created_at排序，本轮不改变环形历史行为。
- DDL证据：[add_password_history.sql](../../../ingot-framework/ingot-security/ingot-security-credential-data/src/main/resources/sql/add_password_history.sql)；列定义见[数据字典](DATA-DICTIONARY.md#password_history)。

### password_expiration — 需要运行数据验证（P2）

密码过期信息

- 主键：`id`；宽度：8 字节（最大键值，不含记录头及变长开销）。
- 标识来源：数据库AUTO_INCREMENT。
- 主要访问/写入：受控迁移/建库工具，未发现在线业务写入入口。
- 有效二级索引/业务唯一键：``KEY `idx_expires_at` (`expires_at`)``；``KEY `idx_next_warning_at` (`next_warning_at`)``；``UNIQUE KEY `uk_user_id` (`user_id`)``。
- 本表引用外键：DDL未声明；业务归属仍须由消费方校验。
- 被其他表引用：未声明入向外键。
- 关系重写：参照下述入口语义；本轮保留该表写入生命周期。
- 判断：自增ID，user_id唯一，expires_at和next_warning_at服务到期和提醒扫描；宽时效扫描成本需实际用户量及调度统计。
- DDL证据：[add_password_history.sql](../../../ingot-framework/ingot-security/ingot-security-credential-data/src/main/resources/sql/add_password_history.sql)；列定义见[数据字典](DATA-DICTIONARY.md#password_expiration)。

后续验证：采集该表行数、主键/二级索引体积、写入频率、代表SQL的EXPLAIN及缓冲池命中；使用匿名等量数据比较原结构与候选结构的总写入、查询和空间。结构变更必须单独批准并提供外键、数据校验、发布及回滚方案。
