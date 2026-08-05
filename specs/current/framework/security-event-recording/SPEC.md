# 安全事件 Recording SPEC

> 记录当前已验收并在线生效的框架事实。

## 1. SPI 分层

```text
SecurityEventPublisher (业务唯一写入口)
  → SecurityEventRecordingDispatcher (分级队列 + 攒批)
       target=local  → SecurityEventStore.appendBatch
       target=center → SecurityEventTransport.deliverBatch
       shadow-targets → 次要投递（迁移期）
```

- **不可违反**：Resilient/LKG 在 Store 层之下；不缓存空值；DURABLE 必须经 file spool 接纳后再 ack。
- **编译产物不进 L2**；衍生缓存用 `VersionedDerivedCache`（本 change 未涉及策略缓存）。

## 2. 优先级默认

| 级别 | 事件类型（节选） |
|---|---|
| BEST_EFFORT | LOGIN_SUCCESS、LOGIN_FAILURE、RATE_LIMIT_VIOLATION |
| DURABLE | ACCOUNT/CREDENTIAL 变更、BLACKLIST_BLOCK、LOGIN_FAIL_*_EXCEED |

## 3. 投递语义

- 业务线程：**fail-open**；`PublishOutcome` 表示同步接纳，不表示最终持久化。
- 事务内事件：**afterCommit** 入队。
- BEST_EFFORT：内存队列满 → `DROPPED`。
- DURABLE：spool 拒绝/超时 → `FAILED`（业务仍放行）。
- at-least-once + Store `event_id` 幂等。

## 4. MySQL Store

- 表：canonical `security_event`（`event_id` UNIQUE、`priority`、`received_at` 索引）。
- 写入：`max-concurrent-writes=1` semaphore + 真批量 INSERT。
- 查询：`SecurityEventQueryRepository` 游标 `(receivedAt,id)` 降序；默认 24h、最大 31 天；page 默认 50、最大 200。
- Retention：命名锁、分批删除、30s 时间预算、队列积压 50% 让步；任务 `PurgeCanonicalSecurityEventTask` cron `0 30 3 * * ?`。

## 5. 中心 admission

Feign/HTTP 上报 → 校验 → `SecurityEventEnqueue` → 异步 Store；HTTP 线程不持有 Store 事务。

响应码：`SEC_EVENT_503`（DURABLE 可重试）、`SEC_EVENT_429`（BEST_EFFORT 拒绝）。

## 6. 稳定态切换

显式配置 `target=local|center` 且 `shadow-targets: []` 后：

- 仅权威 Store 增长；
- legacy `account_security_event` 停止新写入（仅当不再使用 legacy `mode=remote` 映射时）；
- 旧表保留只读 + 独立 retention。

## 7. 后续清理（独立 change）

- 删除 `AsyncSecurityEventReporter`、`RemoteSecurityEventPortAdapter`、legacy `mode` 兼容层；
- 旧表 `account_security_event` 破坏性下线（需离线对账完成后）。
