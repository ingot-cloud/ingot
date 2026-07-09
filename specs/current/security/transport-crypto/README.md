# 传输层信封加密与防重放

> 能力域：`security` / `transport-crypto`

## 摘要

框架层提供应用层传输加解密能力：默认采用信封加密（HYBRID，RSA-OAEP-256 包裹每请求临时 CEK + AES-256-GCM 内容加密），支持整体请求/响应、URL 参数与字段级三种粒度；防重放抽象为独立模块 `ingot-security-replay`，供加密与业务幂等复用。未标注的接口与既有 AES/RSA 模式不受影响。

## 边界

- 应用层加密是对 HTTPS 的补充，不可替代 HTTPS。
- 标注 `@InCryptoHybridContext` 的端点必须携带完整 `X-In-Crypto-*` 协议头，缺头返回 `crypto_header_missing`。
- 同一端点不可混用整体模式（`@InDecrypt`/`@InEncrypt`）与字段级模式（`@InDecryptField`/`@InEncryptField`）。
- 防重放依赖 Redis；存储不可用默认 `fail-close`（拒绝请求）。
- 国密算法本期未实现，仅通过 `alg/enc` 头预留扩展。

## 所有者

- 模块：`ingot-framework/ingot-security/ingot-security-crypto`、`ingot-framework/ingot-security/ingot-security-replay`
- 消费侧：BFF、各业务 Provider、网关（公钥路由与放行）

## 关联模块

| 职责 | 路径 |
|---|---|
| 配置属性 | `ingot-security-crypto/.../InCryptoProperties.java` |
| 拦截器（CEK/AAD 上下文） | `ingot-security-crypto/.../web/HybridCryptoInterceptor.java` |
| 请求体解密 Advice | `ingot-security-crypto/.../web/InDecryptRequestBodyAdvice.java` |
| 响应体加密 Advice | `ingot-security-crypto/.../web/InEncryptResponseBodyAdvice.java` |
| URL 参数解密 | `ingot-security-crypto/.../web/InDecryptParamResolver.java` |
| 字段级 Jackson 加解密 | `ingot-security-crypto/.../jackson/CryptoSerializer.java`、`CryptoDeserializer.java` |
| 公钥下发 | `ingot-security-crypto/.../web/HybridPublicKeyController.java` |
| 防重放 | `ingot-security-replay/.../ReplayGuard.java`、`@Idempotent` |
| 模块使用说明 | `ingot-security-crypto/README.md` |

## 文档索引

- [SPEC](./SPEC.md)：协议、配置、注解组合与运行约束
- 前端对接：[frontend-integration.md](../../changes/archive/2026/20260707-security-crypto-hybrid/frontend-integration.md)
- 来源变更：`specs/changes/archive/2026/20260707-security-crypto-hybrid/`
