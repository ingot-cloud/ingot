# 同源双入口目标环境发布验收

> 状态：draft
> 创建日期：2026-10-31
> 更新日期：2026-11-11
> 类型：已验收双入口实现的首次部署环境验收。

平台端 `apps/admin-platform` 与租户端 `apps/admin` 各自包含 `/login/`、管理页面和同源 `/api`。默认使用平台、租户两个 HTTPS 域名；纯 IP HTTPS 双端口是可选部署方案，按实际目标选择验收。独立登录站及四站架构已退役。

原同源登录变更已由用户确认平台/租户登录、退出、切换组织和多标签页本地验收通过并归档。后续 IAM 验收补充了真实服务登录、锁定恢复、改密及会话规则证据，前端登录外观与错误提示也已验收归档。本 change 只承接这些证据尚未覆盖的目标环境项目，不将已有本地验收重新设为待办，也不据此宣称生产已发布。

## 当前范围

- 按新系统首次部署验收两个同源入口，不要求建设或恢复四站环境。
- 验证实际 TLS/CA、反代、长停留/休眠，以及目标网络、节点和依赖下尚未覆盖的安全与故障场景。
- 验证成套部署、监控/补偿及首次部署失败恢复。旧 Cookie/Redis 盘点与迁移仅在目标确有旧系统时适用。
- 状态保持 `draft`；本轮用户确认调整 Spec，不执行部署、故障注入或账号状态变更。目标环境与执行条件确认后按任务进入 `approved`。

## 文档与证据

- 来源：[已归档实现](../../archive/2026/20261031-security-same-origin-login/README.md)。
- 当前实现：[同源登录基线](../../../current/security/same-origin-login/README.md)。
- 首次部署规划与已验收范围：[IAM 最终确认](../../archive/2026/20260912-iam-identity-access-management/USER-ACCEPTANCE.md)、[验收收口](../../archive/2026/20260912-iam-identity-access-management/ACCEPTANCE-CLOSEOUT.md)。
- 前端已完成：[登录外观](../../../../../ingot-admin/specs/changes/archive/2026/20261030-ingot-login-appearance/README.md)、[登录错误提示](../../../../../ingot-admin/specs/changes/archive/2026/20261106-ingot-login-error-feedback/README.md)。
- [剩余需求](./REQUIREMENTS.md)、[执行设计](./DESIGN.md)、[验收任务](./TASKS.md)。

## 调整记录

2026-11-11 用户确认按现行实现与规划调整：明确双域默认方案、首次部署范围和已有验收归属；原 R01–R05、E00–E05 编号保留。旧系统切换步骤改为条件适用，目标环境缺失证据继续待验。本次只更新 Spec 与索引，不改运行代码、配置或 current，不归档本 change。
