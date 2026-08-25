# Requirements

## 用户场景

1. 运维同时启动 Auth / PMS / Member / Security 等开启了 `ingot.security.event.enabled=true` 的服务：各进程写各自 spool 子目录，互不影响。
2. DURABLE worker 已把 pending 全部 ack 且 `state.json` 完好落盘：再次重启 **claim 为空**，不向 Store 重放。
3. 进程在 claim 之后、ack 之前被杀死：重启后该批记录仍可 claim（at-least-once，`event_id` 幂等）。
4. `state.json` 写到一半被截断：服务仍能启动；坏文件进入 quarantine；未 ack 的 segment 记录可一次性重建 pending。

## 业务规则

- R1：实际 spool 根目录为 `{delivery.spool.directory}/{spring.application.name}`。`directory` 仅为父目录。
- R2：同一 spool 目录同时只允许一个 writer（`spool.lock` 独占）。第二实例快速失败。
- R3：`state.json` **完好** 且 pending、inFlight 皆空，视为已消费完毕，**禁止** 扫描 segment 重建。
- R4：完好 state 中的 inFlight 在启动时全部退回 pending（`nextRetryAt=0`）。
- R5：仅当 `state.json` 缺失或无法解析时，隔离坏文件并扫描现有 segment 重建 pending。
- R6：某 segment 已无 pending/inFlight 引用时：非 active 删除；active 则 truncate 到 0。
- R7：`state.json` 原子写入（tmp + replace）；不 pretty-print；不序列化事件 payload。
- R8：Jackson/IO 解析失败不得冒泡导致整个 ApplicationContext 刷新失败。
- R9：ack/nack persist 失败不得静默忽略；ack 在 persist 成功前不得回收磁盘。

## 边界与非目标

- 同机同一 `spring.application.name` 的第二实例拿不到锁，不在本 change 做分区消费。
- 损坏 state 后的 segment 全量扫描是一次性 at-least-once；Store 靠 `event_id` 幂等。
- 不修改 `durable-ack-timeout-ms`、Store、Transport、事件 code。
- 旧共用目录 `/ingot-data/security-recording/spool` 下的历史文件不自动迁移；上线前由运维删除或改名。

## 验收标准

- [x] enqueue → claim → ack → 关闭 queue → 新实例重启 → claim 为空
- [x] claim 后不 ack 即关闭 → 重启后仍可 claim 同一 `event_id`
- [x] 截断 `state.json` 时构造 `FileSpoolRecordQueue` 不抛；坏文件在 quarantine
- [x] 完好空 pending + 非空 active segment **不得** 把历史扫回 pending
- [x] 全部 ack 后 active segment 长度为 0
- [x] 同一 parent directory、不同 application name 的两个 queue 文件互不覆盖
- [x] 残留损坏 `state.json.tmp` 不影响已成功落盘的 `state.json`
