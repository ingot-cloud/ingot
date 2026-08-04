# Design

## 方案摘要

### 当前问题

当前账号域链路为：

```text
UseCase transaction
  -> CompositeSecurityEventPort
       -> 同步 INSERT account_security_event（与业务共用连接池和事务）
       -> mode=remote 时进入内存有界队列
            -> Feign
                 -> 安全中心请求线程/事务逐条 INSERT security_event
```

该实现只保护远程 Feign 客户端队列，仍存在以下问题：

- 本地事件 INSERT 可占用业务连接并导致业务事务回滚。
- 安全中心 ingest 无独立舱壁，批量接口仍在一个事务中循环单条 INSERT。
- `mode` 同时表达部署拓扑和双写策略，存储技术无法替换。
- 本地表和中心表长期双写但无一致性、查询事实源或统一 retention 规则。
- 没有 `eventId`，未来重试、file spool 或 Kafka 会产生重复记录。
- retention 使用业务数据源，缺少时间预算、实例互斥和对写入积压的让步。

### 目标架构

```text
Domain UseCase
  -> compatibility SecurityEventPort
  -> SecurityEventPublisher
       -> afterCommit
       -> PriorityClassifier
            BEST_EFFORT -> bounded memory queue
            DURABLE     -> bounded file spool
       -> Dispatcher
            target=local  -> SecurityEventStore (MySQL / log / future ES)
            target=center -> SecurityEventTransport (Feign / future Kafka)
                                   -> center admission queue/spool
                                   -> center SecurityEventStore
            shadow-targets -> migration-only secondary delivery
```

稳定态的 `target` 只有一个；shadow 不参与查询事实源判定。

## 模块与依赖

### 新增模块

| 模块 | 职责 | 主要依赖 |
|---|---|---|
| `ingot-security-recording` | 公共 record 模型、Publisher/Store/Queue/Retention SPI、dispatcher、file spool、配置、指标与自动配置 | Spring Boot、Jackson、Micrometer/Actuator 可选 |
| `ingot-security-event-store-mysql` | `security_event` MyBatis 实现、批量写入、查询、幂等、retention | recording、MyBatis-Plus |
| `ingot-security-event-store-log` | JSONL segment 最终 Store；复用 file segment 引擎，支持滚动与 retention，不提供查询 | recording |

### 现有模块调整

- `ingot-security-account-core` 的 `SecurityEventPort` 保持不变。
- `ingot-security-account-adapter` 由兼容适配器把 `AccountSecurityEvent` 映射为 `SecurityEventRecord`，移除稳定态 Composite 双写职责。
- `ingot-security-api` 保留 DTO、enum 与 `RemoteSecurityEventService`，定位为 Transport wire contract。
- `ingot-security-provider` 使用 recording admission + 选中的 Store，不再直接在 Controller 事务中入库。
- Gateway Reporter 改为调用统一 Publisher，而不是持有独立 `AsyncSecurityEventReporter`。

### 未来模块

- `ingot-security-event-store-es`：实现相同 Store/Query contract，retention 映射为 ILM。
- `ingot-security-event-transport-kafka`：实现 Transport/RecordQueue contract，使用同一 wire envelope 与 `eventId`。

## 公共契约

### 领域模型

`SecurityEventRecord` 为安全事件唯一内部模型，至少包含：

| 字段 | 规则 |
|---|---|
| `eventId` | 32 字符 UUID，由 producer 在首次发布前生成，重试保持不变 |
| `eventType` / `eventCategory` | 复用现有 API enum code，类型与类别必须一致 |
| `priority` | `BEST_EFFORT` / `DURABLE` |
| `occurredAt` | 业务发生时间，必填；缺省由 Publisher 补当前时间 |
| `receivedAt` | Store 接收时间，由最终 Store 填充 |
| 业务字段 | tenant/user/account/client/session/device/ip/uri/result/reason/source/operator/trace 等 |
| `extension` | JSON 对象，序列化后最大 16 KiB；拒绝秘密字段，ES future mapping 必须 `dynamic:false` |

`AuditRecord` 只预留契约，包含 `auditId`、actor、action、target、before/after、result、reason、source、trace 与 occurredAt；不与 `SecurityEventRecord` 继承同一个业务字段基类，仅共享 record identity、priority 和投递元数据。

