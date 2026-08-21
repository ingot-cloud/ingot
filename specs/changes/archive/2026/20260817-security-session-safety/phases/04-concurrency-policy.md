# Phase 04 · 并发会话策略

> 状态：implemented（已验收）  
> 依赖：Phase 03 退出条件满足（策略 CRUD 可与管理面并行开发，但 **登录执行** 依赖 Phase 01 撤销彻底性）

## 目标

用可 Nacos 降级、可中心化的并发策略替换 client `tokenSettings` 上的 `STANDARD/UNIQUE` 二值；登录时执行最多 N 个会话及超限行为。

## 实现要点

- `databases/migrations/016_session_concurrency_policy.sql` + `rollback_016_session_concurrency_policy.sql`（`014` / `015` 已被 Phase 03 权限种子与 Phase 02 遗留修正占用）；`USE ingot_security`；GLOBAL 种子 `max_sessions=0`。
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

## 实施记录（As-Built 与计划的差异）

- 权限码种子拆成 `017_session_policy_permission_seed.sql`：`016` 目标库是 `ingot_security`，权限表在 `ingot_core`，一个脚本无法覆盖两库。
- 失效广播订阅从 `SessionConcurrencyConfiguration` 拆到 `SessionConcurrencyInvalidationAutoConfiguration`：`SessionConcurrencyConfiguration` 经 `@EnableInAuthorizationServer` 导入，属用户配置，早于自动配置评估，其中的 `@ConditionalOnBean(InvalidationBus.class)` 恒为假。
- `scope` 匹配为「命中即止」，不做字段级合并（`CLIENT` > `USER_TYPE` > `GLOBAL`），已写入 [PLATFORM-API §8.1](../PLATFORM-API.md#81-生效模型前端必须向用户讲清的三点)。
- GLOBAL 策略禁止删除：删了之后 remote 模式只剩 LKG / 地板兜底，语义比「把 `maxSessions` 改成 0」更难解释。
- 三环境 Nacos：DEV `session.mode=local`（保留无中心路径的日常覆盖），TEST / PROD `remote`；PROD 地板保持 `max-sessions: 0`，避免中心不可用时因地板过严误伤登录。

## 退出条件

- [x] A9：local 模式无安全中心可执行地板并发策略。
  - `LocalSessionConcurrencyPolicyResolver` 每次 resolve 读 `@ConfigurationProperties`，Nacos 刷新后无需重启；单测 `LocalSessionConcurrencyPolicySourceTest`
- [x] A10：remote 停中心走 LKG/地板；改地板不重启，新登录行为变化。
  - 单测已覆盖降级判定与 fail-closed（`RemoteSessionConcurrencyPolicyResolverTest`）；统一验收于 2026-08-20 闭合
- [x] overflow 三种行为有测试。
  - `SessionConcurrencyEnforcerTest`：`REJECT` / `KICK_OLDEST` / `KICK_ALL`
- [x] ADMIN 禁止并发生效。
  - `adminForbidConcurrent` 对 `UserTypeEnum.ADMIN` 强制 N=1，优先级高于策略显式值；同上单测
- [x] current 反映 As-Built；已知限制写入 SPEC：5.2 设备维度、交互式踢人、**非自助路径不清 BFF 会话键（D19，含收敛路径说明以免后人误当 bug）**、网关仍暴露 `/auth/client/**`（D21）。

## 回滚

`mode=local` 回退 Nacos；DB 执行 `rollback_017`（权限码）与 `rollback_016`（策略表），顺序不限但**先切 Auth 配置**：remote 模式下删表会让远端拉取失败，若同时关掉地板即 fail-closed 拒绝新登录。登录路径开关 `ingot.security.session.concurrency.enabled=false` 可紧急退回「只认 client UNIQUE/STANDARD」。
