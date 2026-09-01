# Requirements

## 用户场景

1. Auth、PMS、Member、Gateway 等服务启用安全事件 recording 但当前没有 DURABLE 事件：worker 应阻塞等待，不得持续占满 CPU 核心。
2. worker 正在空闲等待时产生新的 DURABLE 事件：新事件应立即唤醒消费者，不固定等待完整轮询周期。
3. pending 事件尚未到 `nextRetryAtEpochMs`：worker 应等待后重新检查，不得忙轮询。
4. 服务关闭并中断 worker：claim 应及时退出并保留中断标记。

## 业务规则

- R1：`RecordQueue.claim(limit, wait)` 在没有当前可领取记录时，等待不超过 `wait`；新记录到达或线程中断可提前返回。
- R2：`limit <= 0` 时立即返回空集合；`wait` 为零或负数时仅执行一次非阻塞领取，保持兼容。
- R3：`FileSpoolRecordQueue` 必须使用单调时钟计算剩余等待时间，并循环处理虚假唤醒。
- R4：enqueue 新增 pending、nack 退回 pending 后必须通知等待消费者重新检查。
- R5：中断等待时返回空集合并恢复当前线程的中断标记。
- R6：`MemoryRecordQueue` 与 `UnconfiguredDurableRecordQueue` 必须遵守同一非正上限、等待与中断契约。
- R7：不得改变 file spool 的持久化格式、消费位点、ack/nack、退避与 at-least-once 语义。

## 边界与非目标

- 继续复用现有 `delivery.memory.poll-timeout-ms` 作为 dispatcher 传入的等待上限，不新增 spool polling 配置。
- 不承诺 `claim` 一定等待满指定时长；记录到达、中断或已有可领取记录时可提前返回。
- 不处理独立调用方在 queue `close()` 与 `claim()` 之间的并发生命周期协调。
- 不调整 enqueue 持久化失败时的既有处理语义。

## 验收标准

- [x] 空 file spool 在等待期限前不返回，超时后返回空集合
- [x] 空闲 claim 可被 enqueue 唤醒并领取新记录
- [x] 未到重试时间的 pending 不导致 claim 快速返回
- [x] file spool claim 被中断后及时返回并保留中断标记
- [x] unconfigured durable queue 遵守等待与中断契约
- [x] memory queue 在领取上限非正时立即返回且不消费记录
- [x] recording 模块现有与新增测试全部通过
- [x] 空闲 claim 线程处于 `TIMED_WAITING`，`security-recording-durable` 不再形成无等待循环