### SPI

```java
public interface SecurityEventPublisher {
    PublishOutcome publish(SecurityEventRecord record);
}

public interface SecurityEventStore {
    String storeId();
    StoreCapabilities capabilities();
    void appendBatch(List<SecurityEventRecord> records);
}

public interface SecurityEventQueryRepository {
    CursorPage<SecurityEventRecord> query(SecurityEventQuery query, CursorPageRequest page);
}

public interface SecurityEventTransport {
    String transportId();
    DeliveryResult deliverBatch(List<SecurityEventRecord> records);
}

public interface RecordQueue<T> {
    EnqueueResult enqueue(T record);
    List<ClaimedRecord<T>> claim(int limit, Duration wait);
    void ack(List<String> claimIds);
    void nack(List<String> claimIds, Throwable cause);
}
```

约束：

- `PublishOutcome`：`ACCEPTED`、`DROPPED`、`DISABLED`、`FAILED`。
- `StoreCapabilities` 至少包含 `IDEMPOTENT`、`QUERY`、`RETENTION`。
- 日志 Store 不注册 `SecurityEventQueryRepository`；NoOp 不注册 Store，只由禁用 Publisher 返回 `DISABLED`。
- Query page 默认 50、最大 200；cursor 使用 `(receivedAt,id)`，禁止 offset 深分页。
- `SecurityEventReportDTO` 增加可选 `eventId`、`priority`；旧客户端缺少 `eventId` 时由接收端生成，因此只有新客户端具备跨重试幂等。

## 装配与配置

### 主配置

```yaml
ingot:
  security:
    event:
      enabled: true
      target: local              # local | center
      primary-store: mysql       # 仅多个 Store module 同时存在时必填
      shadow-targets: []         # 迁移期可设 [local]，稳定态必须为空
      source-module: ingot-pms
      categories:
        auth: true
        account: true
        credential: true
        access: true
      delivery:
        memory:
          queue-capacity: 2048
          batch-size: 32
          poll-timeout-ms: 100
        spool:
          directory: ${logs.path:./logs}/security-recording/spool
          max-bytes: 1GB
          segment-bytes: 64MB
          durable-ack-timeout-ms: 20
          replay-batch-size: 32
          retry-initial-ms: 1000
          retry-max-ms: 60000
      mysql:
        max-concurrent-writes: 1
        transaction-timeout-seconds: 5
      retention:
        enabled: true
        days: 30
        batch-size: 500
        max-rounds: 100
        max-duration-seconds: 30
        yield-queue-usage-percent: 50
```

规则：

- `target=local` 要求存在且只能解析出一个主 Store。
- `target=center` 要求存在 `SecurityEventTransport`；本地 Store 即使在 classpath 也不写入。
- 同时发现多个 Store 且未配置 `primary-store` 时启动失败。
- `shadow-targets` 与主 target 去重，只用于迁移；启动健康信息必须明确标记 shadow 状态。
- `enabled=false` 注册 Disabled Publisher，不创建 dispatcher/worker/spool。
- 配置 Nacos 热刷新仅影响 category、priority override、target 与 shadow；Store module 集合、spool 目录、容量和线程资源需重启生效。

### 旧配置兼容

当未配置新 `target` 时：

| 旧配置 | 兼容映射 |
|---|---|
| `enabled=false` | Disabled Publisher |
| `mode=local` | `target=local` |
| `mode=remote` | `target=center` + `shadow-targets=[local]`，保持当前双写 |

新 `target` 存在时完全忽略旧 `mode` 并输出一次弃用告警。旧 `async.*` 映射到 `delivery.memory.*`；旧 retention 键继续生效。兼容层保留一个发布周期，删除另开 breaking change。

## 数据流与失败处理

### 事务发布

- Publisher 检测活动 Spring 事务时注册 `TransactionSynchronization.afterCommit`；回滚时不发布。
- 无活动事务时立即进入分类与队列。
- Publisher 不抛 Store/Transport 异常到业务调用方；默认 fail-open，通过返回值、指标与健康状态暴露失败。
- 未来审计可按 action 配置 `FAIL_CLOSED`；本 change 不在业务接口启用该策略。

### 优先级

