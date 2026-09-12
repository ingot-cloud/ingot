# 会话安全 SPEC

> 记录当前已验收并在线生效的系统事实（L5 As-Built）。

## 1. 会话模型

- 会话主键 **`sid`** = `OAuth2Authorization.id`，登录时写入 JWT claim `sid`（`InJwtClaimNames.SID`），此后 refresh 不换 sid。
- 自定义 grant 在签发前预生成 authorizationId，写入 stub `OAuth2Authorization`，保证 Customizer 能取到 sid。
- 运行时类型名仍为 `OnlineToken` / `OnlineTokenService`；主键与查询入口是 sid（`getBySid` / `removeBySid` / `isOnlineSid`）。`jti` 仅表示当前 Access Token，供管理面展示，不再建 Redis 索引。
- JWT 仍为瘦身形态：定位字段（`sid` / userId / tenantId / 客户端 OAuth scope）。`JwtInUserConverter` 按 sid 读 `token:sid:{sid}`，合并会话中的**角色码**、`userType`、`deptIds` 构建 `InUser`。业务权限码不进 JWT，也不以 `OnlineToken.authorities` 为 RBAC 事实来源；请求期由 `AuthorizationSnapshotFilter` 把快照 `permissionCodes` 合并进 `Authentication`，见 [data-authorization](../../pms/data-authorization/SPEC.md)。会话存储宽限不延长业务授权期限。

## 2. Redis 会话 schema

前缀收口在 `RedisKeyConstants.OnlineToken`。权威在线态只在 Auth Redis；安全中心不直连，一律 Inner Feign。

| Key | 类型 | TTL | 用途 |
|---|---|---|---|
| `token:sid:{sid}` | String(`OnlineToken`) | 对齐 Refresh Token 剩余寿命（无 RT 则对齐 AT） | 会话主数据 |
| `token:user:set:{tenantId}:{clientId}:{userId}` | Set\<sid\> | 成员会话 TTL 的最大值；新建后必须 `EXPIRE` | 用户会话集合 |
| `online:user:{tenantId}:{clientId}` | ZSet\<userId, expiresAtMs\> | 无 key TTL | 在线用户分页；撤销后按剩余会话最晚 `expiresAt` 回写 score |
| `session:ip:{tenantId}:{ip}` | Set\<sid\> | 同 userSet（只延长不缩短） | 同 IP 查询 |
| `session:online:registry` | Set\<`online:user:...` key\> | 无 | 小时任务扫描目标，替代 `KEYS` |

运行时**不再写入**：

- `token:jti:{jti}`：RS 只按 JWT `sid` 读主数据。
- UNIQUE `token:user:{tenantId}:{clientId}:{userId}`：互踢走完整 `revokeBySid`。

前缀常量保留，仅供 `bin/session_keys_purge.sh` 清存量匹配。

**TTL 规则**：集合索引只延长不缩短。`extendExpire`：TTL `-2`（键不存在）跳过；`-1`（存在未设过期）或短于本次会话寿命则设为 `sessionTtl`。小时任务 `OnlineTokenTask` 从 ZSet `ZREM` 过期 userId 时 `DEL` 对应 `token:user:set`。IP 孤儿集合靠该 IP 下次登录补 TTL，不 `SCAN session:ip:*`。

**读路径**：`getBySid` 只读 `token:sid:{sid}`，miss 即空。列出用户会话时 `MGET` 主数据后 `SREM` 墓碑 sid；空集合 `DEL`。分页 `listUserSessions(tenantId, clientId, userIds)` 对 userSet 做 pipeline `SMEMBERS`。

**IP 集合上限**：`ingot.security.session.ip-set-max-members` 默认 1000，超限不再 `SADD`。

**不在本 schema**：BFF 私有 `in:bff_session:{sessionId}` 由 BFF 独占读写；Auth / 安全中心不读不写，也不建 sid 反向索引。`oauth2:auth:*` / `oauth2:token:*` 仍由 `RedisOAuth2AuthorizationService` 管理，本能力不改其 TTL 计算。

## 3. Resource Server 校验

| 条件 | 行为 |
|---|---|
| JWT 无 `sid` | 拒绝（`invalid_token`），无 jti 回退 |
| `token:sid:{sid}` miss | 拒绝（视为已撤销） |
| Redis 读写异常 | `ingot.security.session.store-unavailable-grace`（默认 30s）内，签名仍有效的 JWT 可过；超期一律拒绝。打点 `session.redis.unavailable` |
| 长期 JWT-only fallback | **禁止**，无 `convertFromJwtOnly` 路径 |

`InTokenAuthFilter` 只把 sid 放入 `SessionContextHolder`；踢人效力来自 Converter 读 sid miss。热路径不写 `lastAccessAt`（仅登录与 refresh 更新）。

