# Requirements

来源：[安全中心服务需求 §五](../../../../../docs/requirements/themes/security-center-service.md) 的 5.1 在线会话、5.3 并发会话、5.4 强制下线。5.2 登录设备为非目标。

## 用户场景

### S1 登录产生稳定会话（sid）

- 使用者：任意通过授权码 / 自定义 grant 登录的用户。
- 触发条件：Auth 签发 Access Token。
- 期望结果：
  - JWT 含 `sid` 与 `jti`；`sid` 等于本次 `OAuth2Authorization.id`。
  - Redis 写入会话主数据（按 sid）及 jti → sid 索引。
  - 登录环境字段（IP / UA / deviceType / OS / browser）继续采集；`location` 允许空。

### S2 Refresh 不新建会话

- 使用者：持有有效 Refresh Token 的客户端（含 BFF）。
- 触发条件：`grant_type=refresh_token` 换发 Access Token。
- 期望结果：
  - 新 JWT 的 `sid` 与旧 Access Token 相同，`jti` 更新。
  - Redis 会话主数据原地更新（当前 jti、`expiresAt`、`lastAccessAt`）；旧 `token:jti:{oldJti}` 删除。
  - 同一 `oauth2:auth:{sid}` 记录被覆盖，不新建 authorization。

### S3 用户自助登出

- 使用者：已登录用户（经 Web BFF；将来可经 App BFF）。
- 触发条件：调用 BFF 登出接口（`DELETE /bff/auth/logout` 或将来 App BFF 对等接口）。
- 期望结果：
  - BFF 按 `BffSession.sid` Feign 调 Auth Inner `revokeBySid`，当前会话的 OAuth2Authorization（含 Refresh Token）与 Redis 会话一并删除。
  - BFF 清除自己的 `in:bff_session:{sessionId}` 与 Cookie。
  - Access Token 已过期但 Cookie 会话仍在时，按 sid 仍能撤销成功。
  - Auth **没有**对外的 `DELETE /token`。
  - 上报 `LOGOUT` 安全事件；`session_id` 填 sid。

### S4 管理员查看在线会话

- 使用者：平台管理员（安全中心）。
- 触发条件：打开会话管理页，按租户 + Client 分页；可按用户、IP 筛选。
- 期望结果：
  - 列表字段覆盖需求 5.1 中除「登录地点」外的展示项（地点允许空）。
  - 可查看会话详情、某用户全部会话、同 IP 会话。
  - 同设备会话：本闭环仅按 `userAgent` 哈希近似聚合，不引入设备主键（5.2）。
  - 用户名 / 租户名由安全中心调 PMS 拼装；Auth Inner 只返回会话运行时数据。

### S5 管理员撤销指定会话

- 使用者：平台管理员。
- 触发条件：对某个 sid 执行强制下线。
- 期望结果：
  - 该 sid 的 Redis 会话消失；对应 Refresh Token 无法再换发。
  - 持该 sid 的 Access Token（即使 JWT 未过期）在 Resource Server 被拒绝。
  - `STANDARD` 与 `UNIQUE` 客户端同样生效（不再出现「只删 OnlineToken、JWT 仍可用」）。
  - 若该会话来自 BFF：Auth **不**清除 BFF 侧 `in:bff_session:*`（D19）。浏览器下次访问得到一次 401，前端按既有 401 处理跳登录页，重登时覆盖会话键；未重登的残键随 BFF 会话 TTL 消亡。该行为是设计预期，不是缺陷。
  - 上报 `SESSION_REVOKED`，含操作者、原因、sid。

### S6 管理员撤销某用户在指定 Client 下的全部会话

- 使用者：平台管理员。
- 触发条件：按 `userId + tenantId + clientId` 强制下线。
- 期望结果：该组合下全部 sid 按 S5 语义撤销；Refresh Token 全部失效。

### S7 密码修改 / 重置后全部下线

- 使用者：当事用户的其它终端。
- 触发条件：账号域成功修改或重置密码。
- 期望结果：该用户在当前租户下的全部会话被撤销（所有 Client）；原因 `PASSWORD_CHANGED` / `PASSWORD_RESET`；事件 `SESSION_REVOKED`。