| 默认级别 | 事件 |
|---|---|
| `BEST_EFFORT` | `LOGIN_SUCCESS`、`LOGIN_FAILURE`、`RATE_LIMIT_VIOLATION` |
| `DURABLE` | ACCOUNT、CREDENTIAL、`BLACKLIST_BLOCK`、四种 `LOGIN_FAIL_*_EXCEED` |
| `DURABLE` | 未来全部 AuditRecord，除非审计 change 明确覆盖 |

- category/type override 可热刷新，但不能把 AuditRecord 默认降为 BEST_EFFORT。
- BEST_EFFORT 使用 `offer`，队列满即 `DROPPED`。
- DURABLE 提交给单独 file writer，并最多等待 20ms 接纳结果；超时/磁盘满返回 `FAILED`、业务放行、严重告警。
- file spool 采用 append-only segment + record checksum；启动时截断不完整尾记录，损坏中间 segment 移入 quarantine 并标记 health `DOWN`/`DEGRADED`。
- 未 ack 记录不得因 retention 或配额被覆盖。Store 成功、ack 前崩溃会重投，由 `eventId` 幂等吸收。

### 本地 MySQL

- Dispatcher 单 consumer 攒批调用一次真正的 multi-row INSERT/BATCH executor，不再循环调用单条 Service。
- `max-concurrent-writes=1` semaphore 包围取连接至事务完成的全过程。
- 可绑定独立事件 DataSource；未配置时复用主 DataSource，但 semaphore 仍保证事件 writer 最多占一个连接。
- 唯一键冲突视为已成功保存；批次中其它非幂等错误整体 nack 并退避重试。
- shadow Store 失败不影响主 Store ack，但必须单独计数并阻止自动切换验收。

### 远程中心

- Producer Transport 批量上报，默认每批 32，最大请求 100。
- 中心 Controller 完成 schema、枚举、来源、批次大小与 payload 上限校验后进入本地 admission 管道。
- DURABLE 事件只有在中心 spool 接纳后才返回成功；容量不足返回可重试失败，producer 保留本地 claim。
- BEST_EFFORT 中心队列满时返回明确拒绝，producer 记录 drop 后 ack，避免无界重试放大攻击。
- 中心 worker 再调用所选 Store；HTTP 线程不持有 Store 事务。

## 数据模型

### Canonical `security_event`

中心现有表原地增加：

```sql
event_id  VARCHAR(32) NULL,
priority  VARCHAR(16) NOT NULL DEFAULT 'BEST_EFFORT',
UNIQUE KEY uk_event_id (event_id),
KEY idx_received_id (received_at, id)
```

- 新记录 `event_id` 必填；ALTER 阶段允许 NULL 兼容历史多行。
- 保留现有 bigint `id` 作为物理主键和查询 cursor 的第二部分。
- PMS/Member 独立部署在各自数据库新增同结构 canonical `security_event`。
- 旧 `account_security_event` 不加新写入依赖；shadow 期间继续按旧 adapter 写，切换后只读保留。
- 不在线回填历史 `event_id`，不自动把旧本地表迁入中心；如需历史汇总另做离线 change。

### 查询

`SecurityEventQuery` 支持：

- 时间范围（默认最近 24 小时；最大单次范围 31 天）。
- eventType/category、tenantId、userId、sourceModule、traceId。
- `(receivedAt,id)` 降序 cursor，默认 50，最大 200。

本 change 只交付 Repository 与 contract tests，不提供 Platform Controller/导出。

## Retention

### MySQL

- 默认 30 天；`days=0` 永久保留。
- 先按 `(received_at,id)` 有序选择至多 500 个 id，再按主键删除，单次事务一批。
- 单次任务最多 100 批且最多 30 秒，任一先到即退出。
- 使用数据库命名锁保证共享数据库同一时刻仅一个实例清理；获取失败立即跳过。
- dispatcher 内存队列或 spool replay 使用率达到 50% 时跳过本轮或批间退出。
- retention 与 writer 使用同一 semaphore，禁止同时占用额外事件连接。

### 文件日志与 spool

- 日志 Store 按事件 retention 删除已关闭 segment，并受独立 `total-size-cap` 限制。
- spool 只删除已 ack segment；未 ack 数据不受事件 retention 删除。
- 配额不足时拒绝新的 DURABLE 记录，不覆盖旧记录。

