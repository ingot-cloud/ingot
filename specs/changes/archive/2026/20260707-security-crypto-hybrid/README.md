# 传输安全信封加密（HYBRID）与通用防重放

> 状态：completed

## 实施策略

在现有 `ingot-security-crypto` 模块内新增**信封加密（HYBRID）**模式，取代前端长期保存固定对称密钥的做法；同时把**防重放**能力抽象为独立通用模块 `ingot-security-replay`。变更向后兼容既有 `AES/AES_GCM/RSA` 模式与未标注接口。

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260707-security-crypto-hybrid` |
| 领域 | `security` |
| 创建日期 | 2026-07-07 |
| 完成日期 | 2026-07-09 |
| 关联模块 | `ingot-security-crypto`、`ingot-security-replay`、`ingot-commons`（AESUtil）、`ingot-bff`（示例接入） |
| 目标 Current | `specs/current/security/transport-crypto/`（已创建） |

## 目标

消除前端长期持有固定对称密钥的安全风险；提供统一、可配置、可轮换的密钥管理与防篡改/防重放保障，并沉淀可复用的防重放通用能力。

## 范围

### 包含

- `CryptoType.HYBRID`、注解体系（`@InCryptoHybridContext`、`@InDecrypt`、`@InEncrypt`、`@InDecryptField`、`@InEncryptField`）
- 隐晦协议头 `X-In-Crypto-*`、三种加解密粒度（整体 / `DATA_ONLY` / 字段级 / URL 参数）
- `HybridKeyManager`、公钥端点 `GET /crypto/public-keys`
- `ingot-security-replay`：`ReplayGuard`、`@Idempotent`
- 前端对接说明文档

### 不包含

- 前端加解密实现
- 国密 `SM2/SM4`
- `@Idempotent` 强幂等"返回上次结果"语义

## 工件

| 文档 | 内容 |
|---|---|
| [需求](./REQUIREMENTS.md) | 用户场景、业务规则、验收标准 |
| [设计](./DESIGN.md) | 架构、协议、数据流 |
| [任务](./TASKS.md) | 分阶段任务与状态 |
| [前端对接](./frontend-integration.md) | 交付前端的协议说明 |

## 完成记录

- 完成日期：2026-07-09
- 关联提交或 PR：工作区本地变更（待提交）
- 更新的 current capability：`specs/current/security/transport-crypto/`
- 与原设计的差异：
  - 删除 `CryptoDTO` 包装类，密文提取统一 `Map` + `bodyKey`
  - 删除 `ingot.crypto.hybrid.mode`（原 optional/strict 灰度配置）；标注 `@InCryptoHybridContext` 的端点统一要求完整协议头
  - 删除组合注解 `InCryptoHybrid`、`InCryptoHybridField`、`InCryptoAES`、`InCryptoRSA`（实施中已收敛为单一职责注解）
