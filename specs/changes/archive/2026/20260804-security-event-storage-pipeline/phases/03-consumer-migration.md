# Phase 03：消费者与安全中心迁移

## 目标

让 PMS、Member、Gateway 与安全中心进入统一管道，并在不改变现有生产结果的前提下完成 shadow 对账。

## 交付

- 安全中心 admission + 异步 Store worker。
- PMS/Member 同一制品 `target=local|center`。
- Gateway ACCESS 统一 Publisher。
- 旧 mode 兼容、shadow 指标与对账脚本。

## 退出条件

- local/center、旧配置与 Feign 故障集成测试通过。
- 事件失败不回滚业务事务。
- shadow 字段、数量和延迟对账无未解释差异。

## 回滚

恢复旧 mode 映射和 Composite/旧 Controller 路径；保留新表与 spool，使用 eventId 对账，不删除已接纳记录。
