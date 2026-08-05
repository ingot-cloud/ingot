# 安全事件 Recording 框架

> 能力域：`framework` / `security-event-recording`

## 摘要

统一安全事件 **发布、分级队列、file spool、Transport 与 Store** 的运行时框架。业务侧只依赖 `SecurityEventPublisher`；存储与传输由 module 依赖与 `target` 配置决定。

## 模块

| 模块 | 职责 |
|---|---|
| `ingot-security-recording` | SPI、dispatcher、优先级、file spool、配置兼容、Actuator |
| `ingot-security-event-store-mysql` | canonical `security_event` 批量写入、游标查询、retention |
| `ingot-security-event-store-log` | JSONL segment Store（无查询） |
| `ingot-security-event-transport-feign` | Feign Transport + DTO 映射 |

## 配置前缀

`ingot.security.event.*`（与 `SecurityEventProperties` 共用前缀；recording 装配后以 `SecurityEventRecordingProperties` 为准）。

关键项：

| 键 | 说明 |
|---|---|
| `enabled` | 总开关；`false` → `DisabledSecurityEventPublisher` |
| `target` | `local` \| `center`；未设时由 legacy `mode` 映射 |
| `shadow-targets` | 迁移期 shadow，稳定态必须为空 |
| `primary-store` | 多 Store 时必选（如 `mysql`） |
| `delivery.memory.*` | BEST_EFFORT 有界队列 |
| `delivery.spool.*` | DURABLE file spool |
| `priority-overrides` | eventType → BEST_EFFORT \| DURABLE（可热刷新） |

Legacy 映射（一个兼容周期）：

| 旧 `mode` | effective |
|---|---|
| `local` | `target=local` |
| `remote` | `target=center` + `shadow-targets=[local]` |

## 观测

- Actuator：`GET /actuator/securityrecording`
- Micrometer：`ingot.security.event.*` gauges（published/accepted/persisted/dropped/failed/replayed/duplicate/shadow_failures）

## 来源变更

- `specs/changes/archive/2026/20260804-security-event-storage-pipeline/`
