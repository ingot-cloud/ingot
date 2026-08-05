# 安全事件统一 SPI、可靠投递与可插拔存储

> 状态：completed

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260804-security-event-storage-pipeline` |
| 领域 | `security` / `framework` |
| 负责人 | jy |
| 创建日期 | 2026-08-04 |
| 目标发布日期 | TBD（分 Phase 交付） |
| 需求来源 | 统一安全事件中心 L3 上线后发现的本地数据库隔离、双写一致性、存储可替换性与未来审计复用问题 |
| 前置基线 | [统一安全事件中心](../../../current/security/security-event-center/README.md) |

## 目标

把现有“PMS/Member 同步写本地表、`mode=remote` 时再异步写安全中心”的固定实现，改造成职责清晰的安全记录管道：

- 业务侧只依赖统一发布入口，不直接感知 MySQL、ES、日志或远程中心。
- Module 依赖决定可用的存储/传输实现；运行时配置只选择本地或中心目标。
- 稳定态只有一个权威存储，迁移期才允许显式 shadow 双写。
- 事件洪峰、数据库慢查询或中心故障不能占满业务连接池或回滚正常业务事务。
- 首期实现 MySQL、结构化文件日志、显式 NoOp 和 file spool；预留 ES Store 与 Kafka Transport。
- 为后续业务审计日志定义独立领域契约，复用投递、背压、幂等、retention 与观测底座。

## 范围

**包含：**

- 新建统一 recording runtime 与安全事件发布、存储、查询、队列、retention SPI。
- 新建 MySQL Store 与结构化文件日志 Store module。
- 现有 Feign 上报改为 Transport，不再承担存储选择语义。
- PMS/Member 同一制品支持 `target=local|center`；旧 `mode` 保留一个兼容周期。
- 安全中心 ingest 异步接纳、数据库写入舱壁、真实批量写入和幂等落库。
- `eventId`、优先级、分级可靠性、file spool、重放与可观测。
- 单一权威存储迁移、shadow 对账、旧表只读保留与独立清理。
- 审计模型和 SPI 预留，不接具体业务生产者。

**不包含：**

- ES Store、Kafka Transport 的实际实现与部署。
- 业务审计 producer、审计表、Platform 查询/导出 API 或 UI。
- 风险规则、告警业务、安全大盘、历史跨库回填或对象存储冷归档。
- 登录失败计数与账号锁定状态的存储改造；它们仍属于业务安全状态。
- 本 change 内删除旧 `mode`、`account_security_event` 或遗留兼容代码。

## 分 Phase 交付

| Phase | 内容 | 前置门禁 | 状态 |
|---|---|---|---|
| [01](./phases/01-contract-runtime.md) | 统一契约、dispatcher、配置与兼容层 | Change approved | completed |
| [02](./phases/02-storage-delivery.md) | MySQL/日志 Store、file spool、查询与 retention | Phase 01 | completed |
| [03](./phases/03-consumer-migration.md) | PMS/Member/Gateway/安全中心迁移与 shadow 对账 | Phase 02 | completed |
| [04](./phases/04-validation-cutover.md) | 故障压测、单一权威切换、基线更新 | Phase 03 | completed |

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)
- [阶段](./phases/)

## 审阅门禁

- [x] 确认 module 与公共 SPI 命名。
- [x] 确认 `target`、旧 `mode` 映射和单一权威迁移规则。
- [x] 确认事件优先级、file spool 默认参数和失败策略。
- [x] 确认 MySQL schema、查询边界和 retention 默认值。
- [x] 状态改为 `approved` 后方可实施。

## 完成记录

- 完成日期：2026-08-05
- 关联提交或 PR：（待填写）
- 更新的 current capability：
  - `specs/current/framework/security-event-recording/`
  - `specs/current/security/security-event-center/`
- 与原设计的差异：Micrometer 使用 gauge 桥接计数器（非 Counter 类型）；legacy 清理类保留待 breaking change
- 取消原因：
