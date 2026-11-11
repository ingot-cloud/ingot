# 源表结构盘点（T01 输入）

> 范围处置（2026-09-15）：本次按全新系统建设，旧数据迁移、源快照预检和切换演练已退出必需交付范围，T02/T14/T15 取消后续实施。本文件保留为历史方案与已开展工作记录，不作为本次门禁、不恢复旧表依赖；若未来需要迁移，另行对齐并建 change。辅助功能和安全能力仍须保留，持久化归属见 REMEDIATION。

来源仅为 `databases/ingot_core.sql` 的 CREATE TABLE 声明，不读取或输出 INSERT 数据。本清单不是实际快照 preflight 报告；未执行迁移、状态比较、授权等价证明或登录验证。

仓库结构共 28 张表；以下逻辑去向依据 MIGRATION，不代表目标 DDL 已完成。实际源库多出的表必须报告并处置，不能按本清单静默跳过。

| 源表 | 逻辑去向 / 检查边界 |
|---|---|
| `account_lock_state` | 保留账号锁定事实，不能只迁 sys_user.locked |
| `biz_leaf_alloc` | 辅助发号领域；验证业务键和高水位不倒退 |
| `password_expiration` | 保留账号密码到期、强制改密及宽限状态 |
| `password_history` | 保留账号密码历史；摘要不进入报告 |
| `platform_app` | Application；旧默认开放转换为显式开通事实 |
| `platform_dict` | 保留字典事实及其平台/租户/应用归属 |
| `platform_menu` | Menu；导航不推导操作权限 |
| `platform_permission` | Action 及展示分组；显式映射旧权限码 |
| `platform_role` | RoleDefinition / RoleRevision；治理权必须明确处置 |
| `platform_role_permission` | 版本 RoleGrant；与范围共同映射 |
| `platform_menu_permission` | Menu 与 Action 关联；保留 ANY/ALL 语义 |
| `platform_resource` | Resource；补齐范围与字段能力及执行适配器 |
| `platform_role_data_rule` | 角色版本逐操作范围；孤立规则阻塞 |
| `tenant_role_data_rule_private` | 本地角色范围或共享差异；无法等价转换则阻塞 |
| `security_event` | 保留历史事实与旧来源标识，不虚构新授权事件 |
| `sys_social_details` | 保留社会化配置；验证密钥依赖及租户绑定 |
| `sys_tenant` | Tenant；所有者映射不明确则阻塞 |
| `sys_tenant_plan` | 保留套餐及明确应用映射 |
| `sys_tenant_plan_record` | 保留套餐应用历史与开通来源 |
| `sys_user` | Account；保留 ID、凭证及状态，组织资料分离 |
| `sys_user_social` | 保留社会化身份关联并验证配置 |
| `sys_user_tenant` | TenantMember；成员 ID 映射及 account+tenant 唯一性；平台身份按已确认 D01 使用显式映射 |
| `tenant_app_config` | TenantAppEntitlement；保留禁用、期限和来源 |
| `tenant_dept` | Department；禁止跨租户、循环和孤儿 |
| `tenant_role_permission_private` | 自定义 RoleGrant 或共享 ADD 差异；消除基础重合 |
| `tenant_role_private` | 本地 RoleDefinition / RoleRevision |
| `tenant_role_user_private` | RoleAssignment；部门转换为命名参数，不按名称自动授予 |
| `tenant_user_dept_private` | MemberDepartment；保留多部门与主部门，验证关系归属 |

会话、令牌及授权缓存不在此 MySQL 清单中，不应因此认为它们可迁移。实际迁移仍须验证相关配置、存储位置与清理/重新登录流程。
