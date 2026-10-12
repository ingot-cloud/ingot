# 安全配置落点（Nacos 治理）

> 能力域：`security` / `config-governance`

## 摘要

`ingot.security.*` 安全规则按四层落到固定 Nacos dataId，面向用户的安全提示集中到独立 `in-security-messages.yml`；共享文件只给真正的消费者 import。查找靠本能力配置地图，不靠每个服务挂大全。

操作口诀：**改阈值去共享策略文件；改本进程开不开启去 `in-service-*.yml`；改事件上报到哪也去服务 yml；改安全提示去 `in-security-messages.yml`。**

## 边界

- **含**：Nacos dataId 分配、前缀 → 消费者矩阵、`account.*` 前缀收口后的键名。
- **不含**：各域策略语义（见 account / access / credential / session / event / challenge 各 SPEC）；安全中心 remote 管理面。

## 所有者

- Nacos 真源：仓库 `nacos/{DEV,TEST,PROD}_GROUP/`
- 绑定类：各 `ingot.security.*` `@ConfigurationProperties`

## 文档索引

- [SPEC](./SPEC.md)：分层、import 矩阵、前缀地图
- 来源变更：`specs/changes/archive/2026/20260825-security-config-governance/`
- [独立安全文案配置](../../../changes/archive/2026/20261011-security-credential-expiry-messages/README.md)（2026-10-12 用户确认验收）
