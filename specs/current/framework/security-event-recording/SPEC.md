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

## 3. 生产者触发语义

优先级表（§2）只决定投递可靠性，**不**决定是否重复 publish。生产者侧：

| 语义 | 适用 | 规则 |
|---|---|---|
| 状态变更 **边沿** | `ACCOUNT_LOCKED` / `ACCOUNT_UNLOCKED` / `ACCOUNT_ENABLED` / `ACCOUNT_DISABLED` | 仅 false→true / true→false 时 `publishEvent` |
| 超阈值 **边沿** | `LOGIN_FAIL_*_EXCEED`、`BLACKLIST_BLOCK`、网关升级路径上的 `RATE_LIMIT_VIOLATION` | 同一 key 一次临时封禁生命周期至多 1 条；后续只刷新 TTL |
| 活动 **电平** | `LOGIN_SUCCESS`、`LOGIN_FAILURE` | 每次成功/失败可发；不去重 |

`RATE_LIMIT_VIOLATION` 默认仍为 **BEST_EFFORT**（优先级表不变）；网关限流升级路径改为边沿触发。账号锁定短路与 Redis 信号见 [account-protection](../../security/account-protection/SPEC.md)；Access/Gateway 边沿见 [access-protection](../../security/access-protection/SPEC.md)。

## 4. 投递语义

- 业务线程：**fail-open**；`PublishOutcome` 表示同步接纳，不表示最终持久化。
- 事务内事件：**afterCommit** 入队。
- BEST_EFFORT：内存队列满 → `DROPPED`。
- DURABLE：spool 拒绝/超时 → `FAILED`（业务仍放行）。
- at-least-once + Store `event_id` 幂等。

## 5. MySQL Store

- 表：canonical `security_event`（`event_id` UNIQUE、`priority`、`received_at` 索引）。
- 写入：`max-concurrent-writes=1` semaphore + 真批量 INSERT。
- 查询：`SecurityEventQueryRepository` 游标 `(receivedAt,id)` 降序；默认 24h、最大 31 天；page 默认 50、最大 200。
- Retention：命名锁、分批删除、30s 时间预算、队列积压 50% 让步；任务 `PurgeCanonicalSecurityEventTask` cron `0 30 3 * * ?`。

## 6. 中心 admission

Feign/HTTP 上报 → 校验 → `SecurityEventEnqueue` → 异步 Store；HTTP 线程不持有 Store 事务。

响应码：`SEC_EVENT_503`（DURABLE 可重试）、`SEC_EVENT_429`（BEST_EFFORT 拒绝）。

## 7. 配置

唯一 `@ConfigurationProperties`：`com.ingot.framework.security.recording.config.SecurityEventProperties`（前缀 `ingot.security.event`）。字段说明与样例见 [example.yml](../../../ingot-framework/ingot-security/ingot-security-recording/example.yml) 与 [README](./README.md)。

## 8. 稳定态

显式配置 `target=local|center` 且 `shadow-targets: []`：

- 仅 canonical `security_event` 增长；
- legacy `account_security_event` 已物理删除（migration `013`）。

## 9. Legacy 清理后事实

- 已删除 `AsyncSecurityEventReporter`、`RemoteSecurityEventPortAdapter`、legacy `mode`/`async` 兼容层与 `legacyModeUsed` 观测字段（20260806）。
- 配置仅认 `target` / `shadow-targets` / `delivery.*`；唯一绑定类见 §7。
- Retention：`PurgeCanonicalSecurityEventTask`；让步探测分别读取 `MemoryRecordQueue` 与 `FileSpoolRecordQueue`（不得按裸 `RecordQueue` 解析，以免双 Bean 歧义）。
- 旧表 `account_security_event` 已由 migration `013` DROP（`ingot_core` / `ingot_member`）。