### S8 账号锁定 / 禁用后全部下线

- 使用者：被锁定或禁用的用户。
- 触发条件：`ACCOUNT_LOCKED` / `ACCOUNT_DISABLED` 用例成功。
- 期望结果：撤销该用户当前租户下全部会话。网关锁定信号仍保留（L2），本闭环补「已签发 Token 立即失效」，避免锁定窗口内旧 JWT 继续访问资源服务器。

### S9 并发：只允许一个会话（UNIQUE 语义升级）

- 使用者：配置为单会话的 Client 或命中「最多 1 个 / 管理员禁止并发」策略的用户。
- 触发条件：同一 `tenantId + clientId + userId` 再次登录成功。
- 期望结果：旧会话按 S5 彻底撤销（含 Refresh Token），不只删 Redis jti；上报 `SESSION_CONCURRENT_KICKOUT`。

### S10 并发：最多 N 个会话

- 使用者：命中 `maxSessions=N`（N>1）策略的用户。
- 触发条件：已有 N 个有效会话时再次登录。
- 期望结果：按策略执行 `REJECT`（拒绝新登录）/ `KICK_OLDEST`（踢最旧 sid）/ `KICK_ALL`（踢全部旧会话）；新登录仅在非 REJECT 时成功。

### S11 未部署安全中心（降级）

- 使用者：只部署 Auth + Gateway + BFF 的环境。
- 触发条件：`ingot.security.session.mode=local`，或不部署 `ingot-security`。
- 期望结果：
  - 登录、refresh、BFF 自助登出、UNIQUE/N 并发（读 Nacos 地板）正常。
  - **没有** Platform 统一会话管理页；管理员签退须经安全中心或直接调 Auth Inner，Auth 不再提供 `/token/**`。
  - 账号域联动撤销仍走 Auth Inner（同进程或 Feign 到 Auth），不依赖安全中心。

### S12 安全中心不可用（管理面降级）

- 使用者：管理员。
- 触发条件：`ingot-security` 宕机。
- 期望结果：主登录链路不受影响；Platform 会话页不可用。并发策略走 LKG，无 LKG 走 Nacos 地板（D4）。

### S13 Redis 会话存储故障（D1）

- 使用者：已登录用户访问资源服务器。
- 触发条件：Redis 对会话 key 的读写异常（不是「键不存在」）。
- 期望结果：行为符合 D1 决议；键不存在一律拒绝。打点可观测，不得静默 fail-open 到「只验 JWT 签名」。

### S14 上线后持旧 Token 访问（D2：不兼容）

- 使用者：发布前已登录、Token 中无 `sid` 的用户。
- 触发条件：Phase 01 发布完成后携旧 Access Token 请求，或用旧 Refresh Token 换发。
- 期望结果：
  - Resource Server 返回 401 `invalid_token`；不存在「无 sid 回退 jti」的放行分支。
  - 客户端按既有 401 处理引导重新登录。
  - 不提供过渡开关；发布视为一次全局强制下线，需在发布预案中声明并选择低峰窗口。

## 业务规则

### 会话标识（P0）

1. `sid` 等于 `OAuth2Authorization.getId()`，登录时确定，refresh 不变，登出 / 强制下线后不得复用同一 sid 的有效会话。
2. `jti` 仍是 Access Token 实例 ID，仅作索引与排错，不再当会话主键。
3. 不引入第四套对外标识：BFF Cookie `sessionId` 继续只服务浏览器 Cookie 会话，通过 `BffSession.sid` 单向记录对应的 auth 会话；**不建 sid → sessionId 反向索引**（D19）。

### 在线判断（P0）

4. Resource Server 必须以 Redis 会话存在为准；JWT 未过期但会话不存在 → 拒绝。
5. 不得再使用「OnlineToken miss → `convertFromJwtOnly`」作为 STANDARD 客户端的长期路径；该方法作为放行分支整体移除。
6. JWT 缺少 `sid` claim → 直接拒绝，不回退 jti（D2）。
7. `InTokenAuthFilter` 的 UNIQUE 校验从「比对最新 jti」改为「校验 JWT.sid 对应会话存在且（若策略要求）为该用户当前允许集合之一」。

