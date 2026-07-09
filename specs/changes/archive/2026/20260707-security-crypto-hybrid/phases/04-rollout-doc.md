# Phase 04 · 前端文档与基线

> 状态：completed

## 目标

完成前端交付文档、示例接口联调，并在验收后更新 current 基线。

## 实现要点

- 前端对接说明（[frontend-integration.md](../frontend-integration.md)）已交付。
- BFF 登录等敏感接口 HYBRID 联调通过。
- 移除 `ingot.crypto.hybrid.mode`（optional/strict），标注端点统一要求协议头。
- 已创建 `specs/current/security/transport-crypto/`（README + SPEC）。

## 退出条件

- [x] 前端文档评审通过、示例接口联调成功。
- [x] current 基线已更新，change 记录完成信息并归档。

## 回滚

- 移除 `@InCryptoHybridContext` 注解即恢复明文；不涉及数据变更。
