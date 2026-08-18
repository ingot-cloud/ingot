# Tasks

本 change 规模超出单一发布单元，实施按 [phases/](./phases/) 四阶段推进。本文件是总顺序与门禁；阶段内细节以各 `phases/0N-*.md` 为准。

## Phase 0 — 规格审阅（当前）

- [ ] T0-1：负责人审阅 README / REQUIREMENTS / DESIGN / TASKS / phases，闭合 D1–D18
  - 依赖：无
  - 验收：DESIGN「待审阅决策记录」与 README 决策表已填决议；change 状态转 `approved`

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

- [ ] T1-1：claim 常量 `SID`；`RedisKeyConstants` 收口会话 key
- [ ] T1-2：`OnlineToken` 增 sid/lastAccessAt；`RedisOnlineTokenService` 新 schema + 双读 + Set TTL 修正
- [ ] T1-3：签发链写 sid（Customizer + Custom grant 预生成 authorizationId）
- [ ] T1-4：`SessionRevocationService`；`remove(authorization)` 级联 `removeBySid`；UNIQUE 踢旧走完整撤销
- [ ] T1-5：RS：去掉 JWT-only fallback；Filter 按 sid 校验；D1/D2 行为
- [ ] T1-6：网关按 sid 读 userType（jti 回退限 D2）
- [ ] T1-7：BFF `BffSession.sid`；logout 按 sid
- [ ] T1-8：清理任务改为注册表 / SCAN，删除 `keys()`
- [ ] T1-9：Phase 01 单测 + 登录/refresh/下线集成验收

**阶段门禁**：A1–A6、A12 可在无安全中心的情况下通过。

---

## Phase 02 — 执行面与事件

详见 [phases/02-execution-events.md](./phases/02-execution-events.md)。

- [ ] T2-1：Auth `InnerSessionAPI` + 查询/撤销实现
- [ ] T2-2：`RemoteSessionService`（Feign 指向 Auth）
- [ ] T2-3：`SESSION_REVOKED` / `SESSION_CONCURRENT_KICKOUT` + classifier DURABLE；自助登出报 `LOGOUT`
- [ ] T2-4：账号域密码/锁定/禁用联动 `revokeByUser`
- [ ] T2-5：Phase 02 测试（含联动失败可观测）

**阶段门禁**：A7、A11；Inner 可被后续中心调用。

---

## Phase 03 — 中心管理面

详见 [phases/03-center-admin.md](./phases/03-center-admin.md)。

- [ ] T3-1：Platform `/platform/security/session/**` + PMS 拼装
- [ ] T3-2：`PLATFORM-API.md` 与 Controller/Swagger 对齐
- [ ] T3-3：权限码种子（若走字典/权限表）
- [ ] T3-4：Auth `/token/tokens|/jti|/user` deprecated 并转调新实现；去掉 Auth→PMS 管理查询依赖
- [ ] T3-5：Phase 03 E2E：中心列表 + 按 sid/用户下线

**阶段门禁**：A8、A13；不部署中心时 A9 仍成立。

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

- [ ] V1：A1–A14 勾选，与 DESIGN 决议一致
- [ ] V2：L2 锁定、L3 事件、L4 登录失败封禁回归
- [ ] V3：无 `RedisTemplate.keys` 用于 `online:user:` 清理
- [ ] V4：current 已按 As-Built 编写，非复制 change 原文

## 完成检查

- [ ] 实现与 DESIGN 一致（差异写入 README 完成记录）
- [ ] REQUIREMENTS 验收标准全部满足
- [ ] Current 已更新：`specs/current/security/session-safety/`
- [ ] Change 已记录完成信息并归档至 `specs/changes/archive/2026/`
- [ ] [security-center-roadmap.md](../../../../docs/requirements/themes/security-center-roadmap.md) L5 = `done`
- [ ] [ROADMAP.md](../../../../docs/requirements/ROADMAP.md) `R-2026-028` = `done`
