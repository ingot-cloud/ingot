# JWK 密钥管理 SPEC

> 记录当前已验收并在线生效的系统事实。

## 1. Redis 键结构

| 键 | 说明 |
|---|---|
| `in:security:jwk:current-key-id` | 当前用于签名的密钥 ID |
| `in:security:jwk:key-ids` | 活跃密钥 ID 集合 |
| `in:security:jwk:key:{keyId}:pub` | 公钥（十六进制编码） |
| `in:security:jwk:key:{keyId}:pri` | 私钥（可加密存储） |
| `in:security:jwk:key:{keyId}:encrypted` | 私钥是否加密（`true`/`false`） |
| `in:security:jwk:key:{keyId}:created` | 创建时间戳（毫秒） |

## 2. 配置（`ingot.security.jwk`）

| 属性 | 默认值 | 说明 |
|---|---|---|
| `master-key` | — | 私钥加密主密钥，建议 `${AUTH_JWK_MASTER_KEY}` |
| `enable-encryption` | `true` | 是否加密 Redis 私钥 |
| `key-lifetime` | `90d` | 密钥生命周期，到期触发轮换 |
| `key-grace-period` | `2h` | 旧密钥保留宽限期 |
| `max-active-keys` | `3` | 最大活跃密钥数 |
| `cache-refresh-interval` | `5m` | 资源服务器公钥缓存刷新间隔 |

## 3. 签名与验签职责

| 路径 | 组件 | 密钥 | 选择策略 |
|---|---|---|---|
| JWT 签发 | `NimbusJwtEncoder` + `JwkSelector` | 1 把私钥 | `AuthServerJwkSupplier.getCurrentSigningKey()`，对应 `current-key-id` |
| JWT 验签 | `JWSVerificationKeySelector` | N 把公钥 | JWT header `kid` 匹配 |
| JWKS 暴露 | `JwksEndpoint` | N 把公钥 | `jwkSupplier.get().toPublicJWKSet()` |

约束：

- 授权服务器 `authServerJwkSupplier` bean 类型为 `AuthServerJwkSupplier`（非 `JwkSupplier` 接口），bean 名 `authServerJwkSupplier`。
- `AuthServerJwtEncoderConfiguration` 通过 `@EnableInAuthorizationServer` 加载；`JwtEncoder` bean 使用 `@ConditionalOnBean(name = "authServerJwkSupplier")` 仅在授权服务器生效。
- `ResourceServerJwkSupplier` 不修改，多公钥验签是预期行为。

## 4. 密钥轮换

触发：`AuthServerJwkSupplier.get()` 检测到 `current-key-id` 对应密钥超过 `key-lifetime`。

行为：

1. 生成新密钥并写入 Redis，更新 `current-key-id`。
2. 旧密钥保留在 `key-ids` 集合中，供验签历史 JWT。
3. 超过 `key-lifetime + key-grace-period` 且超出 `max-active-keys` 的旧密钥被清理。

轮换后签发：新 JWT 的 `kid` 必须等于新的 `current-key-id`。

## 5. 公开端点

- `GET /.well-known/jwks.json`：返回所有活跃公钥，无需认证。

## 6. 已知限制

- `rotateKey()` 使用 JVM 内 `synchronized`，多授权服务器节点并发轮换可能产生额外密钥；签名选择器按 `current-key-id` 选取，不影响签发正确性。
