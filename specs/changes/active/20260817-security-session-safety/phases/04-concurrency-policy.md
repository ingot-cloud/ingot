# Phase 04 · 并发会话策略

> 状态：pending  
> 依赖：Phase 03 退出条件满足（策略 CRUD 可与管理面并行开发，但 **登录执行** 依赖 Phase 01 撤销彻底性）

## 目标

用可 Nacos 降级、可中心化的并发策略替换 client `tokenSettings` 上的 `STANDARD/UNIQUE` 二值；登录时执行最多 N 个会话及超限行为。

## 实现要点

- `databases/migrations/014_session_concurrency_policy.sql` + `rollback_014_session_concurrency_policy.sql`；`USE ingot_security`；GLOBAL 种子 `max_sessions=0`。
- Provider：Entity / Mapper / AdminService / Platform CRUD / Inner `GET /inner/security/session/concurrency-policies`。
- `SecurityPolicyDomain.SESSION_CONCURRENCY` 失效广播。
- Auth：`LayeredCacheBuilder` 装配（参考 `GatewayRuleClientAutoConfiguration` 的 `resilientSingleKey`）；配置键 `ingot.security.session.*` 留在 Auth。
- `mode=local`：只读 `@ConfigurationProperties` 地板，rebinder 热刷新。
- `mode=remote`：Feign → LKG Redis → Nacos 地板；地板关闭且无 LKG → **拒绝新登录**（fail-closed，不静默无限会话）。
- 登录成功写会话前：按 dimension=`USER_CLIENT` 数 sid 集合；应用 overflow。
- `admin_forbid_concurrent`：ADMIN 用户强制 N=1。
- D15：client `UNIQUE` 仅在策略为无限时作为缺省 N=1；策略表显式值优先。
- UNIQUE 踢旧事件用 `SESSION_CONCURRENT_KICKOUT`。
- Nacos 三环境 `in-service-auth.yml`：DEV/TEST/PROD 增加 session 段；生产 `mode=remote`。
- 动态刷新验证写入模块 JavaDoc / 本 Phase 验收记录。
- 验收后新建 `specs/current/security/session-safety/{README,SPEC}.md`；更新 current 索引；roadmap L5 → done；本 change 归档。

## 退出条件

- [ ] A9：local 模式无安全中心可执行地板并发策略。
- [ ] A10：remote 停中心走 LKG/地板；改地板不重启，新登录行为变化。
- [ ] overflow 三种行为有测试。
- [ ] ADMIN 禁止并发生效。
- [ ] current 反映 As-Built；已知限制写入 SPEC：5.2 设备维度、交互式踢人、**非自助路径不清 BFF 会话键（D19，含收敛路径说明以免后人误当 bug）**、网关仍暴露 `/auth/client/**`（D21）。

## 回滚

`mode=local` 回退 Nacos；DB 执行 rollback_014。登录路径开关 `ingot.security.session.concurrency.enabled=false` 可紧急退回「只认 client UNIQUE/STANDARD」。