网关：`BearerJwtPayloadReader.readSid`；`AuthContextRelayFilter` / `ReactiveOnlineTokenUserTypeReader` 按 sid 读会话。

## 4. 撤销

唯一实现 `SessionRevocationService`（`DefaultSessionRevocationService`）：

```text
revokeBySid(sid, reason, actor):
  1. authorizationService.findById(sid) 存在 → remove(authorization)
     （remove 级联 onlineTokenService.removeBySid）
  2. 授权已不在但仍有会话残留 → removeBySid
  3. 按 reason 发 SESSION_REVOKED 或 LOGOUT（USER_LOGOUT）

revokeByUser(tenantId, clientId, userId, reason, actor):
  遍历 token:user:set 中每个 sid 调用 revokeBySid
  clientId 为空 → 该用户当前租户下全部 Client
```

`removeBySid` 若用户仍有其它会话，按剩余最晚 `expiresAt` 回写 `online:user` score。

**原因**（`SessionRevokeReason`）：`USER_LOGOUT`、`ADMIN_REVOKE`、`CONCURRENT_KICKOUT`、`PASSWORD_CHANGED`、`ACCOUNT_LOCKED`、`ACCOUNT_DISABLED`、`TENANT_REVOKE`。

不含 BFF 会话清理。管理员 / 账号联动 / 并发踢旧后，`in:bff_session:*` 残键靠下一次 401 引导重登，未重登则随 BFF 会话 TTL 消失。BFF 自助登出由 BFF 自己 `DEL` 该键。

## 5. 模块结构与依赖方向

```text
ingot-service/ingot-auth/
├── ingot-auth-api/        # RemoteAuthTokenService（/oauth2/**）、RemoteAuthSessionService
└── ingot-auth-provider/   # InnerSessionAPI、会话执行
```

`@FeignClient(AUTH_SERVICE)` 只出现在 `ingot-auth-api`。BFF / 安全中心 / 账号域 → Auth；Auth **不**反向调用它们，也不读写 `in:bff_session:*`。

| 路径 | 调用方 | 网关 |
|---|---|---|
| `/inner/session/**` | 安全中心、账号域、BFF | 不注册 |
| `/oauth2/**` | BFF Feign | 不注册 |
| `/auth/client/**` | 管理前端（`DevClientAPI`） | **保留** |
| `/auth/token/**` | — | **已摘除**（`TokenEndpoint` 已删除，无兼容层） |

## 6. 接口

### 6.1 Auth Inner（`/inner/session`，`@Permit(INNER)`）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/page` | 分页；query：tenantId、clientId、userId?、ip?、current、size |
| GET | `/{sid}` | 详情；已撤销返回成功且 data 为空 |
| GET | `/user` | 用户会话列表 |
| DELETE | `/{sid}` | body：reason、actorId |
| DELETE | `/user` | body：userId、tenantId、clientId?、reason、actorId；clientId 空 = 该租户全部 Client |
| DELETE | `/tenant/{tenantId}` | **501** 预留；Feign 契约未暴露 |

无 Feign fallback。中心调用失败返回 5xx 给管理员，不影响登录主链路。

### 6.2 Platform 会话（`/platform/security/sessions`）

经网关为 `/security/platform/security/sessions`。

| 方法 | 路径 | 权限 |
|---|---|---|
| GET | `` | `platform:security:session:query` |
| GET | `/{sid}` | 同上 |
| DELETE | `/{sid}` | `platform:security:session:revoke` |
| DELETE | `/user` | 同上 |

下线参数走 query（避免 DELETE body 被丢弃）。`reason` 固定 `ADMIN_REVOKE`，`actorId` 取当前管理员，不接受前端传入。用户名 / 租户名由中心拼 PMS；PMS 失败时名称可空，sid 级字段仍返回。

查询必须提供 `clientId` 或 `userId`（或两者）；全租户全 Client 翻页拒绝。仅 `clientId` 时 `total` 是**在线用户数**。字段与错误码见归档 [PLATFORM-API.md](../../../changes/archive/2026/20260817-security-session-safety/PLATFORM-API.md)。

### 6.3 Platform 并发策略（`/platform/security/session/concurrency-policies`）

| 方法 | 权限 |
|---|---|
| GET 列表 / `/{id}` | `platform:security:session:policy:query` |
| POST / PUT / DELETE `/{id}` | `platform:security:session:policy:update` |

PUT 为全量更新，未传可选字段会写成缺省值。GLOBAL 行禁止删除。

Auth 拉全量快照：`GET /inner/security/session/concurrency-policies`（含 `enabled=false`，以便 LKG 能表达「停用」）。

权限种子：`014_session_admin_permission_seed.sql`（会话）、`017_session_policy_permission_seed.sql`（策略，库 `ingot_core`）。菜单沿用 `platform:security:onlinetoken`。

