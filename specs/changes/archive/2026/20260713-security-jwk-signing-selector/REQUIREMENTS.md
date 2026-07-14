# Requirements

## 用户场景

1. **密钥轮换后登录**：用户通过 OAuth2 授权码流程换取 token 时，Redis 中存在 2 把及以上活跃 JWK（当前密钥 + 历史密钥），系统应成功签发 access_token。
2. **旧 token 验证**：密钥轮换后，使用旧密钥签发的、尚未过期的 JWT 仍能被资源服务器验签通过。

## 业务规则

1. 新签发的 JWT 必须使用 Redis `current-key-id` 对应的私钥签名。
2. JWT header 必须包含正确的 `kid`，与 `current-key-id` 一致。
3. `/.well-known/jwks.json` 继续暴露所有活跃公钥，供资源服务器验证历史 token。
4. 资源服务器验签逻辑不变，继续支持多公钥 + `kid` 匹配。

## 边界与非目标

- 不修改 `AuthServerJwkSupplier` 轮换策略和 Redis 密钥存储格式。
- 不修改 `ResourceServerJwkSupplier`。
- 不处理多节点并发轮换导致 3+ 密钥的竞态问题（独立变更）。

## 验收标准

- [x] OAuth2 授权码换 token 成功，不再抛出 `JwtEncodingException`
- [x] 新 JWT header 的 `kid` 等于 `redis GET in:security:jwk:current-key-id`
- [x] `/.well-known/jwks.json` 返回多把公钥（keys 数组长度 >= 2）
- [x] 未过期旧 token（如有）仍可通过资源服务器验签
