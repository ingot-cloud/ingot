# Phase 02：存储与可靠投递

## 目标

交付可替换 Store 和 DURABLE file spool，并证明统一 SPI 能覆盖写入、查询、幂等与 backend-native retention。

## 交付

- file segment/spool engine 与恢复工具。
- MySQL Store、QueryRepository、RetentionHandler。
- 结构化文件日志 Store。
- additive schema migration 与 Store contract test suite。

## 退出条件

- MySQL 真批量写、单连接许可、eventId 幂等、cursor query 和 retention 测试通过。
- spool 重启/损坏/满盘/ack 窗口测试通过。
- 旧表和现有 producer 尚未被破坏性修改。

## 回滚

新表/列保留但不启用；移除 Store 自动配置即可回到旧链路。不得删除含有 shadow/spool 数据的文件或表。
