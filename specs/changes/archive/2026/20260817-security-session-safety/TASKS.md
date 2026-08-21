# Tasks

本 change 规模超出单一发布单元，实施按 [phases/](./phases/) 四阶段推进。本文件是总顺序与门禁；阶段内细节以各 `phases/0N-*.md` 为准。

## Phase 0 — 规格审阅（当前）

- [x] T0-1：负责人审阅 README / REQUIREMENTS / DESIGN / TASKS / phases；D1–D21 已按决议闭合，确认 change 转 `approved`（2026-08-18 审阅通过）
  - 依赖：无
  - 验收：DESIGN「待审阅决策记录」无待确认项；change 状态转 `approved`

- [x] T0-2：更新 [security-center-roadmap.md](../../../../../docs/requirements/themes/security-center-roadmap.md) L5 为 `in-spec` 并链接本目录
  - 依赖：本草稿提交
  - 验收：roadmap 表 L5 行 Change 指向 `specs/changes/active/20260817-security-session-safety/`

- [x] T0-3：在 [ROADMAP.md](../../../../../docs/requirements/ROADMAP.md) Next 增加 `R-2026-028` 会话安全，状态 `in-spec`
  - 依赖：T0-2
  - 验收：Next 表存在 R-2026-028 且链接本 change

**暂停点**：T0-1 审阅通过前不进入 Phase 01 编码。

---

## Phase 01 — 会话模型与撤销收口

详见 [phases/01-session-model.md](./phases/01-session-model.md)。

- [x] T1-0：Auth 拆 `ingot-auth-api` + `ingot-auth-provider`（D20）
  - `settings.gradle` 按 pms/member 两行 include；`config/ingot.gradle` 增加 `ingot.auth_api`
  - api：`EnableAPIConfiguration` + `AutoConfiguration.imports`；`RemoteAuthTokenService` 承接 BFF 现有 `/oauth2/pre_authorize` / `/oauth2/authorize` / `/oauth2/token`；`DELETE /token` 仅作 Phase 01 施工桥，Phase 02 删除
  - BFF 删除 `com.ingot.cloud.bff.client.AuthClient`，改依赖 `ingot-auth-api`
  - Dockerfile 随 provider 平移；`.gitlab-ci.yml` assemble 路径改为 `:ingot-service:ingot-auth:ingot-auth-provider`
  - 验收：BFF 登录/登出仍走 Feign；`AUTH_SERVICE` 的 `@FeignClient` 定义只在 `ingot-auth-api`；镜像仍可 assemble
- [x] T1-1：claim 常量 `SID`；`RedisKeyConstants` 收口会话 key
- [x] T1-2：`OnlineToken` 增 sid/lastAccessAt；`RedisOnlineTokenService` 新 schema + Set TTL 修正（无双读）
- [x] T1-2b：会话 Redis 存储收敛（Phase 01 schema As-Built 修正）
  - `extendExpire`：`TTL=-1` 补过期，不再当成永久
  - 读路径 `MGET` 主数据后 `SREM` 墓碑；撤销后回写 `online:user` score
  - 小时任务 `ZREM` 过期 userId 时 `DEL` 对应 `token:user:set`
  - 运行时不再写 `token:jti:*` / UNIQUE `token:user:{t}:{c}:{u}`；`InTokenAuthFilter` 只放 `SessionContextHolder`
  - `session.ip-set-max-members` 默认 1000；管理面按用户分页改为批量 `listUserSessions`
