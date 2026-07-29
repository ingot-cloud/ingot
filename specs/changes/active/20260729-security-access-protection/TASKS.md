# Tasks

## Phase 0 — 规格审阅（当前）

- [ ] T0-1：负责人审阅 README / REQUIREMENTS / DESIGN / TASKS / [PLATFORM-API.md](./PLATFORM-API.md)，闭合 D1–D11
  - 依赖：无
  - 验收：DESIGN「待审阅决策记录」表已填决议；change 状态转 `approved`

- [ ] T0-2：更新 [security-center-roadmap.md](../../../../docs/requirements/themes/security-center-roadmap.md) L4 为 `in-spec`（本步骤已完成则跳过）
  - 依赖：T0-1 草稿提交
  - 验收：roadmap 表 L4 行 Change 链接指向本目录

- [ ] T0-3：更新 [ROADMAP.md](../../../../docs/requirements/ROADMAP.md) `R-2026-026` 状态 `in-spec`
  - 依赖：T0-2
  - 验收：Next 表存在 R-2026-026 且链接正确

**暂停点**：T0-1 审阅通过前不进入 Phase 1 编码。

---

## Phase 1 — Gateway Policy Resilience

- [ ] T1-1：新增 `PolicyRemoteUnavailableException`、`PolicyLastKnownGoodStore`（Redis）、`LocalPolicyFloorSupplier`
  - 依赖：T0-1 approved
  - 验收：单元测试覆盖 save/load LKG；floor 返回非空 baseline

- [ ] T1-2：实现 `ResilientSnapshotFetcher` + `PolicySourceHolder`；改造 `RemoteSnapshotFetcher` 失败抛异常
  - 依赖：T1-1
  - 验收：`ResilientSnapshotFetcherTest`：成功刷新 LKG / 失败走 LKG / 无 LKG 走地板 / 合法空不兜底

- [ ] T1-3：装配 Resilient 链至各 `Remote*Service`；Actuator 端点暴露 `policySource`
  - 依赖：T1-2
  - 验收：remote 失败时 Actuator 显示 LKG 或 LOCAL_FLOOR；不出现 null 快照 silent 放行

- [ ] T1-4：Nacos 地板示例 `in-security-policy.yml` + 动态刷新说明写入模块 JavaDoc
  - 依赖：T1-3
  - 验收：改 floor yaml 不重启，Actuator 来源仍为 LOCAL_FLOOR 且规则变化

---

## Phase 2 — Execution Closure

- [ ] T2-1：编写 migration `011_security_access_protection_seed.sql` + rollback + 基线 SQL 同步
  - 依赖：T0-1 approved
  - 验收：`login-auth` / `api-business` 分组及限流种子可执行可回滚

- [ ] T2-2：更新 Nacos DEV/TEST/PROD `in-service-gateway.yml`：SDK 四域 enabled + remote + event.remote
  - 依赖：T2-1
  - 验收：网关启动 `SecurityPolicyBootstrapLogger` 打印规则条数 > 0

- [ ] T2-3：E2E 阶段二 remote：Platform 快照 + 失效广播 + 登录路径 429
  - 依赖：T2-2、T1-3
  - 验收：`test-case/security-policy-e2e.md` 阶段二核心用例通过

- [ ] T2-4：移除 `/pms/**`、`/member/**`、`/security/**` 路由 `RequestRateLimiter` filter
  - 依赖：T2-3
  - 验收：Nacos 无 RequestRateLimiter；E2E 限流行为与迁移前等价或更严（D9）

---

## Phase 3 — 3.1 四维度登录失败保护 + 安全中心 remote

- [ ] T3-0：`login_failure_protection_policy` DDL + 四维种子（合入 migration 011）+ 基线 SQL
  - 依赖：T0-1 approved
  - 验收：表可执行可回滚；种子与 PLATFORM-API §3.2 一致

- [ ] T3-0b：`LoginFailureProtectionAPI`（Platform）+ `InnerLoginFailurePolicyAPI` + `RemoteLoginFailurePolicyService`（api 模块）
  - 依赖：T3-0
  - 验收：Swagger 可浏览；字段与 [PLATFORM-API.md](./PLATFORM-API.md) 一致；PUT 触发 `LOGIN_FAILURE_PROTECTION` 失效

