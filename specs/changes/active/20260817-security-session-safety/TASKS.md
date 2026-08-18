# Tasks

本 change 规模超出单一发布单元，实施按 [phases/](./phases/) 四阶段推进。本文件是总顺序与门禁；阶段内细节以各 `phases/0N-*.md` 为准。

## Phase 0 — 规格审阅（当前）

- [ ] T0-1：负责人审阅 README / REQUIREMENTS / DESIGN / TASKS / phases；D1–D21 已按决议闭合，确认 change 转 `approved`
  - 依赖：无
  - 验收：DESIGN「待审阅决策记录」无待确认项；change 状态转 `approved`

- [x] T0-2：更新 [security-center-roadmap.md](../../../../docs/requirements/themes/security-center-roadmap.md) L5 为 `in-spec` 并链接本目录
  - 依赖：本草稿提交
  - 验收：roadmap 表 L5 行 Change 指向 `specs/changes/active/20260817-security-session-safety/`

- [x] T0-3：在 [ROADMAP.md](../../../../docs/requirements/ROADMAP.md) Next 增加 `R-2026-028` 会话安全，状态 `in-spec`
  - 依赖：T0-2
  - 验收：Next 表存在 R-2026-028 且链接本 change

**暂停点**：T0-1 审阅通过前不进入 Phase 01 编码。

---

## Phase 01 — 会话模型与撤销收口

详见 [phases/01-session-model.md](./phases/01-session-model.md)。

- [ ] T1-0：Auth 拆 `ingot-auth-api` + `ingot-auth-provider`（D20）
  - `settings.gradle` 按 pms/member 两行 include；`config/ingot.gradle` 增加 `ingot.auth_api`
  - api：`EnableAPIConfiguration` + `AutoConfiguration.imports`；`RemoteAuthTokenService` 承接 BFF 现有 `/oauth2/pre_authorize` / `/oauth2/authorize` / `/oauth2/token`；`DELETE /token` 仅作 Phase 01 施工桥，Phase 02 删除
  - BFF 删除 `com.ingot.cloud.bff.client.AuthClient`，改依赖 `ingot-auth-api`
  - Dockerfile 随 provider 平移；`.gitlab-ci.yml` assemble 路径改为 `:ingot-service:ingot-auth:ingot-auth-provider`
  - 验收：BFF 登录/登出仍走 Feign；`AUTH_SERVICE` 的 `@FeignClient` 定义只在 `ingot-auth-api`；镜像仍可 assemble
- [ ] T1-1：claim 常量 `SID`；`RedisKeyConstants` 收口会话 key
- [ ] T1-2：`OnlineToken` 增 sid/lastAccessAt；`RedisOnlineTokenService` 新 schema + Set TTL 修正（无双读）
- [ ] T1-3：签发链写 sid（Customizer + Custom grant 预生成 authorizationId）
- [ ] T1-4：`SessionRevocationService`；`remove(authorization)` 级联 `removeBySid`；UNIQUE 踢旧走完整撤销。**不含**任何 BFF 键操作（D19）
- [ ] T1-5：RS：去掉 `convertFromJwtOnly` 放行；无 sid 直接拒绝；Filter 按 sid 校验；D1 宽限
- [ ] T1-6：网关按 sid 读 userType
- [ ] T1-7：BFF `BffSession.sid`（选租户后写入）；logout 暂走施工桥 `RemoteAuthTokenService`（Auth `DELETE /token` 内改 `revokeBySid`）。**不**建反向索引
- [ ] T1-8：清理任务改为注册表 / SCAN，删除 `keys()`
- [ ] T1-9：发布运维脚本（SCAN 清空 `in:bff_session:*` / `token:jti:*` / `oauth2:auth:*` / `oauth2:token:*`）+ 发布/回滚预案（含强制下线告知）。因 D19 不做运行时清理，本脚本是清除存量 BFF 会话键的唯一时机
- [ ] T1-10：Phase 01 单测 + 登录/refresh/下线集成验收，含 D19 行为断言（管理员下线后 BFF 键仍在但请求 401）

**阶段门禁**：A1–A6、A12、A15、A17 可在无安全中心的情况下通过；T1-0 完成后 Phase 02 才可加会话 Feign。A16 / A13 在 Phase 02 Inner 落地并删除 `TokenEndpoint` 后验收。

---

## Phase 02 — 执行面与事件

详见 [phases/02-execution-events.md](./phases/02-execution-events.md)。

