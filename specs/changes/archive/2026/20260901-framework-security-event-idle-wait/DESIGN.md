# Design

## 方案摘要

将 `RecordQueue.claim(limit, wait)` 明确为带超时的可中断等待契约。file spool 复用保护 `SpoolState` 的 `ReentrantLock` 创建 `Condition`：claim 无可领取记录时释放锁等待，enqueue/nack 改变 pending 后通知等待方。占位 durable queue 使用可中断的定时停放，避免 dispatcher 测试或自定义装配空转。

## 数据模型与接口

- 不新增或修改数据模型、配置键和方法签名。
- 补全 `RecordQueue.claim` JavaDoc：`limit`、`wait`、提前返回、中断与空结果语义。
- `MemoryRecordQueue` 在阻塞 poll 前处理非正 `limit`，避免取出记录后以负 drain 数量失败。
- `FileSpoolRecordQueue` 增加进程内 `Condition` 字段，不持久化、不影响 spool 格式。

## 数据流与失败处理

### File spool claim

1. `limit <= 0` 时立即返回空集合；非正 `wait` 时仅按当前状态尝试一次，不进入条件等待。
2. 使用 `System.nanoTime()` 计算等待截止时间并在锁内尝试领取到期 pending。
3. 无可领取记录且仍有剩余时间：调用 `Condition.awaitNanos`，释放锁等待。
4. 被 enqueue/nack 通知或虚假唤醒后重新检查记录和剩余时间；超时返回空集合。
5. 捕获 `InterruptedException`，恢复中断标记并返回空集合，由 dispatcher 在关闭标记下退出。

### 唤醒

- enqueue 将记录加入 pending 后 `signalAll`；等待方获得锁后只能看到完整的锁内状态变更。
- nack 将记录退回 pending 并计算下次重试时间后 `signalAll`；若尚未到期，claim 继续等待到本轮超时。

### Unconfigured queue

- 正等待时间使用 `LockSupport.parkNanos` 定时停放；中断会提前返回且中断标记自然保留。
- 非正等待时间立即返回空集合。

## 迁移与回滚

- 纯运行时兼容修复，无数据或配置迁移。
- 回滚仅需恢复旧构建；spool 文件和 `state.json` 完全兼容。

## 测试策略

- 使用 executor/future 与宽松的超时窗口验证等待和唤醒，避免基于精确耗时的脆弱断言。
- 构造 nack 后带未来 `nextRetryAtEpochMs` 的记录，验证 claim 不快速返回。
- 使用独立线程验证中断返回和中断标记。
- 执行 `:ingot-framework:ingot-security:ingot-security-recording:test` 全量回归。
