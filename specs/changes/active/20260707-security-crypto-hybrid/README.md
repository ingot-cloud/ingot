# 传输安全信封加密（HYBRID）与通用防重放

> 状态：approved

## 实施策略

在现有 `ingot-security-crypto` 模块内新增**信封加密（HYBRID，类 JWE：`RSA-OAEP-256` 包裹每请求临时 CEK + `AES-256-GCM` 内容加密）**模式，取代前端长期保存固定对称密钥的做法；同时把**防重放**能力抽象为独立通用模块 `ingot-security-replay`（含 `@Idempotent` 注解），供加密、验签与业务幂等复用。变更向后兼容既有 `AES/AES_GCM/RSA` 模式，采用 `optional -> strict` 灰度切换，逐步实施。

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260707-security-crypto-hybrid` |
| 领域 | `security` |
| 负责人 | TBD |
| 创建日期 | 2026-07-07 |
| 目标发布日期 | TBD |
| 关联模块 | `ingot-framework/ingot-security/ingot-security-crypto`、新增 `ingot-framework/ingot-security/ingot-security-replay`、`ingot-framework/ingot-commons`（AESUtil）、`ingot-service/ingot-gateway`（公钥端点路由与放行） |
| 目标 Current | `specs/current/security/transport-crypto/`（验收后创建/更新） |

## 目标

消除前端长期持有固定对称密钥的安全风险：前端仅持有服务端公钥（可公开），每请求随机生成 CEK 并用公钥包裹传输，响应复用同一 CEK。适配纯浏览器 Web、移动 H5、企业微信 H5、微信小程序及未来 APP。提供统一、可配置、可轮换的密钥管理与防篡改/防重放保障，并沉淀可复用的防重放通用能力。

## 范围

### 包含

- 新增 `CryptoType.HYBRID` 与组合注解 `@InCryptoHybrid`，改造请求/响应 Advice 支持信封加密。
- 隐晦协议头 `X-In-Crypto-*`（可配置）、触发协议（注解 + 头，`optional`/`strict` 模式）。
- 响应默认保留 `R` 结构（仅加密 `data`）、CEK 复用、GET 支持。
- `HybridKeyManager`（多 kid 密钥、轮换）与公钥下发端点 `GET /crypto/public-keys`，网关统一路由与放行。
- AEAD AAD 绑定 nonce/ts/kid 防篡改。
- 新增独立模块 `ingot-security-replay`：`NonceStore`/`RedisNonceStore`、`ReplayGuard`、`ReplayErrorCode`、`@Idempotent` + 切面、自动配置。
- `AESUtil` 新增 `byte[]` 原始密钥 + AAD 的 GCM 重载。
- 前端对接说明文档（交付前端实现，本次不实现前端）。

### 不包含

- 前端加解密实现。
- 国密 `SM2/SM4`（仅通过 `alg/enc` 协商预留扩展）。
- 字段级 `@InFieldEncrypt/@InFieldDecrypt` 接入 HYBRID。
- `@Idempotent` 的"返回上次结果"强幂等语义（本期仅"拒绝重复"）。

## 工件

| 文档 | 内容 |
|---|---|
| [需求](./REQUIREMENTS.md) | 用户场景、业务规则、边界与验收标准 |
| [设计](./DESIGN.md) | 架构、协议、密钥管理、防重放、数据流、兼容与测试 |
| [任务](./TASKS.md) | 分阶段实施任务、依赖与验收 |
| [阶段](./phases/) | 分阶段实现、退出条件与回滚 |
| [前端对接](./frontend-integration.md) | 交付前端的协议/报文/加解密/错误处理说明 |

## 完成记录

- 完成日期：
- 关联提交或 PR：
- 更新的 current capability：
- 与原设计的差异：
- 取消原因：
