# Requirements

来源：[安全中心服务需求 §五](../../../../docs/requirements/themes/security-center-service.md) 的 5.1 在线会话、5.3 并发会话、5.4 强制下线。5.2 登录设备为非目标。

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

- 使用者：已登录用户（含 BFF `DELETE /bff/auth/logout`）。
- 触发条件：调用 Auth `DELETE /token`（或 BFF 封装）。
- 期望结果：
  - 当前会话的 OAuth2Authorization（含 Refresh Token 索引）与 Redis 会话一并删除。
  - BFF 清除 `in:bff_session:{sessionId}` 与 Cookie。
  - 上报 `LOGOUT` 安全事件（已有类型）；`session_id` 填 sid。

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
  - 登录、refresh、自助登出、UNIQUE/N 并发（读 Nacos 地板）正常。
  - **没有** Platform 统一会话管理页；管理员历史 `/auth/token/tokens` 在兼容期结束后不再作为正式管理面。
  - 账号域联动撤销仍走 Auth Inner（同进程或 Feign 到 Auth），不依赖安全中心。

### S12 安全中心不可用（管理面降级）

- 使用者：管理员。
- 触发条件：`ingot-security` 宕机。
- 期望结果：主登录链路不受影响；Platform 会话页不可用。并发策略走 LKG，无 LKG 走 Nacos 地板（D4）。

### S13 Redis 会话存储故障（D1）

- 使用者：已登录用户访问资源服务器。
- 触发条件：Redis 对会话 key 的读写异常（不是「键不存在」）。
- 期望结果：行为符合 D1 决议；键不存在一律拒绝。打点可观测，不得静默 fail-open 到「只验 JWT 签名」。

## 业务规则

### 会话标识（P0）

1. `sid` 等于 `OAuth2Authorization.getId()`，登录时确定，refresh 不变，登出 / 强制下线后不得复用同一 sid 的有效会话。
2. `jti` 仍是 Access Token 实例 ID，仅作索引与排错，不再当会话主键。
3. 不引入第四套对外标识：BFF Cookie `sessionId` 继续只服务浏览器 Cookie 会话，与 auth `sid` 通过 `BffSession.sid` 关联（D5）。

### 在线判断（P0）

4. Resource Server 必须以 Redis 会话存在为准；JWT 未过期但会话不存在 → 拒绝。
5. 不得再使用「OnlineToken miss → `convertFromJwtOnly`」作为 STANDARD 客户端的长期路径。
6. `InTokenAuthFilter` 的 UNIQUE 校验从「比对最新 jti」改为「校验 JWT.sid 对应会话存在且（若策略要求）为该用户当前允许集合之一」。

### 撤销（P0）

7. 任何强制下线 / 自助登出必须同时：删除 Redis 会话与索引、删除 `OAuth2Authorization`（从而失效 Refresh Token）。
8. 不引入独立 JWT 黑名单；会话缺失即视为 revoked。
9. 下线必须记录原因：`USER_LOGOUT` / `ADMIN_REVOKE` / `PASSWORD_CHANGED` / `PASSWORD_RESET` / `ACCOUNT_LOCKED` / `ACCOUNT_DISABLED` / `CONCURRENT` / `POLICY`。
10. UNIQUE 新登录踢旧必须走完整撤销，禁止只 `removeByJti`。

### 查询（P0 / P1）

11. P0 查询维度：`tenantId + clientId` 分页、按 `userId`、按 `sid`、按 IP。
12. 跨租户全局扫描不是 P0（避免 KEYS）；清理过期 ZSet 必须用 SCAN 或注册表，禁止 `redisTemplate.keys`。

### 并发策略（P0）

13. 策略可表达：无限、最多 N、每 Client 一个（即 N=1 且维度 `USER_CLIENT`）、管理员账号禁止并发。
14. 「每个设备一个会话」依赖 5.2，本闭环不做。
15. 超限行为 P0：`REJECT` / `KICK_OLDEST` / `KICK_ALL`。
16. 策略加载：`mode=local` 读 Nacos；`mode=remote` 走 `remote → LKG → Nacos 地板`；策略变更经 `SecurityPolicyDomain.SESSION_CONCURRENCY` 失效广播，无重启生效。
17. 现网 client `tokenSettings.auth-type=UNIQUE` 在地板与迁移期映射为 `maxSessions=1, overflow=KICK_ALL, dimension=USER_CLIENT`；`STANDARD` 映射为无限。

### 事件与联动（P0）

18. 管理员下线、并发踢人使用新事件类型；用户自助登出使用已有 `LOGOUT`。
19. 事件失败不回滚已经完成的 Redis / Authorization 删除（与 L3 BEST_EFFORT/DURABLE 原则一致）。强制下线事件默认 DURABLE。
20. 密码 / 锁定 / 禁用的撤销在账号用例成功之后异步或同步调用 Auth Inner；失败必须打错误日志与指标，并在 TASKS 验收中规定重试或补偿（不得静默丢）。

### 权限（P0）

21. Platform 会话接口：`@AdminOrHasAnyAuthority({"platform:security:session:query|revoke"})`。
22. Auth Inner 会话接口：`@Permit(mode = PermitMode.INNER)`。
23. 用户自助登出仅能撤销**当前** sid，不能凭普通用户身份撤销他人会话。

## 边界与非目标

- 兼容：已发出的无 sid JWT 仅在 D2 窗口内可用；窗口外必须重新登录或 refresh。
- 多实例 Auth：会话在共享 Redis，撤销立即对所有 Resource Server 生效（受 D1 本地短缓存限制，若采用）。
- 不在本闭环解决网关不做 JWT 签名校验的既有分工（鉴权仍在下游 RS）。
- 不在本闭环做会话历史库、设备可信体系、风险引擎下线。
- 租户级「踢光某个租户全部会话」列为 P1 能力：Inner 可留接口，Platform 完整交互不阻塞 Phase 01–02 验收。

## 验收标准

- [ ] A1：新登录 JWT 含 `sid`，且 `sid == authorizationId`。
- [ ] A2：refresh 后 `sid` 不变、`jti` 变；旧 jti 的 Redis key 不残留。
- [ ] A3：管理员按 sid 下线后，Access Token 立即被 RS 拒绝，Refresh Token 换发失败。
- [ ] A4：`STANDARD` 客户端执行 A3 同样成功（不再 JWT-only 放行）。
- [ ] A5：用户 `DELETE /token` 与管理员下线共用同一撤销实现（Authorization + 会话索引）。
- [ ] A6：UNIQUE / `maxSessions=1` 新登录踢旧后，旧 Refresh Token 无效。
- [ ] A7：密码修改、密码重置、锁定、禁用后，该用户当前租户会话全部失效。
- [ ] A8：安全中心可按租户+Client 分页查看在线会话，并展示用户/租户名称（PMS 拼装）。
- [ ] A9：不部署安全中心时，登录 / refresh / 自助登出 / local 并发策略仍可用。
- [ ] A10：`mode=remote` 时停安全中心，并发策略走 LKG 或 Nacos 地板；改 Nacos 地板无重启后新登录行为变化（动态刷新）。
- [ ] A11：强制下线写入 `SESSION_REVOKED`（或并发场景 `SESSION_CONCURRENT_KICKOUT`），含 sid 与原因。
- [ ] A12：过期在线用户清理不再调用 `RedisTemplate.keys`。
- [ ] A13：Auth 管理端 `/token/tokens`、`/token/jti`、`/token/user` 在兼容期后不再作为正式管理面（或明确废弃并转发）。
- [ ] A14：D1–D5 已在 DESIGN 决策表闭合，实现与决议一致。