## 7. 账号联动与安全事件

密码修改/重置、锁定、禁用成功后，账号域经 `SessionRevocationPort`（Feign `FeignSessionRevocationAdapter`，无 Auth 时 `NoOpSessionRevocationPort`）撤销该用户**当前租户、全部 Client** 的会话。

| 原因 | 事件 |
|---|---|
| `USER_LOGOUT` | 既有 `LOGOUT` |
| 其它强制下线 | `SESSION_REVOKED` |
| 并发踢旧 | `SESSION_CONCURRENT_KICKOUT` |

事件上报失败不回滚登出 / 下线。

## 8. 并发策略

**表** `ingot_security.session_concurrency_policy`（migration `016`）。`client_id` / `user_type` 为 `NOT NULL DEFAULT ''`（空串而非 NULL，保证唯一索引）。`user_type` 存枚举值 `0` / `1`。

| 列 | 含义 |
|---|---|
| `scope` | `GLOBAL` / `CLIENT` / `USER_TYPE` |
| `max_sessions` | `0` = 无限；`1` = 单会话 |
| `dimension` | P0 仅 `USER_CLIENT`（tenantId + clientId + userId） |
| `overflow` | `REJECT` / `KICK_OLDEST` / `KICK_ALL` |
| `admin_forbid_concurrent` | true 时 ADMIN 强制 N=1，高于策略显式值 |
| `enabled` | 停用 |

唯一索引 `(scope, client_id, user_type)`。种子一行 GLOBAL `max_sessions=0`，不可删。

**匹配**：命中即止，`CLIENT` > `USER_TYPE` > `GLOBAL`，不做字段级合并。

**D15**：client `UNIQUE` 仅在策略为无限时缺省 N=1；策略表显式值优先。紧急回退：`ingot.security.session.concurrency.enabled=false` 只认 client `UNIQUE/STANDARD`。

登录成功写会话前，`SessionConcurrencyEnforcer` 按 dimension 数 sid 集合并应用 overflow。踢旧走 `revokeBySid(..., CONCURRENT_KICKOUT)`。

**Auth 配置**（`in-service-auth.yml`，前缀 `ingot.security.session`）：

| 键 | 现行 |
|---|---|
| `store-unavailable-grace` | `30s` |
| `mode` | DEV `local`；TEST / PROD `remote` |
| `concurrency.enabled` | `true` |
| `concurrency.max-sessions` | 地板；PROD 为 `0`（中心不可用时不误伤登录） |
| `concurrency.overflow` | `KICK_OLDEST` |
| `ip-set-max-members` | `1000` |

remote 缓存名 `session-concurrency-policy`：`LayeredCacheBuilder`（L1 → 失效通知 → L2 → Resilient(Feign → LKG → Nacos 地板)）。地板关闭且无 LKG → **拒绝新登录**。配置键归属 Auth，框架只收 `LayeredCacheSettings`。

失效域 `SecurityPolicyDomain.SESSION_CONCURRENCY`。订阅在 `SessionConcurrencyInvalidationAutoConfiguration`（不能放进经 `@EnableInAuthorizationServer` 导入的用户 `@Configuration`，否则 `@ConditionalOnBean(InvalidationBus)` 恒假）。

## 9. 运维

- 发布清空存量会话 / 旧 jti 索引 / BFF 会话键：`bin/session_keys_purge.sh`（SCAN + UNLINK，默认 dry-run，`--apply` 执行）。上线无 sid 的旧 JWT 立即失效，用户重新登录。
- 小时任务扫 `session:online:registry`，禁止对 `online:user:` 使用 `RedisTemplate.keys`。
- 登录路径分组修正：`015_login_auth_group_pattern_fix.sql`（把仍含 `/auth/token/**` 的种子行改为现网登录路径）。

## 10. 已知限制

- 需求 5.2 设备维度（可信设备、按设备撤销）未做；管理面只展示已采集的 IP / UA / deviceType / OS / browser。
- 无交互式「由用户选择踢哪一个」；overflow 只有拒绝 / 踢最旧 / 踢全部。
- 非自助路径不清 BFF 会话键（依赖单向）。残键不是漏洞：对应 Refresh Token 已随 Authorization 失效。
- 网关仍暴露 `/auth/client/**`。
- Inner `DELETE /inner/session/tenant/{tenantId}` 返回 501；Platform 无租户级踢光交互。
- `OnlineToken` 仍内嵌 `authorities`（登录角色码）与 `deptIds`，不在本能力删除；业务 RBAC 以授权快照为准。
- 小时任务不扫描 `session:ip:*`；IP 孤儿集合靠下次登录补 TTL。
- `oauth2:auth:*` / `oauth2:token:*` 的 TTL 取授权下最晚过期（通常 RT），本能力未改。
