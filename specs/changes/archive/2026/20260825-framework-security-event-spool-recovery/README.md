# DURABLE spool 隔离与消费位点修复

> 状态：completed

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260825-framework-security-event-spool-recovery` |
| 领域 | `framework` / `security-event-recording` |
| 负责人 | jy |
| 创建日期 | 2026-08-25 |
| 目标发布日期 | TBD |

## 目标

- 各服务 DURABLE file spool 目录隔离，禁止多进程抢写同一 `state.json`。
- ack 成功落盘后，稳定重启 **不再重放** 已消费事件。
- `state.json` 截断或损坏时 **降级恢复**，不得阻止 Spring 容器启动。
- claim 后、ack 前崩溃的 inFlight 记录在启动时退回 pending 并重投。

## 范围

### 包含

- `FileSpoolRecordQueue` / `SpoolState` / `SpoolRecovery` / `FileSpoolAutoConfiguration`
- 按 `spring.application.name` 拼子目录 + `spool.lock` 独占
- 空队列禁止扫描重建；ack 后 truncate 已无引用的 active segment
- 原子 compact `state.json`；payload 只留在 segment
- 单测与 example.yml / Nacos 注释
- 验收后更新 `specs/current/framework/security-event-recording`

### 不包含

- Store / Transport / 事件类型变更
- 跨实例共享消费组
- 调整 `durable-ack-timeout-ms`

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：2026-08-25
- 关联提交或 PR：工作区实施（随代码一并提交）
- 更新的 current capability：`specs/current/framework/security-event-recording`
- 与原设计的差异：`LoadResult` 实例方法命名为 `isCorrupt()` / `isMissing()`，避免与静态工厂 `corrupt()` 冲突；`durableRecordQueue` Bean 返回具体类型 `FileSpoolRecordQueue`，便于 retention 让步探测
- 取消原因：—
