# 统一安全事件中心 SPEC

> 记录当前已验收并在线生效的系统事实（post legacy cleanup）。

## 1. 配置（`ingot.security.event`）

各服务独立 Nacos dataId。**唯一绑定类**：`com.ingot.framework.security.recording.config.SecurityEventProperties`（`ingot-security-recording` 模块）。完整样例见 [example.yml](../../../ingot-framework/ingot-security/ingot-security-recording/example.yml)。

### 1.1 拓扑

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 总开关；`false` 时不做任何上报 |
| `target` | `local` | `local` 写本库 `security_event`；`center` Feign 上报中心 |
| `shadow-targets` | `[]` | 次要投递；稳定态必须为空 |
| `primary-store` | （空） | `target=local` 时填 `mysql` |
| `source-module` | `unknown` | 写入 `security_event.source_module` |

### 1.2 类别过滤（`categories.*`）

| 键 | 默认 | 说明 |
|---|---|---|
| `auth` | `true` | 登录成功/失败（AUTH） |
| `account` | `true` | 锁定/解锁/建删账号（ACCOUNT） |
| `credential` | `true` | 改密/重置（CREDENTIAL） |
| `access` | `true` | 网关封禁/限流（ACCESS）；Gateway 通常仅开此项 |

### 1.3 投递与 spool（`delivery.*`）

| 键 | 默认 | 说明 |
|---|---|---|
| `delivery.memory.queue-capacity` | `2048` | BEST_EFFORT 有界队列 |
| `delivery.memory.batch-size` | `32` | dispatcher 攒批上限 |
| `delivery.memory.poll-timeout-ms` | `100` | poll 超时 |
| `delivery.memory.shutdown-timeout-ms` | `5000` | 关闭排空超时 |
| `delivery.spool.directory` | `./logs/security-recording/spool` | DURABLE spool 目录 |
| `delivery.spool.max-bytes` | `1GB` | spool 总配额 |
| `delivery.spool.durable-ack-timeout-ms` | `20` | producer 等待 spool 接纳超时 |
| `priority-overrides` | `{}` | eventType → BEST_EFFORT \| DURABLE |

### 1.4 MySQL Store（`mysql.*`）

| 键 | 默认 | 说明 |
|---|---|---|
| `mysql.max-concurrent-writes` | `1` | 写入信号量 |
| `mysql.transaction-timeout-seconds` | `5` | 批量 INSERT 事务超时 |

### 1.5 Retention（`retention.*`）

| 键 | 默认 | 说明 |
|---|---|---|
| `retention.enabled` | `true` | 是否启用定时清理 |
| `retention.days` | `30` | 保留天数；`0`=永久 |
| `retention.batch-size` | `500` | 单批 DELETE 条数 |
| `retention.max-rounds` | `100` | 单次任务最大批次数 |
| `retention.max-duration-seconds` | `30` | 单次任务时间预算 |
| `retention.yield-queue-usage-percent` | `50` | 队列积压让步阈值 |

| 表 | 任务 | cron |
|---|---|---|
| canonical `security_event` | `PurgeCanonicalSecurityEventTask` | `0 30 3 * * ?` |

> legacy `account_security_event` 已由 migration `013` 物理删除；无独立 retention 任务。

### 1.6 各服务推荐

| 服务 | `target` | `primary-store` | `source-module` |
|---|---|---|---|
| PMS | `local` | `mysql` | `PMS` |
| Member | `local` | `mysql` | `MEMBER` |
| Gateway | `center` | — | `GATEWAY` |
| Security | `local` | `mysql` | `SECURITY` |

**推荐配置（PMS/Member/Security）**：

```yaml
ingot:
  security:
    event:
      enabled: true
      target: local
      shadow-targets: []
      source-module: PMS
      primary-store: mysql
```

## 2. 上报链路

### 2.1 账号域

```text
UseCase → SecurityEventPort (CompositeSecurityEventPort)
            → SecurityEventPublisher → dispatcher → MySqlSecurityEventStore (target=local)
```

### 2.2 网关 ACCESS

```text
BlacklistEventReporter → SecurityEventReportPublisher → target=center → 中心 admission
```

### 2.3 中心 ingest

```text
InnerSecurityEventAPI / InnerSecurityPolicyAPI.reportBlacklist
  → SecurityEventAdmissionService → enqueue → async MySqlSecurityEventStore
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

## 6. 已知限制

- 无 Platform 读侧 API（Repository 已交付）。
- ES/Kafka Store/Transport 未实现。

## 7. 迁移与回滚

1. 执行 migration `012`（canonical `security_event`）
2. 执行 migration `013`（DROP legacy `account_security_event`；可选事前 dump）
3. 部署 recording + store + transport 模块
4. Nacos 使用 `target`（禁止旧 `mode`）
5. 回滚 recording：改 `target` 或 `enabled=false`；以 `eventId` 对账
6. 回滚 `013`：`rollback_013` 仅重建空旧表，不恢复数据

## 8. 来源变更

- `specs/changes/archive/2026/20260804-security-event-storage-pipeline/`
- `specs/changes/archive/2026/20260806-security-event-legacy-cleanup/`（已验收归档）
- `specs/changes/archive/2026/20260811-security-drop-account-security-event/`（legacy 表 DROP）
