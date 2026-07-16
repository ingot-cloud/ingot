# 固定 HYBRID 协议头与协议版本枚举

> 状态：completed（已归档）

## 元数据

- Change ID：`20260716-security-crypto-protocol-headers`
- 领域：`security`
- 负责人：jy
- 创建日期：2026-07-16
- 目标发布日期：2026-07-16

## 目标

将 HYBRID 信封加密的七个协议头名称从可配置属性改为模块内常量（去掉 `X-` 前缀，统一为 `In-Crypto-*`），并将 `modeValue` 从配置项改为协议版本枚举；缺协议头时保持 fail-close。

## 范围

**包含：**

- 新增 `HybridHeaders`、`HybridProtocolVersion`
- 精简 `InCryptoProperties.Hybrid`（删除 `headers`、`modeValue`）
- 更新 `HybridCryptoInterceptor` 与相关 Javadoc
- 单测、模块 README、`specs/current/security/transport-crypto/`

**不包含：**

- 可选加密 / fail-open 模式
- 协议版本 H2 及多版本分支逻辑
- 修改 `specs/changes/archive/` 历史工件

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：2026-07-16
- 验收：已通过（协议头常量化、版本枚举、fail-close 行为）
- 关联提交或 PR：（待提交）
- 更新的 current capability：`specs/current/security/transport-crypto/`
- 与原设计的差异：无
- 取消原因：—
