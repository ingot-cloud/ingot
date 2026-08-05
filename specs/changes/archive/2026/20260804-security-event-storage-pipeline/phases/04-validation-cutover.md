# Phase 04：验证与单一权威切换

## 目标

在攻击、慢库、中心停机、重启和清理任务场景下验证资源隔离，然后关闭 shadow，形成单一权威存储。

## 交付

- 资源隔离/故障恢复/retention/回滚测试报告。
- local 或 center 权威切换记录。
- 旧 `account_security_event` 停写、只读与清理安排。
- 最终 current capability 与后续 breaking cleanup change。

## 退出条件

- REQUIREMENTS 全部验收。
- 观察窗口内只有权威 Store 增长，无持续 spool 积压和未解释 drop。
- Current 已按最终实现更新，旧兼容清理有独立跟踪。

## 回滚

重新开启 shadow 或恢复旧本地权威；保留新旧数据，通过 eventId/时间窗对账，禁止破坏性删表回滚。
