# Design

## 方案摘要

在 `ingot-security-recording` 的 file spool 上修复三类缺陷：目录冲突、ack 后误重建、state 截断 fail-fast。

实际路径：`{spool.directory}/{applicationName}/`，目录内持有 `spool.lock`。消费位点以完好 `state.json` 的 pending/inFlight 为准；空队列即已 drain。payload 只存在 append-only segment 中，state 只存 offset 元数据，tmp+replace 原子落盘。

## 数据模型与接口

### 运行时目录

```text
{directory}/{applicationName}/
  spool.lock
  state.json
  state.json.tmp          # 仅写入窗口存在
  segments/*.spool
  quarantine/
```

`FileSpoolAutoConfiguration` 注入 `spring.application.name`（缺省 `application`）并传给 `FileSpoolRecordQueue` 三参构造器。单测不传 application name 时目录等于配置的 `directory`（已是唯一 temp 路径）。

### `SpoolState`

- 字段：`activeSegment`、`totalBytes`、`pending`、`inFlight`
- `SpoolEntry` 持久化：`claimId`、`segment`、`offset`、`length`、`attempts`、`nextRetryAtEpochMs`
- `record` 运行时填充，`@JsonIgnore`，不写盘
- `save`：写 `state.json.tmp`，再 `ATOMIC_MOVE`（不支持则 `REPLACE_EXISTING`）；无 pretty printer
- `load`：区分 MISSING / LOADED / CORRUPT，CORRUPT 不抛给容器

### 启动流程

1. 创建目录，`tryLock(spool.lock)`；失败则 `IOException`（Bean 创建失败是有意的，避免双 writer）。
2. 截断所有 segment 不完整尾记录。
3. 按 load 结果：
   - LOADED：inFlight 全部并入 pending 且 `nextRetryAt=0`；**不**扫描 segment。
   - MISSING 或 CORRUPT：隔离坏 `state.json`（若有），扫描 segment 重建 pending。
4. 按 offset 从 segment hydrate `record`；读失败则 quarantine 该 segment。
5. 回收无引用 segment（active truncate 到 0）。
6. 刷新 `totalBytes` 并 persist。

### ack / nack

- ack：先从 inFlight 移除，**persist 成功后再** `deleteAcknowledgedSegments`（含 active truncate）。persist 失败则把刚移除的条目加回 pending。
- nack：退回 pending 并 persist；失败保留内存中的 pending 视图，不得吞掉后假装成功到「磁盘已是最终态」。

## 数据流与失败处理

```text
enqueue → append segment → pending 元数据 → persist
claim   → pending→inFlight → persist → 返回 hydrate 后的 record
ack     → 去 inFlight → persist → 删/truncate 无引用 segment
nack    → inFlight→pending + backoff → persist
```

损坏 `state.json`：移入 `quarantine/state.json-{epoch}`，扫描 segment 重建（一次性重放）。截断 JSON 不得导致 `UnsatisfiedDependencyException` 链。

## 迁移与回滚

- 配置键 `delivery.spool.directory` 不变；运行时自动拼 application name。旧共用目录中的文件不会被新进程读取。
- 上线：停服务 → 删除或改名 `/ingot-data/security-recording/spool` 下旧文件 → 启动新构建。
- 回滚旧版本：旧代码仍写父目录本身；与新子目录并存，互不消费。勿把新旧进程指向同一最终目录。

## 测试策略

`FileSpoolRecordQueueTest` 覆盖 REQUIREMENTS 验收标准；现有 enqueue/claim/ack、配额、pending 恢复、尾截断用例适配 `AutoCloseable`（重启前关闭上一实例以释放锁）。
