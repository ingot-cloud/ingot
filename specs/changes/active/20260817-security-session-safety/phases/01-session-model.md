# Phase 01 · 会话模型与撤销收口

> 状态：pending

## 目标

引入 `sid = authorizationId`，把 OnlineToken 升为会话主数据；强制下线与 UNIQUE 踢旧必须同时撤销 OAuth2Authorization；Resource Server 以 Redis 会话为准拒绝访问。本 Phase **不**做安全中心 Platform、不接入并发策略表；**做** Auth 模块拆分（D20），以便后续 Feign 契约有归属。

**发布性质（D2）**：不兼容无 sid 的旧 JWT，本 Phase 上线即一次全局强制下线，需低峰窗口 + 发布预案；回滚代价对称。

## 实现要点

- **模块拆分（D20）**：`ingot-auth` → `ingot-auth-api` + `ingot-auth-provider`，对齐 pms / member / security。
  - api：`EnableAPIConfiguration` + `AutoConfiguration.imports`；`RemoteAuthTokenService` 承接 BFF 现有 `/oauth2/**` 协议端点。`DELETE /token` 仅作本 Phase 施工桥（内部改 `revokeBySid`），Phase 02 随 `TokenEndpoint` 一起删除。
  - provider：现有实现整体平移（含 Dockerfile、`ingot-assemble.gradle`）。
  - `settings.gradle` 两行 include；`config/ingot.gradle` 增加 `ingot.auth_api`；`.gitlab-ci.yml` assemble 改为 `:ingot-service:ingot-auth:ingot-auth-provider`。
  - BFF 删除 `AuthClient`，改依赖 `ingot-auth-api`。
- `InJwtClaimNames.SID`、`JwtClaimNamesExtension` 转发。
- `RedisKeyConstants.OnlineToken`：`sidKey`、`uniqueUserKey`、`userSetKey`、`onlineUserKey`、`ipSetKey`、`onlineRegistryKey`；删除 Service 内硬编码前缀。
- `OnlineToken`：`sid`、`lastAccessAt`；`jti` 表示当前 Access Token。
- `RedisOnlineTokenService.save`：upsert 按 sid；refresh 删旧 jti 索引；修正 user Set TTL（DESIGN D 节「TTL 修正」）。
- `JwtOAuth2TokenCustomizer`：写 sid；`save(user, sid, jti, exp)`。
- `OAuth2CustomAuthenticationProvider`：预生成 id，stub Authorization 放入 token context，保存时 `builder.id(sid)`。
- 新建 `SessionRevocationService`（建议放 authorization-server 或 auth 领域层，避免 Endpoint 直接拼 Redis）。
- `RedisOAuth2AuthorizationService.remove` → `removeBySid`。
- `kickOldTokenIfUnique` → `revokeBySid`。
- `JwtInUserConverter`：删除 `convertFromJwtOnly` 放行路径；miss → 无效 token。
- `InTokenAuthFilter` / `JwtTenantValidator`：按 sid。
- D1：Redis 异常短窗口；键不存在拒绝。可用微小 Caffeine 正缓存（≤5s）降低 Redis 抖动，负缓存可选。
- D2：**无兼容分支**。JWT 无 sid → 拒绝；不引入 `legacy-jti-fallback` 开关，不做双读。
- 网关 `BearerJwtPayloadReader` / `ReactiveOnlineTokenUserTypeReader` / `AuthContextRelayFilter`。
- `BffSession.sid`：选租户拿到 Token 后写入。**不**建 `in:bff_session:sid:*` 反向索引，`CacheConstants` 不新增键（D19）。
- `BffAuthService.logout`：本 Phase 暂走施工桥 `RemoteAuthTokenService`（Auth `DELETE /token` 内 `revokeBySid`），再删自己的会话键与 Cookie；`sid` 写入 `BffSession`。Phase 02 Inner 落地后切到 `RemoteAuthSessionService`，施工桥删除。
- `SessionRevocationService` **不含**任何 BFF 键操作 —— Auth 无出向依赖，不读写 `in:bff_session:*`（D19）。
- `OnlineTokenTask`：遍历 `session:online:registry`，禁止 `keys()`。
- `TokenEndpoint`：本 Phase 仅保留 `DELETE /token` 作为施工桥并改为 `revokeBySid`；`/tokens` `/jti` `/user` 可在本 Phase 直接删除（D17，无兼容窗口），最迟 Phase 02 整类删除。
- 发布运维脚本：清空 `in:bff_session:*`、`token:jti:*`、`oauth2:auth:*`、`oauth2:token:*`（用 SCAN，不用 KEYS），随发布执行。因不做运行时跨服务清理，这是清除存量 BFF 会话键的唯一时机。

## 退出条件

- [ ] 新登录 JWT 含 sid 且等于 authorizationId。
- [ ] refresh 后 sid 不变、旧 jti key 删除、Refresh Token 仍对应同一 authorization。
- [ ] 无 sid 的 JWT 被 RS 拒绝；代码中无 jti 回退与 `convertFromJwtOnly` 放行分支（A15）。
- [ ] 管理员通过 `SessionRevocationService`（或临时 Inner 前的测试入口）按下线后，Refresh Token 换发失败；STANDARD 客户端 RS 拒绝旧 AT。
- [ ] UNIQUE 新登录后旧 RT 无效。
- [ ] BFF 自助登出清除会话键与 Cookie（本 Phase 经施工桥 `DELETE /token`）。
- [ ] 管理员下线 BFF 来源会话后，Auth 侧未访问 `in:bff_session:*`；该浏览器下次请求 401、重登可恢复（A17）。
- [ ] 清理代码路径无 `redisTemplate.keys`。
- [ ] `ingot-auth-api` / `ingot-auth-provider` 可编译；BFF 走 `RemoteAuthTokenService`，自建 `AuthClient` 已删除。
- [ ] 相关单测通过。

## 回滚

回退 Auth 与 RS 版本。**无兼容层，回滚同样触发一次强制下线**；`token:sid:*` 待 TTL 或随回滚脚本清空。不回滚已撤销的 Authorization。
