# 统一分层缓存

> 能力域：`framework` / `layered-cache`

## 摘要

把「L1 Caffeine → 刷新通知 → L2 Redis → Resilient(remote → LKG → Nacos 地板) → Loader」抽象为泛型装饰器链，作为策略、配置、字典等**读多写少、来自远端、故障时不能 fail-open** 的参考数据的默认缓存机制。

已接入消费者：gateway 共享策略快照、登录失败策略、凭证策略、字典客户端、会话并发策略、账号锁定策略、授权快照。

## 边界

- **含**：分层装饰器、LKG、本地地板、跨节点失效协调、版本派生缓存、Actuator 汇总端点。
- **不含**：Spring `@Cacheable` / `InRedisCacheManager`（实体 CRUD）；dict 的 LKG/地板（业务决策，框架已支持但未开启）；统一 Micrometer 指标体系。

## 所有者

- 框架：`ingot-framework/ingot-cache`
- 接入规范：`.agents/skills/layered-cache/`、根 `AGENTS.md`「缓存接入规范」

## 关联模块

| 职责 | 路径 |
|---|---|
| 框架模块 | `ingot-framework/ingot-cache` |
| 网关策略快照 | `ingot-gateway-rule-client` |
| 登录失败策略 | `ingot-security-access-adapter` |
| 凭证策略 | `ingot-security-credential` |
| 字典客户端 | `ingot-dict-client` |
| 会话并发策略 | `ingot-security-authorization-server` |
| 账号锁定策略 | `ingot-security-account-adapter` |
| 授权快照 | `ingot-data-mybatis-scope` |

## 文档索引

- [SPEC](./SPEC.md)：分层顺序、SPI、语义不变量、消费者 Redis key 与配置键
- 模块说明：`ingot-framework/ingot-cache/README.md`
- 来源变更：`specs/changes/archive/2026/20260730-framework-layered-cache/`；授权快照接入见 `specs/changes/archive/2026/20260910-pms-rbac-data-authorization/`
