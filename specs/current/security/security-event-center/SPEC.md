# 统一安全事件中心 SPEC

> 记录当前已验收并在线生效的系统事实（post recording pipeline）。

## 1. 配置（`ingot.security.event`）

各服务独立 Nacos dataId；recording 装配后以 `SecurityEventRecordingProperties` 解析。

### 1.1 拓扑

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 总开关 |
| `target` | （空） | `local` \| `center`；空则映射 legacy `mode` |
| `shadow-targets` | `[]` | 迁移 shadow；稳定态必须为空 |
| `primary-store` | （空） | 多 Store 时必填 `mysql` |
| `source-module` | `unknown` | 写入 `source_module` |

**Legacy 映射（兼容一个发布周期）**：

| enabled | legacy mode | effective |
|---|---|---|
| true | `local` | target=local |
| true | `remote` | target=center + shadow=local + 继续写 `account_security_event` |

**显式 target 切换（稳定态）**：

```yaml
ingot:
  security:
    event:
      enabled: true
      target: center   # 或 local
      shadow-targets: []
      source-module: ingot-pms
      primary-store: mysql
```

### 1.2 投递与 spool

| 前缀 | 说明 |
|---|---|
| `delivery.memory.*` | BEST_EFFORT 队列（默认 capacity 2048、batch 32） |
| `delivery.spool.*` | DURABLE spool 目录、配额、ack 超时 20ms |
| `priority-overrides` | 按 eventType 覆盖优先级 |

旧 `async.*` 映射到 `delivery.memory.*`。

### 1.3 Retention

| 键 | 默认 | 说明 |
|---|---|---|
| `retention.enabled` | `true` | |
| `retention.days` | `30` | `0`=永久 |
| `retention.batch-size` | `500` | |
| `retention.max-rounds` | `100` | |
| `retention.max-duration-seconds` | `30` | |
| `retention.yield-queue-usage-percent` | `50` | 写入积压让步 |

| 表 | 任务 | cron |
|---|---|---|
| canonical `security_event` | `PurgeCanonicalSecurityEventTask` | `0 30 3 * * ?` |
| legacy `account_security_event` | `PurgeAccountSecurityEventTask` | `0 0 3 * * ?`（切换后仅清理历史） |

## 2. 上报链路

### 2.1 账号域

```text
UseCase → SecurityEventPort (CompositeSecurityEventPort)
            ├─ [legacy mode=remote] DefaultSecurityEventPortAdapter → account_security_event
            └─ SecurityEventPublisher → dispatcher → Store/Transport (+ shadow)
```

### 2.2 网关 ACCESS

```text
BlacklistEventReporter → SecurityEventReportPublisher → target=center → 中心 admission
```

### 2.3 中心 ingest

```text
InnerSecurityEventAPI → SecurityEventAdmissionService → enqueue → async MySqlSecurityEventStore
```

Feign：`RemoteSecurityEventService`；DTO 含 `eventId`、`priority`（可选）。

## 3. 数据模型

中心与 PMS/Member 各自库：canonical `security_event`（migration `012`）。

核心列：`event_id`(UK)、`priority`、`occurred_at`、`received_at`、`source_module`、`extension` JSON。

## 4. 失败与降级

| 场景 | 行为 |
|---|---|
| 事件失败 | 不回滚业务事务 |
| BEST_EFFORT 队列满 | DROPPED + 指标 |
| DURABLE spool 满 | FAILED + 告警；中心返回 503 |
| shadow 失败 | 主链路 ack；`shadowFailures` 计数 |
| Feign/中心不可用 | durable 本地 spool 重放 |

## 5. 观测

- `GET /actuator/securityrecording`：target、shadow、队列深度、计数器
- Micrometer：`ingot.security.event.*`
- Shadow 对账脚本：`databases/scripts/security_event_shadow_reconcile.sql`

## 6. 已知限制

- 无 Platform 读侧 API（Repository 已交付）。
- ES/Kafka Store/Transport 未实现。
- 旧 `AsyncSecurityEventReporter` 类保留，运行路径已迁移；后续 breaking change 删除。

## 7. 迁移与回滚

1. 执行 migration `012`
2. 部署 recording + store + transport 模块
3. shadow 对账窗口（legacy `mode=remote` 或显式 shadow）
4. 切换显式 `target` + 清空 `shadow-targets`
5. 回滚：恢复 shadow 或 legacy mode；禁止删表；以 `eventId` 对账