- [ ] T2-1：Auth `InnerSessionAPI` + 查询/撤销实现
- [ ] T2-2：`ingot-auth-api` 增加 `RemoteAuthSessionService`；从 `RemoteAuthTokenService` 去掉 `/token` revoke
  - 依赖：T1-0
- [ ] T2-3：BFF logout 切到 `RemoteAuthSessionService.revokeBySid`；`sid` 为空只清自己的键，不回落旧 API
- [ ] T2-4：删除 `TokenEndpoint`、`BizUserTokenService` 及 PMS 管理查询依赖（D8 / D17）；无 deprecated 包装
- [ ] T2-5：`SESSION_REVOKED` / `SESSION_CONCURRENT_KICKOUT` + classifier DURABLE；自助登出报 `LOGOUT`
- [ ] T2-6：账号域密码/锁定/禁用联动 `revokeByUser`，依赖 `ingot-auth-api`
- [ ] T2-7：网关路由收敛：三套 `nacos/*/in-service-gateway.yml` 的 `in-service-auth` predicate 改为仅 `/auth/client/**`；顺带删除失效 `verifyUrls` 中的 `/auth/oauth2/*`
- [ ] T2-8：Phase 02 测试（含联动失败可观测、A13 / A16 / A19）

**阶段门禁**：A7、A11、A13、A16、A18、A19；Inner 可被后续中心调用。

---

## Phase 03 — 中心管理面

详见 [phases/03-center-admin.md](./phases/03-center-admin.md)。

- [ ] T3-1：Platform `/platform/security/session/**` + PMS 拼装
- [ ] T3-2：`PLATFORM-API.md` 与 Controller/Swagger 对齐
- [ ] T3-3：权限码种子（若走字典/权限表）
- [ ] T3-4：Phase 03 E2E：中心列表 + 按 sid/用户下线

**阶段门禁**：A8；不部署中心时 A9 仍成立。

---

## Phase 04 — 并发会话策略

详见 [phases/04-concurrency-policy.md](./phases/04-concurrency-policy.md)。

- [ ] T4-1：migration `014_session_concurrency_policy.sql` + rollback + 基线 SQL
- [ ] T4-2：中心 Entity/Mapper/AdminService/Platform CRUD/Inner
- [ ] T4-3：Auth `LayeredCacheBuilder` 接入 + Nacos 地板 + Actuator 来源
- [ ] T4-4：登录路径执行 maxSessions / overflow / adminForbidConcurrent
- [ ] T4-5：client `UNIQUE/STANDARD` 映射与优先级（D15）
- [ ] T4-6：动态刷新与 remote 故障注入（A10）
- [ ] T4-7：新建 `specs/current/security/session-safety/`；roadmap L5 → `done`；归档本 change

**阶段门禁**：A9、A10、A14；REQUIREMENTS 验收标准全部勾选。

---

## 验证任务

- [ ] V1：A1–A19 勾选，与 DESIGN 决议一致
- [ ] V2：L2 锁定、L3 事件、L4 登录失败封禁回归
- [ ] V3：无 `RedisTemplate.keys` 用于 `online:user:` 清理与发布清空脚本
- [ ] V4：current 已按 As-Built 编写，非复制 change 原文
- [ ] V5：全仓检索确认无 sid 回退分支与 `convertFromJwtOnly` 放行路径残留
- [ ] V6：依赖方向自检（D19）——全仓检索 `bff_session` / `bffSessionKey`，确认写入方只有 `ingot-bff`、读取方只有 `ingot-bff` 与 `ingot-gateway`，`ingot-auth-provider` 及 `ingot-security-provider` 无任何引用
- [ ] V7：契约唯一性自检（D20）——全仓检索 `AUTH_SERVICE`，确认 `@FeignClient` 定义只出现在 `ingot-auth-api`
- [ ] V8：全仓检索 `TokenEndpoint`、`/token/tokens`、`/token/jti`、`RequestMapping("/token")`，确认已删除且无 deprecated 残留（D17）

## 完成检查

- [ ] 实现与 DESIGN 一致（差异写入 README 完成记录）
- [ ] REQUIREMENTS 验收标准全部满足
- [ ] Current 已更新：`specs/current/security/session-safety/`
- [ ] Change 已记录完成信息并归档至 `specs/changes/archive/2026/`
- [ ] [security-center-roadmap.md](../../../../docs/requirements/themes/security-center-roadmap.md) L5 = `done`
- [ ] [ROADMAP.md](../../../../docs/requirements/ROADMAP.md) `R-2026-028` = `done`