### 撤销（P0）

8. 任何强制下线 / 自助登出必须同时：删除 Redis 会话与索引、删除 `OAuth2Authorization`（从而失效 Refresh Token）。
9. 撤销执行统一在 Auth Inner；入口按来源分流：自助登出由 BFF（及将来 App BFF）编排后调 Inner，管理面经安全中心调 Inner，账号联动由账号域直调 Inner。BFF **不**提供踢人接口，也不承担会话查询。
10. **依赖单向**：Auth 不得调用 BFF，也不得读写 `in:bff_session:*`（D19）。非自助路径撤销后的 BFF 残键由 401 与 TTL 收敛，不做跨服务清理。
11. 不引入独立 JWT 黑名单；会话缺失即视为 revoked。
12. 下线必须记录原因：`USER_LOGOUT` / `ADMIN_REVOKE` / `PASSWORD_CHANGED` / `PASSWORD_RESET` / `ACCOUNT_LOCKED` / `ACCOUNT_DISABLED` / `CONCURRENT` / `POLICY`。
13. UNIQUE 新登录踢旧必须走完整撤销，禁止只 `removeByJti`。

### 查询（P0 / P1）

14. P0 查询维度：`tenantId + clientId` 分页、按 `userId`、按 `sid`、按 IP。
15. 跨租户全局扫描不是 P0（避免 KEYS）；清理过期 ZSet 必须用 SCAN 或注册表，禁止 `redisTemplate.keys`。

### 并发策略（P0）

16. 策略可表达：无限、最多 N、每 Client 一个（即 N=1 且维度 `USER_CLIENT`）、管理员账号禁止并发。
17. 「每个设备一个会话」依赖 5.2，本闭环不做。
18. 超限行为 P0：`REJECT` / `KICK_OLDEST` / `KICK_ALL`。
19. 策略加载：`mode=local` 读 Nacos；`mode=remote` 走 `remote → LKG → Nacos 地板`；策略变更经 `SecurityPolicyDomain.SESSION_CONCURRENCY` 失效广播，无重启生效。
20. 现网 client `tokenSettings.auth-type=UNIQUE` 在地板与迁移期映射为 `maxSessions=1, overflow=KICK_ALL, dimension=USER_CLIENT`；`STANDARD` 映射为无限。

### 事件与联动（P0）

21. 管理员下线、并发踢人使用新事件类型；用户自助登出使用已有 `LOGOUT`。
22. 事件失败不回滚已经完成的 Redis / Authorization 删除（与 L3 BEST_EFFORT/DURABLE 原则一致）。强制下线事件默认 DURABLE。
23. 密码 / 锁定 / 禁用的撤销在账号用例成功之后异步或同步调用 Auth Inner；失败必须打错误日志与指标，并在 TASKS 验收中规定重试或补偿（不得静默丢）。

### 权限（P0）

24. Platform 会话接口：`@AdminOrHasAnyAuthority({"platform:security:session:query|revoke"})`。
25. Auth Inner 会话接口：`@Permit(mode = PermitMode.INNER)`，依赖 `OAuth2InnerResourceFilter` 校验 `In-Inner-From` 与网关入口剥头，不额外开放对外路由。
26. 用户自助登出仅能撤销**当前** sid。该约束由 BFF 保证（只传本会话 `BffSession.sid`）；Auth Inner 是服务间接口，不按终端用户身份再鉴权一次。

### 模块与暴露（P0）

27. Auth 的内部 Feign 契约必须有自己的归属模块 `ingot-auth-api`（D20）；三个消费方（安全中心 provider、账号域 adapter、BFF）共用同一契约，禁止在消费方各自定义。BFF 现有自建 `AuthClient` 迁入后删除。
28. Auth 会话相关 HTTP 只留 `/inner/session/**`（D8）。`TokenEndpoint` `/token/**` 删除，无 deprecated（D17）。网关 `in-service-auth` 仅保留 `/auth/client/**`（D21）。
29. `/inner/session/**` 与 `/oauth2/**` 不注册网关路由，仅供 Feign 直连。

