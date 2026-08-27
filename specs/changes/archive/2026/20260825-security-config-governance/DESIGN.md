# Design

## 方案摘要

配置按四层分配，查找靠配置地图（前缀 → dataId → 消费者）：

| 层 | 内容 | dataId |
|---|---|---|
| L0 密钥 | `crypto`、Auth `jwk.master-key` | `in-security-crypto.yml` / `in-service-auth.yml` |
| L1 共享策略 | `credential`、`replay`、`account.signal`、PMS/Member/Security 的 `event.delivery`/`mysql` | `in-security-policy.yml` |
| L1 网关地板 | `ratelimit`/`blacklist`/`violation-escalation` 的 groups/rules/items 数值 | `in-security-gateway.yml`（仅 Gateway import） |
| L2 执行面 | `enabled`/`mode`/路径/`account.bff`/`account.gateway`/`access`/`session` | 对应 `in-service-*.yml` |
| L3 投递 | `event.target`/`source-module`/`categories`/`retention` | 对应 `in-service-*.yml` |

**import 规则**：会执行该能力才挂对应 dataId。Auth 不挂 `in-security-policy.yml`（不跑凭证策略引擎）。BFF 挂 policy 是因为要读 `replay` 与 `account.signal`，会多看到未执行的 `credential` 段。

`spring.config.import` 顺序：common → database（若有） → crypto（若消费） → policy 或 gateway-policy（若消费） → `${spring.application.name}.yml`（最后覆盖）。

## 数据模型与接口

无数据库变更。公共配置契约：

| 现前缀 | 新前缀 | 绑定类 |
|---|---|---|
| `ingot.security.account-lock-signal` | `ingot.security.account.signal` | `AccountLockSignalProperties` |
| `ingot.security.account-lock-bff` | `ingot.security.account.bff` | `AccountLockBffProperties` |
| `ingot.security.account-lock-gateway` | `ingot.security.account.gateway` | `AccountLockGatewayProperties` |

三类仍独立 `@ConfigurationProperties`。`AccountDomainProperties` 前缀保持 `ingot.security.account`，未知子节点走默认 `ignoreUnknownFields`。

新增常量 `NacosConstants.IN_SECURITY_GATEWAY = "in-security-gateway.yml"`。

## 数据流与失败处理

Nacos 热刷新机制不变：`@ConfigurationProperties` + rebinder。Gateway local 限流改 `in-security-gateway.yml` 后仍走 `EnvironmentChangeEvent` → `LocalPolicyEnvironmentRefreshListener`（按属性前缀，不按 dataId）。

`account.lockout` 仍按进程一份：PMS / Member 各自服务 yml。硬塞进共享文件会把 B/C 锁成同一套。

## 迁移与回滚

- 代码与三环境 `nacos/` **同版本发布**，一次性改键，不双写旧前缀。
- 回滚：回退该提交，并把 Nacos 控制台（若已手工同步）一并回退。
- 不改 `ingot_nacos_config.sql` 历史 dump。

## 测试策略

- 编译期：prefix 字符串全库检索无旧键读取。
- 单测：BFF/Gateway 锁定短路单测构造 Properties 对象，不依赖 Nacos 键名。
- 验收：对照搬家前后 yaml 数值；BFF import policy 后 `account.signal` / `replay` 可绑定。
