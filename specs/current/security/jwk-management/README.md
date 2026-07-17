# JWK 密钥管理与 JWT 签名

> 能力域：`security` / `jwk-management`

## 摘要

授权服务器使用 Redis 存储 RSA JWK，支持密钥轮换与多密钥并存；资源服务器从 Redis 加载公钥验证 JWT。密钥轮换后，签发端通过 `JwkSelector` 固定选择 `current-key-id` 对应私钥，验签端通过 `kid` 匹配多把公钥。

## 边界

- 授权服务器持有私钥并负责签发；资源服务器只读公钥。
- Redis 私钥可经 `AUTH_JWK_MASTER_KEY` AES-256-GCM 加密存储。
- 多活跃密钥是正常状态（当前密钥 + 宽限期内历史密钥），不是异常。
- 集群跨节点 `rotateKey()` 竞态（可能产生 3+ 密钥）本期未治理。

## 所有者

- 模块：`ingot-security-common`、`ingot-security-authorization-server`、`ingot-auth`
- 配置文档：`docs/modules/authorization-server/JWK-CONFIGURATION.md`

## 关联模块

| 职责 | 路径 |
|---|---|
| 授权服务器密钥供应（私钥、轮换） | `ingot-security-common/.../AuthServerJwkSupplier.java` |
| 资源服务器密钥供应（公钥、缓存） | `ingot-security-common/.../ResourceServerJwkSupplier.java` |
| 授权服务器 JWK/JwtEncoder 装配 | `ingot-auth/.../AuthServerJwkConfiguration.java` |
| JwtEncoder 签名选择器 | `ingot-security-authorization-server/.../AuthServerJwtEncoderConfiguration.java` |
| JWKS 端点 | `ingot-auth/.../JwksEndpoint.java` |
| 资源服务器 JwtDecoder | `ingot-security-common/.../CustomOAuth2ResourceServerJwtConfiguration.java` |

## 文档索引

- [SPEC](./SPEC.md)：Redis 键、轮换规则、签名/验签数据流
- 运维配置：[JWK-CONFIGURATION.md](../../../../docs/modules/authorization-server/JWK-CONFIGURATION.md)
- 来源变更：`specs/changes/archive/2026/20260713-security-jwk-signing-selector/`
