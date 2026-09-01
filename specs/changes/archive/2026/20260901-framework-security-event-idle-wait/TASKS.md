# Tasks

## 实施任务

- [x] T1：明确 `RecordQueue.claim` 等待与中断公共契约
  - 依赖：无
  - 验收：JavaDoc 符合 `docs/standards/Javadoc.md`，方法签名不变
- [x] T2：实现 `FileSpoolRecordQueue` Condition 等待与 enqueue/nack 唤醒
  - 依赖：T1
  - 验收：空队列和未到期 pending 不快速返回；新 pending 可唤醒；中断可退出
- [x] T3：让 `MemoryRecordQueue` 与 `UnconfiguredDurableRecordQueue` 遵守完整契约
  - 依赖：T1
  - 验收：正等待时间不立即返回，中断标记保留
- [x] T4：补齐并发回归测试
  - 依赖：T2、T3
  - 验收：覆盖超时、enqueue 唤醒、retry 未到期和中断

## 验证任务

- [x] V1：执行 `ingot-security-recording` 模块测试
- [x] V2：核对改动未涉及配置、spool 格式、数据库与服务业务代码
- [x] V3：并发测试观察 claim 线程进入 `TIMED_WAITING`；本机 Nacos 未运行，完整 Auth 进程可在 IDEA 启动后复核

## 完成检查

- [x] 实现与 DESIGN 一致
- [x] REQUIREMENTS 验收标准全部满足
- [x] Current 已更新
- [x] Change 已记录完成信息并归档
