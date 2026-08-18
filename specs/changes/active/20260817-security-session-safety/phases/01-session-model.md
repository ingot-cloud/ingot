# Phase 01 · 会话模型与撤销收口

> 状态：pending

## 目标

引入 `sid = authorizationId`，把 OnlineToken 升为会话主数据；强制下线与 UNIQUE 踢旧必须同时撤销 OAuth2Authorization；Resource Server 以 Redis 会话为准拒绝访问。本 Phase **不**做安全中心 Platform、不接入并发策略表。

## 实现要点

- `InJwtClaimNames.SID`、`JwtClaimNamesExtension` 转发。
- `RedisKeyConstants.OnlineToken`：`sidKey`、`uniqueUserKey`、`userSetKey`、`onlineUserKey`、`ipSetKey`、`onlineRegistryKey`；删除 Service 内硬编码前缀。
- `OnlineToken`：`sid`、`lastAccessAt`；`jti` 表示当前 Access Token。
- `RedisOnlineTokenService.save`：upsert 按 sid；refresh 删旧 jti 索引；修正 user Set TTL（DESIGN D 节「TTL 修正」）。
- `JwtOAuth2TokenCustomizer`：写 sid；`save(user, sid, jti, exp)`。
- `OAuth2CustomAuthenticationProvider`：预生成 id，stub Authorization 放入 token context，保存时 `builder.id(sid)`。
- 新建 `SessionRevocationService`（建议放 authorization-server 或 auth 领域层，避免 Endpoint 直接拼 Redis）。
- `RedisOAuth2AuthorizationService.remove` → `removeBySid`。
- `kickOldTokenIfUnique` → `revokeBySid`。
- `JwtInUserConverter`：删除 `convertFromJwtOnly` 作为 STANDARD 放行路径；miss → 无效 token。
- `InTokenAuthFilter` / `JwtTenantValidator`：按 sid。
- D1：Redis 异常短窗口；键不存在拒绝。可用微小 Caffeine 正缓存（≤5s）降低 Redis 抖动，负缓存可选。
- D2：无 sid 时回退 jti；用配置 `ingot.security.session.legacy-jti-fallback-enabled`（默认 true，窗口结束后 false）。
- 网关 `BearerJwtPayloadReader` / `ReactiveOnlineTokenUserTypeReader` / `AuthContextRelayFilter`。
- `BffSession.sid`；选租户后写入；logout 优先 sid。
- `OnlineTokenTask`：遍历 `session:online:registry`，禁止 `keys()`。
- `TokenEndpoint.DELETE /token` 改 `revokeBySid`；`/jti` `/user` 先接到新撤销（彻底性），正式退役放 Phase 03。

## 退出条件

- [ ] 新登录 JWT 含 sid 且等于 authorizationId。
- [ ] refresh 后 sid 不变、旧 jti key 删除、Refresh Token 仍对应同一 authorization。
- [ ] 管理员按 jti 或用户下线后，Refresh Token 换发失败；STANDARD 客户端 RS 拒绝旧 AT。
- [ ] UNIQUE 新登录后旧 RT 无效。
- [ ] 清理代码路径无 `redisTemplate.keys`。
- [ ] 相关单测通过。

## 回滚

关闭 sid 写入、RS 恢复 jti + 允许 JWT-only（仅紧急）；`token:sid:*` 待 TTL。不回滚已撤销的 Authorization。
