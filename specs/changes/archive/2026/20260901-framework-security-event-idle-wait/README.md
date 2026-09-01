# 安全事件 DURABLE 队列空闲等待修复

> 状态：completed

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260901-framework-security-event-idle-wait` |
| 领域 | `framework` / `security-event-recording` |
| 负责人 | jy |
| 创建日期 | 2026-09-01 |
| 目标发布日期 | 2026-09-01 |

## 目标

- 消除 DURABLE worker 在无可领取事件时的忙轮询，避免每个启用 recording 的服务持续占满一个 CPU 核心。
- 统一 `RecordQueue.claim(limit, wait)` 的等待与中断契约，避免其他实现重复引入相同问题。

## 范围

### 包含

- `RecordQueue` claim 等待契约 JavaDoc
- `MemoryRecordQueue` 非正领取上限兼容守卫
- `FileSpoolRecordQueue` 基于 `Condition` 的空闲等待与 enqueue/nack 唤醒
- `UnconfiguredDurableRecordQueue` 定时等待
- 等待、唤醒、重试未到期与中断回归测试

### 不包含

- spool 文件格式、ack/nack、重放和退避策略调整
- 新增配置键或修改 Nacos
- Store、Transport、数据库与服务业务代码变更
- recording worker 优雅关闭的其他重构

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：2026-09-01
- 关联提交或 PR：工作区实施（随代码一并提交）
- 更新的 current capability：`specs/current/framework/security-event-recording`
- 与原设计的差异：完整 SPI 契约复核时补充了 `MemoryRecordQueue` 的非正 limit 守卫；其余实现与批准设计一致
- 取消原因：
