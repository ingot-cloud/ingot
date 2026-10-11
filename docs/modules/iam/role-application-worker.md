# 角色版本应用 worker

角色版本应用只处理平台自定义角色的固定版本分配。工作线程依据数据库保存的账号和平台成员标识恢复当前有效身份；角色编辑权限不能代替独立的分配升级治理资格。任务不保存登录令牌，不继承 HTTP 身份。

## 代码职责

`RoleApplicationScheduler` 通过 `RoleApplicationWork` 调用发现与单批执行。`RoleApplicationService` 管理计划、集合、明细状态、幂等和事务；内部 `RoleApplicationDomainSupport` 封装业务域访问。目前只有 `PlatformRoleApplicationDomainSupport`，直接复用既有角色、范围、委派和分配服务，不通过注册中心或配置动态选择域。

当前任务没有租户上下文，本轮职责分离不会开放租户应用或独立 App 用户授权。租户接入需要另行确定任务上下文、路由和治理门禁；现有租户成员使用 App 不自动构成新的授权域。

## 运行契约

- 确认进入 RUNNING 的事务提交后，本地立即按任务 ID 排队，再通过独立线程发送跨节点提示。通知仅作唤醒，不携带授权；丢失或重复由去重及数据库发现补偿。
- 默认每节点在一次正常发现完成 60 秒后再次扫描，首次随机错峰；失败按 60/120/240/300 秒退避。续批默认 250 毫秒，本地及发送队列容量 256。
- 数据库 UTC 时间确定 30 秒租约；每批最多 100 条，同一角色最多一个 RUNNING 任务。锁按授权→角色→任务/分配取得，分配、明细和审计同事务写入。
- 每批恢复当前有效身份并重新检查授权、范围和委派。冲突、撤权或依赖故障回滚当前批次并暂停；已提交批次保留。继续处理需重新预检未完成项并再次确认。
- 提交后沿用授权及字段缓存失效，普通读取保留既有缓存窗口，写入使用 fresh 校验。适配器不缓存准入，不增加逐条身份或角色查询。
- 退出时停止接收和安排新批次，最多等待当前批次 30 秒，不强制中断提交。其他节点在租约过期后接续。

配置前缀保持 `ingot.iam.role-applications`，无需新增迁移或前端变更。关闭本节点 `worker-enabled` 后仍可向其他节点广播；全部节点关闭时任务留在数据库等待恢复。

## 与 TSS 共存

worker 未通过 `@ScheduledTask` 或 `@Scheduled` 注册，也不共用 Spring TSS 的调度与执行池。切换 `ingot.tss.type` 不切换 worker；两者的开关和执行状态独立。

本轮不使用 TSS Cron 替换角色发现，因为当前扫描还包含错峰、固定延时和错误退避。未来接入时必须避免双重扫描；TSS 只负责请求发现/唤醒，角色任务的租约、业务校验、状态和审计仍由 IAM 承担。详见 [TSS 职责说明](../task-scheduler-system/tss-architecture.md)。

当前已验收规则见 [角色版本应用基线](../../../specs/current/iam/role-version-application/SPEC.md)；职责分离的实施与验收记录见 [已归档 change](../../../specs/changes/archive/2026/20261011-iam-role-application-domain-support/README.md)。