- [x] T1-3：签发链写 sid（Customizer + Custom grant 预生成 authorizationId）
- [x] T1-4：`SessionRevocationService`；`remove(authorization)` 级联 `removeBySid`；UNIQUE 踢旧走完整撤销。**不含**任何 BFF 键操作（D19）
- [x] T1-5：RS：去掉 `convertFromJwtOnly` 放行；无 sid 直接拒绝；Filter 按 sid 校验；D1 宽限（`SessionStoreAvailability` + `ingot.security.session.store.*` 指标）
- [x] T1-6：网关按 sid 读 userType
- [x] T1-7：BFF `BffSession.sid`（选租户后写入）；logout 暂走施工桥 `RemoteAuthTokenService`（Auth `DELETE /token` 内改 `revokeBySid`）。**不**建反向索引
- [x] T1-8：清理任务改为注册表 / SCAN，删除 `keys()`
- [x] T1-9：发布运维脚本 [`bin/session_keys_purge.sh`](../../../../../bin/session_keys_purge.sh)（SCAN + UNLINK，默认 dry-run，`--apply` 执行）+ 发布/回滚预案见 [phases/01](./phases/01-session-model.md#发布预案)
- [ ] T1-10：Phase 01 单测 + 登录/refresh/下线集成验收，含 D19 行为断言（管理员下线后 BFF 键仍在但请求 401）
  - 单测已完成：framework 新增 24 例（`SessionStoreAvailabilityTest`、`JwtInUserConverterTest`、`RedisOnlineTokenServiceTest`、`DefaultSessionRevocationServiceTest`、`SessionRegistrarTest`），gateway / bff / auth 既有单测全绿
  - 待联调环境完成集成验收后勾选

**阶段门禁**：A1–A6、A12、A15、A17 可在无安全中心的情况下通过；T1-0 完成后 Phase 02 才可加会话 Feign。A16 / A13 在 Phase 02 Inner 落地并删除 `TokenEndpoint` 后验收。

---

## Phase 02 — 执行面与事件

详见 [phases/02-execution-events.md](./phases/02-execution-events.md)。

- [x] T2-1：Auth `InnerSessionAPI` + 查询/撤销实现
- [x] T2-2：`ingot-auth-api` 增加 `RemoteAuthSessionService`；从 `RemoteAuthTokenService` 去掉 `/token` revoke
  - 依赖：T1-0
- [x] T2-3：BFF logout 切到 `RemoteAuthSessionService.revokeBySid`；`sid` 为空只清自己的键，不回落旧 API
- [x] T2-4：删除 `TokenEndpoint`、`BizUserTokenService` 及 PMS 管理查询依赖（D8 / D17）；无 deprecated 包装
- [x] T2-5：`SESSION_REVOKED` / `SESSION_CONCURRENT_KICKOUT` + classifier DURABLE；自助登出报 `LOGOUT`
- [x] T2-6：账号域密码/锁定/禁用联动 `revokeByUser`，依赖 `ingot-auth-api`
- [x] T2-7：网关路由收敛：三套 `nacos/*/in-service-gateway.yml` 的 `in-service-auth` predicate 改为仅 `/auth/client/**`；顺带删除失效 `verifyUrls` 中的 `/auth/oauth2/*`
  - 顺带清理：三套 `in-security-policy.yml` 与 `PolicySnapshotFloorAssembler` 的登录地板改 `/bff/auth/login/**`（旧 `/auth/token/**` 已无保护对象）
- [x] T2-8：Phase 02 测试（含联动失败可观测、A13 / A16 / A19）
  - 单测已完成并全仓 `./gradlew test` 通过；需真实 Redis + 服务实例的集成验收待联调环境
  - 遗留已闭合：`011` 种子里的 `/auth/token/**` 由 [`015_login_auth_group_pattern_fix.sql`](../../../../../databases/migrations/015_login_auth_group_pattern_fix.sql) 修正（仅改仍含旧路径的行）；基线 `databases/ingot_security.sql` 早已为 `/bff/auth/login`

**阶段门禁**：A7、A11、A13、A16、A18、A19；Inner 可被后续中心调用。

---

## Phase 03 — 中心管理面

详见 [phases/03-center-admin.md](./phases/03-center-admin.md)。

- [x] T3-1：Platform `/platform/security/sessions/**` + PMS 拼装
  - 路径按 `sessions` 复数收敛（DESIGN 表格里的 `/session` + `/sessions` 会拼成 `session/sessions`，属笔误，已与负责人确认）
  - `SessionAdminService` + `PlatformSessionAPI`；VO/DTO 在 `ingot-security-api`；`security-provider` 新增 `ingot.auth_api` / `ingot.pms_api` 依赖
  - 顺带修正 `RemotePmsUserDetailsService.getAllUserInfo` 的契约错误（声明 `@GetMapping` 但服务端是 `POST /inner/user/list`，调用必然 405；Phase 02 摘掉 Auth 的 enrichment 后本方法无调用方，本 Phase 首次真正使用）
- [x] T3-2：[`PLATFORM-API.md`](./PLATFORM-API.md) 与 Controller/Swagger 对齐
  - 含旧 `/auth/token/**` → 新接口的字段级迁移映射与前端改造清单（列表形态由「用户行 + tokens[]」改为扁平会话，下线入参 `jti` → `sid`）
- [x] T3-3：权限码种子 [`014_session_admin_permission_seed.sql`](../../../../../databases/migrations/014_session_admin_permission_seed.sql) + rollback + `databases/ingot_core.sql` 基线
  - `platform:security:session:query` / `:revoke` 作为操作级 API 权限挂在既有「在线用户」菜单权限下
- [ ] T3-4：Phase 03 E2E：中心列表 + 按 sid/用户下线
  - 单测已完成：`SessionAdminServiceImplTest` 11 例（范围校验、租户上下文回落、PMS 降级、C 端用户跳过 PMS、跨 Client 内存分页、撤销原因/操作者固定）
  - 待联调环境完成 E2E 后勾选

**阶段门禁**：A8；不部署中心时 A9 仍成立。

---

## Phase 04 — 并发会话策略

详见 [phases/04-concurrency-policy.md](./phases/04-concurrency-policy.md)。

- [x] T4-1：migration [`016_session_concurrency_policy.sql`](../../../../../databases/migrations/016_session_concurrency_policy.sql) + rollback + 基线 SQL（`014` / `015` 已被 Phase 03 与 Phase 02 遗留修正占用）
  - GLOBAL 种子 `max_sessions=0`，升级后行为与升级前一致（收紧靠新增窄 scope 记录）
  - 权限码另起 [`017_session_policy_permission_seed.sql`](../../../../../databases/migrations/017_session_policy_permission_seed.sql) + rollback（目标库不同，`016` 是 `ingot_security`、`017` 是 `ingot_core`，不能合并成一个脚本）
  - 基线同步：`databases/ingot_security.sql` 增表与种子、`databases/ingot_core.sql` 增两条权限
- [x] T4-2：中心 Entity/Mapper/AdminService/Platform CRUD/Inner
  - `SessionConcurrencyPolicy` / `SessionConcurrencyPolicyMapper` / `SessionConcurrencyPolicyAdminService(+Impl)`
  - `SessionConcurrencyPolicyAPI`（`/platform/security/session/concurrency-policies`，权限码 `platform:security:session:policy:query|update`）
  - `InnerSessionConcurrencyPolicyAPI`（`GET /inner/security/session/concurrency-policies`）+ `SecurityPolicyDomain.SESSION_CONCURRENCY` 失效广播
  - 写入统一归一：不适用的 `clientId` / `userType` 落空串（唯一索引不比较 NULL）；GLOBAL 策略禁止删除
- [x] T4-3：Auth `LayeredCacheBuilder` 接入 + Nacos 地板 + Actuator 来源
  - `SessionConcurrencyConfiguration` 按 `enabled` / `mode` 三态互斥装配（client-only / local / remote）
  - remote：`FeignSessionConcurrencyPolicyLoader` → `resilientSingleKey`(LKG + `SessionConcurrencyPolicyFloorSupplier`) → `l2SingleKey`；`LayeredCacheRegistry` 注入以暴露 Actuator 来源
  - 失效订阅放在 `SessionConcurrencyInvalidationAutoConfiguration`（自动配置阶段才能可靠 `@ConditionalOnBean(InvalidationBus)`）
- [x] T4-4：登录路径执行 maxSessions / overflow / adminForbidConcurrent
  - `SessionConcurrencyEnforcer` 由 `SessionRegistrar` 在**新登录**时调用，refresh 换发跳过（同 sid 不增会话数，且续期不应因策略源故障失败）
- [x] T4-5：client `UNIQUE/STANDARD` 映射与优先级（D15）
  - 顺序：`adminForbidConcurrent`(ADMIN→1) > 策略显式 `maxSessions` > Client `UNIQUE` 缺省 1 > 无限
- [x] T4-6：动态刷新与 remote 故障注入（A10）
  - 单测覆盖：`LocalSessionConcurrencyPolicySourceTest`（改配置即生效、地板非空）、`RemoteSessionConcurrencyPolicyResolverTest`（远端不可用 fail-closed）
  - 真实 Nacos 推送与停中心演练待联调环境
- [x] T4-7：新建 `specs/current/security/session-safety/`；roadmap L5 → `done`；归档本 change

**阶段门禁**：A9、A10、A14；REQUIREMENTS 验收标准全部勾选。

---

## 验证任务

- [x] V1：A1–A19 勾选，与 DESIGN 决议一致
- [x] V2：L2 锁定、L3 事件、L4 登录失败封禁回归
- [x] V3：无 `RedisTemplate.keys` 用于 `online:user:` 清理与发布清空脚本
- [x] V4：current 已按 As-Built 编写，非复制 change 原文
- [x] V5：全仓检索确认无 sid 回退分支与 `convertFromJwtOnly` 放行路径残留
- [x] V6：依赖方向自检（D19）——全仓检索 `bff_session` / `bffSessionKey`，确认写入方只有 `ingot-bff`、读取方只有 `ingot-bff` 与 `ingot-gateway`，`ingot-auth-provider` 及 `ingot-security-provider` 无任何引用
- [x] V7：契约唯一性自检（D20）——全仓检索 `AUTH_SERVICE`，确认 `@FeignClient` 定义只出现在 `ingot-auth-api`
- [x] V8：全仓检索 `TokenEndpoint`、`/token/tokens`、`/token/jti`、`RequestMapping("/token")`，确认已删除且无 deprecated 残留（D17）

## 完成检查

- [x] 实现与 DESIGN 一致（差异写入 README 完成记录）
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新：`specs/current/security/session-safety/`
- [x] Change 已记录完成信息并归档至 `specs/changes/archive/2026/`
- [x] [security-center-roadmap.md](../../../../../docs/requirements/themes/security-center-roadmap.md) L5 = `done`
- [x] [ROADMAP.md](../../../../../docs/requirements/ROADMAP.md) `R-2026-028` = `done`