- [ ] T3-1：脚手架 `ingot-security-access-core` + `ingot-security-access-adapter` + Gradle；Auth 引入 adapter
  - 依赖：T3-0b
  - 验收：模块编译；`ingot.security.access.mode` 开关

- [ ] T3-1b：`LocalLoginFailurePolicyLoader` + `RemoteLoginFailurePolicyLoader` + `ResilientLoginFailurePolicyLoader` + LKG + Floor + Actuator
  - 依赖：T3-1
  - 验收：remote 成功/LKG/地板单测；与凭证 Resilient 语义一致（D5）

- [ ] T3-2：扩展 `AuthFailureDTO`（clientId/deviceId）+ `DefaultAuthenticationFailureHandler`
  - 依赖：T3-1
  - 验收：登录失败事件 payload 含新字段（有 Header/参数时）

- [ ] T3-3：实现 `LoginFailureProtectionService` + Redis 滑动窗口 + `TempBlockWriter`（经 loader 读策略）
  - 依赖：T3-1b、T3-2
  - 验收：单测达阈值写 `in:gw:bl:tmp:*`；策略来自 remote Platform 配置

- [ ] T3-4：Auth listener 接线 + `LoginFailurePolicyCacheCoordinator`（失效订阅）
  - 依赖：T3-3
  - 验收：S3–S6、S12；Platform 改阈值 ≤10s 生效

- [ ] T3-5：扩展 `SecurityEventType` + 异步上报 ACCESS 事件
  - 依赖：T3-4、L3
  - 验收：封禁后 `security_event` 有对应 `LOGIN_FAIL_*_EXCEED`

- [ ] T3-6：实现 `attemptWindowMinutes` 滑动窗口（`RecordLoginUseCaseService`）
  - 依赖：T3-1
  - 验收：S8 窗口外失败计数归零

- [ ] T3-7：`ClientIdentity` + `RateLimitDimension.CLIENT` + `IpKeyType.CLIENT`（P0）
  - 依赖：T2-3
  - 验收：Platform 可配 Client 维度限流/名单；与 PLATFORM-API §4 一致

- [ ] T3-8：Nacos `in-service-auth.yml`：`ingot.security.access.mode=remote`（DEV/TEST/PROD）
  - 依赖：T3-4
  - 验收：Auth 启动 Feign 拉策略；local 模式集成测试仍可通过

---

## Phase 4 — 验收与基线

- [ ] T4-1：Resilience 故障注入：停 security → LKG → 地板 → 恢复 remote
  - 依赖：Phase 1–3
  - 验收：S9 通过；无 fail-open

- [ ] T4-2：全量 E2E + L2/L3 回归 + 前端 API 字段抽查（PLATFORM-API vs Swagger）
  - 依赖：T4-1
  - 验收：REQUIREMENTS 验收标准全部勾选

- [ ] T4-3：新建 `specs/current/security/access-protection/`（README + SPEC）
  - 依赖：T4-2
  - 验收：current 反映 As-Built；已知限制入 SPEC

- [ ] T4-4：更新 roadmap L4 → `done`；归档本 change 至 `specs/changes/archive/2026/`
  - 依赖：T4-3
  - 验收：ROADMAP R-2026-026 → done

---

## 验证任务

- [ ] V1：`ResilientSnapshotFetcher` / `ResilientLoginFailurePolicyLoader` / 滑动窗口单元测试
- [ ] V5：[PLATFORM-API.md](./PLATFORM-API.md) 与实现 Controller 字段逐项对照
- [ ] V2：migration 011 在 `ingot_security` 执行 + rollback
- [ ] V3：Nacos 热刷新 S11（login-failure 阈值 + SDK 开关）
- [ ] V4：登录失败 → 403 → ACCESS 事件 DB 直查

---

## 完成检查

- [ ] 实现与 DESIGN 一致（含 D1–D11 决议）
- [ ] REQUIREMENTS 验收标准全部满足
- [ ] Current 已更新（`specs/current/security/access-protection/`）
- [ ] [security-center-roadmap.md](../../../../docs/requirements/themes/security-center-roadmap.md) L4 状态与 Change 链接已更新
- [ ] [ROADMAP.md](../../../../docs/requirements/ROADMAP.md) R-2026-026 已更新
- [ ] Change README 完成记录已填写并归档
