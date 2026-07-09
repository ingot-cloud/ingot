# Tasks

> 状态：completed（已验收，current 基线已更新，待归档）

## 阶段总览

| 阶段 | 内容 | 状态 |
|---|---|---|
| P1 | 新建 `ingot-security-replay` 通用防重放模块 + `@Idempotent` | 已完成 |
| P2 | crypto 基础设施（类型/工具/密钥/服务/配置/错误码） | 已完成 |
| P3 | Advice 集成、触发协议、R 结构响应、公钥端点与网关路由 | 已完成 |
| P4 | 联调、前端对接文档、current 基线 | 已完成 |

## 实施任务

### P1 通用防重放模块（[phases/01](./phases/01-replay-module.md)）· 已完成

- [x] T1.1：创建模块 `ingot-framework/ingot-security/ingot-security-replay`
- [x] T1.2：`NonceStore` + `RedisNonceStore`
- [x] T1.3：`ReplayGuard`/`DefaultReplayGuard` + `ReplayProperties` + `ReplayErrorCode`
- [x] T1.4：`@Idempotent` + `IdempotentAspect`
- [x] T1.5：`ReplayAutoConfiguration` + AutoConfiguration.imports 注册

### P2 crypto 基础设施（[phases/02](./phases/02-crypto-core.md)）· 已完成

- [x] T2.1：`AESUtil` byte[] CEK + AAD GCM 重载
- [x] T2.2：`CryptoType.HYBRID` + 字段级 `@InDecryptField`/`@InEncryptField`
- [x] T2.3：`InCryptoProperties.hybrid`（responseWrap/keyPairs/headers/modeValue/replayNamespace）
- [x] T2.4：`HybridKeyManager`（多 kid、RefreshScope 热刷新）
- [x] T2.5：`HybridCryptoService`
- [x] T2.6：`CryptoErrorCode` + crypto 依赖 replay 模块

### P3 集成与端点（[phases/03](./phases/03-web-integration.md)）· 已完成

- [x] T3.1：`HybridCryptoInterceptor` + `InDecryptRequestBodyAdvice` HYBRID 分支
- [x] T3.2：`InEncryptResponseBodyAdvice` HYBRID 分支（`DATA_ONLY`/`FULL`）
- [x] T3.3：`HybridPublicKeyController` + 条件注册
- [x] T3.5：字段级 HYBRID + `GlobalExceptionHandlerResolver` 异常还原
- [x] T3.8：URL 参数 HYBRID 解密 + 单测
- [x] T3.4：网关路由 `/crypto/public-keys` 与放行白名单（部署侧已联调验证）

### P4 灰度与交付（[phases/04](./phases/04-rollout-doc.md)）· 已完成

- [x] T4.1：前端对接说明文档（[frontend-integration.md](./frontend-integration.md)）
- [x] T4.2：BFF 登录等敏感接口 HYBRID 联调通过
- [x] T4.3：移除 `hybrid.mode`（optional/strict），标注端点统一要求协议头
- [x] T4.4：创建 `specs/current/security/transport-crypto/`

## 验证任务

- [x] V1：单元测试（replay、AESUtil、HybridCryptoService、字段级、URL 参数）通过
- [x] V2：集成测试（端到端 POST/GET、多 kid、篡改/重放、公钥端点）通过
- [x] V3：兼容回归（旧模式与未标记接口）通过

## 完成检查

- [x] 实现与 DESIGN 一致（已移除 `hybrid.mode`，见完成记录）
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新：`specs/current/security/transport-crypto/`
- [x] Change 已记录完成信息并归档

## 已定默认决策

- 公钥端点由框架提供，部署在对外公开的认证/BFF 服务，网关统一路由；密钥集经配置中心全局共享。
- RSA 密钥长度默认 2048；CEK 为 AES-256；GCM IV 12 字节、tag 128 位。
- `responseWrap` 默认 `DATA_ONLY`，时间窗默认 ±5 分钟。
- 标注 `@InCryptoHybridContext` 的端点必须携带协议头（已删除 optional/strict 配置）。
- Redis 不可用默认 `fail-close`。