### 审计与未来 ES

- AuditRecord 默认 180 天，具体合规要求由后续审计 change 覆盖。
- ES Store 必须把相同 retention 映射到 index template + ILM；应用定时任务不执行全量 `delete_by_query`。

## 可观测与安全

至少暴露以下指标，并按 `sourceModule`、priority、target、result 做低基数标签：

- published/accepted/persisted/dropped/failed/replayed/duplicate total。
- memory queue depth/usage、spool bytes/records/oldest age、quarantine count。
- Store latency、Transport latency、retry count、retention deleted/skipped。
- 当前 target、primary Store、shadow targets、legacy mode mapping 和健康状态 Actuator 信息。

日志必须限流，不能对每个 dropped event 输出 warn。spool 文件权限为 owner-only；payload 在入队前完成脱敏和大小校验，首期依赖宿主机持久卷/磁盘加密，不另做应用层加密。

## 审计契约预留

- `AuditPublisher`、`AuditStore`、`AuditQueryRepository` 与 `AuditRecord` 进入 recording API，但不注册业务 producer。
- 审计默认 DURABLE、retention 180 天、失败策略 `FAIL_OPEN_ALERT`。
- 高风险操作可在后续 change 显式配置 `FAIL_CLOSED`。
- MySQL 审计表、ES 索引、查询/导出权限、防篡改签名或哈希链均不在本 change。

## 迁移与回滚

### 上线顺序

1. 发布 recording 与 Store modules，执行 additive schema migration；不切 producer。
2. 安全中心接入 admission pipeline，保留旧 API wire compatibility。
3. PMS/Member/Gateway 接入 Publisher，旧 `mode` 兼容映射保持现有行为。
4. 开启新配置与 shadow，对账 eventId、字段、数量、延迟、drop 和 shadow failure。
5. 切换 `target=center` 或 `target=local` 且清空 `shadow-targets`，确认只有权威 Store 增长。
6. 旧 `account_security_event` 停止写入，继续只读/retention；更新 current 后归档 change。

### 回滚

- 配置回退到旧 `mode` 映射与旧 Composite 链路；新表和新增列为 additive，不需立即回滚 DDL。
- 中心新 admission 异常时可回退旧 Controller 直接入库，但必须保留请求限流和事件 writer 舱壁，禁止恢复无限制逐条写入。
- 切换失败时恢复 shadow/旧本地权威，不删除已写入的新 Store 数据；以 `eventId` 对账。
- file spool 在回滚后保留，待原版本无法消费时使用离线重放工具导出，不直接删除。

## 测试策略

- **单元**：优先级、事务 afterCommit、路由、旧配置映射、NoOp、payload 校验、退避和 health 状态。
- **Store contract**：MySQL 幂等批量写、cursor query、retention；日志 segment 滚动、恢复和 retention。
- **spool**：正常重启、尾部截断、中间损坏、配额满、ack 前崩溃、quarantine 和重放。
- **集成**：PMS/Member local/center、Gateway ACCESS、安全中心 admission、中心停机恢复。
- **资源隔离**：小连接池、慢 INSERT、死锁、retention 并发与攻击洪峰下验证连接许可和业务响应。
- **迁移**：旧 `mode` 矩阵、shadow 对账、单一权威切换、旧表停止增长和回滚。
- **审计预留**：模型、SPI、默认 priority/retention/failure policy contract 编译与单测，不触发真实审计落库。

## 关键决策

| ID | 决策 | 状态 |
|---|---|---|
| D1 | Store 技术由 module 依赖确定；多个 Store 必须显式选主 | 已批准 |
| D2 | 同一 PMS/Member 制品用 `target` 切换本地/中心 | 已批准 |
| D3 | 稳定态单一权威，双写仅用于 shadow 迁移 | 已批准 |
| D4 | MySQL + 日志首期实现，ES/Kafka 仅预留 SPI | 已批准 |
| D5 | 分级可靠；高频 best-effort，关键事件 file spool | 已批准 |
| D6 | 仅 eventId 传输幂等，不做业务 dedup | 已批准 |
| D7 | 审计独立领域模型，复用 recording runtime | 已批准 |