## 边界与非目标

- **不兼容旧 JWT**（D2）：无 sid 的存量 Token 上线即失效，Phase 01 发布等同一次全局强制下线；需低峰窗口与发布预案，回滚同样造成一次强制下线。
- **不兼容旧 HTTP API**（D17）：`TokenEndpoint` 删除后不提供过渡路径；直连客户端须改走 BFF 或安全中心。
- 多实例 Auth：会话在共享 Redis，撤销立即对所有 Resource Server 生效（受 D1 本地短缓存限制，若采用）。
- BFF 只在用户自助登出时清除自己的 Cookie 会话，不提供会话查询或踢人 API；非自助路径不做 BFF 会话联动清理（D19）。
- `DevClientAPI` 与 `/auth/client/**` 本期不迁移，网关继续暴露该端点（D21）。
- 不在本闭环解决网关不做 JWT 签名校验的既有分工（鉴权仍在下游 RS）。
- 不在本闭环做会话历史库、设备可信体系、风险引擎下线。
- 租户级「踢光某个租户全部会话」列为 P1 能力：Inner 可留接口，Platform 完整交互不阻塞 Phase 01–02 验收。

## 验收标准

- [x] A1：新登录 JWT 含 `sid`，且 `sid == authorizationId`。
- [x] A2：refresh 后 `sid` 不变、`jti` 变；旧 jti 的 Redis key 不残留。
- [x] A3：管理员按 sid 下线后，Access Token 立即被 RS 拒绝，Refresh Token 换发失败。
- [x] A4：`STANDARD` 客户端执行 A3 同样成功（不再 JWT-only 放行）。
- [x] A5：BFF 自助登出与管理员下线共用同一 `SessionRevocationService`（均经 Auth Inner；Authorization + 会话索引）。
- [x] A6：UNIQUE / `maxSessions=1` 新登录踢旧后，旧 Refresh Token 无效。
- [x] A7：密码修改、密码重置、锁定、禁用后，该用户当前租户会话全部失效。
- [x] A8：安全中心可按租户+Client 分页查看在线会话，并展示用户/租户名称（PMS 拼装）。
- [x] A9：不部署安全中心时，登录 / refresh / BFF 自助登出 / local 并发策略仍可用。
- [x] A10：`mode=remote` 时停安全中心，并发策略走 LKG 或 Nacos 地板；改 Nacos 地板无重启后新登录行为变化（动态刷新）。
- [x] A11：强制下线写入 `SESSION_REVOKED`（或并发场景 `SESSION_CONCURRENT_KICKOUT`），含 sid 与原因。
- [x] A12：过期在线用户清理不再调用 `RedisTemplate.keys`。
- [x] A13：`TokenEndpoint` 及 `/token/**` 已删除；代码与网关均无该路径；无 deprecated 包装（D8 / D17）。
- [x] A14：D1–D21 已在 DESIGN 决策表闭合，实现与决议一致。
- [x] A15：无 `sid` claim 的 JWT 被 RS 拒绝，代码中不存在 jti 回退或 `convertFromJwtOnly` 放行分支。
- [x] A16：BFF 自助登出按 `BffSession.sid` 调 Auth Inner 撤销成功（含 Access Token 已过期的场景），并清除自己的会话键与 Cookie。
- [x] A17：管理员下线 BFF 来源会话后，Auth 侧代码路径未访问 `in:bff_session:*`；该浏览器下次请求得 401 且重登可恢复（D19 行为断言）。
- [x] A18：`ingot-auth-api` 已落地；会话与 Token Feign 契约均在该模块，`ingot-security-api` 与 BFF 内无重复定义；BFF 自建 `AuthClient` 已删除（D20）。
- [x] A19：网关 `in-service-auth` 路由仅剩 `/auth/client/**`。
