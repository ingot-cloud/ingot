# Design

## 方案摘要

在 `ingot-security-authorization-server` 新增 `AuthServerJwtEncoderConfiguration`，当存在 `authServerJwkSupplier` bean 时，注册带 `JwkSelector` 的 `JwtEncoder` bean。

`OAuth2ConfigurerUtils.getJwtEncoder()` 优先使用容器中的 `JwtEncoder` bean，从而替代无 selector 的默认 `NimbusJwtEncoder`。

签名与验签职责分离：

| 路径 | 组件 | 密钥数量 | 选择策略 |
|------|------|---------|---------|
| 签发 | `NimbusJwtEncoder` + `JwkSelector` | 1（当前私钥） | `getCurrentSigningKey()` |
| 验签 | `JWSVerificationKeySelector` | N（所有公钥） | JWT header `kid` 匹配 |
| JWKS | `JwksEndpoint` | N（所有公钥） | 全部暴露 |

## 数据模型与接口

- 无数据库变更
- 无公共 API 变更
- Redis JWK 存储结构不变

## 数据流与失败处理

```
Token 请求 → JwtGenerator → JwtEncoder (带 selector)
  → getCurrentSigningKey() → 单把 RSA 私钥 → 签名成功
```

若 `current-key-id` 缺失或密钥加载失败，`getCurrentSigningKey()` 沿用现有逻辑生成新密钥或抛出异常。

## 迁移与回滚

- **上线**：部署含新配置的 ingot-auth 即可，无需 Redis 操作
- **回滚**：移除 `AuthServerJwtEncoderConfiguration` 或回退版本；多密钥场景下签发仍会失败

## 实施偏差

- 类级别 `@ConditionalOnBean(AuthServerJwkSupplier.class)` 在 `@Import` 阶段评估过早，改为方法级别 `@ConditionalOnBean(name = "authServerJwkSupplier")`
- `authServerJwkSupplier` bean 返回类型改为 `AuthServerJwkSupplier`

## 测试策略

- 编译验证：`ingot-security-authorization-server`、`ingot-auth` 模块
- 手动验收：多密钥 Redis 环境下 OAuth2 token 签发、JWKS 端点、kid 校验（2026-07-14 通过）
