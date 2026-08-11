# 安全事件 Recording 框架

> 能力域：`framework` / `security-event-recording`

## 摘要

统一安全事件 **发布、分级队列、file spool、Transport 与 Store** 的运行时框架。业务侧只依赖 `SecurityEventPublisher`；存储与传输由 module 依赖与 `target` 配置决定。

## 模块

| 模块 | 职责 |
|---|---|
| `ingot-security-recording` | SPI、dispatcher、优先级、file spool、配置、Actuator |
| `ingot-security-event-store-mysql` | canonical `security_event` 批量写入、游标查询、retention |
| `ingot-security-event-store-log` | JSONL segment Store（无查询） |
| `ingot-security-event-transport-feign` | Feign Transport + DTO 映射 |

## 配置

**前缀**：`ingot.security.event.*`

**唯一绑定类**：`com.ingot.framework.security.recording.config.SecurityEventProperties`（`ingot-security-recording` 模块）

各服务 Nacos dataId 独立生效。完整字段说明与三场景样例见 [example.yml](../../../ingot-framework/ingot-security/ingot-security-recording/example.yml)。

| 键 | 说明 |
|---|---|
| `enabled` | 总开关；`false` → `DisabledSecurityEventPublisher` |
| `target` | `local` \| `center`（默认 `local`） |
| `primary-store` | `target=local` 时必填，典型 `mysql` |
| `shadow-targets` | 迁移 shadow；稳定态 `[]` |
| `source-module` | 写入 `security_event.source_module` |
| `categories.*` | 按 AUTH/ACCOUNT/CREDENTIAL/ACCESS 过滤 |
| `delivery.memory.*` | BEST_EFFORT 有界队列 |
| `delivery.spool.*` | DURABLE file spool |
| `mysql.*` | Store 写入并发与事务 |
| `retention.*` | canonical 表过期清理 |
| `priority-overrides` | eventType → BEST_EFFORT \| DURABLE |

### 各服务推荐值

| 服务 | `target` | `primary-store` | 说明 |
|---|---|---|---|
| PMS / Member | `local` | `mysql` | 写业务库 `security_event` |
| Gateway | `center` | — | ACCESS 事件 Feign 上报中心 |
| Security 中心 | `local` | `mysql` | 中心 admission 异步入库 |

## 观测

- Actuator：`GET /actuator/securityrecording`
- Micrometer：`ingot.security.event.*` gauges（published/accepted/persisted/dropped/failed/replayed/duplicate/shadow_failures）

## 来源变更

- `specs/changes/archive/2026/20260804-security-event-storage-pipeline/`
- 配置类合并：`specs/changes/active/20260806-security-event-legacy-cleanup/`
