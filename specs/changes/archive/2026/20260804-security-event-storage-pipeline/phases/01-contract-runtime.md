# Phase 01：统一契约与运行时

## 目标

建立不依赖 MySQL、Feign、ES 的 recording 公共契约与受保护发布管道，使业务模块只面向 Publisher。

## 交付

- `ingot-security-recording` module。
- 安全事件与审计预留模型、Store/Query/Transport/Queue/Retention SPI。
- afterCommit、优先级、target/shadow 路由、内存队列、失败策略、metrics/health。
- 旧 `mode/async/retention` 到新配置的兼容映射。

## 退出条件

- 自动配置装配矩阵、事务、队列满和路由单测通过。
- 无具体存储或 RPC 依赖渗入公共模块。
- D1-D7 已批准且公共 API 不再有待决命名。

## 回滚

本阶段不迁移 producer，不改变运行行为；删除新 module 依赖即可回滚。
