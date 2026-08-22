# 安全事件类型 SoT 收敛与 code 常量解耦

> 状态：validating

## 元数据

- Change ID：`20260812-security-event-type-sot-cleanup`
- 领域：`security`（security-event-recording、security-event-center、account）
- 负责人：jy
- 创建日期：2026-08-12
- 目标发布日期：TBD

## 背景

1. **双份 `SecurityEventType`**：[`ingot-security-api`](../../../../ingot-service/ingot-security/ingot-security-api/src/main/java/com/ingot/cloud/security/api/model/enums/SecurityEventType.java) 已是跨模块 wire SoT（含 ACCESS / `LOGIN_FAIL_*`），但 [`account-core`](../../../../ingot-framework/ingot-security/ingot-security-account/ingot-security-account-core/src/main/java/com/ingot/framework/security/account/domain/model/enums/SecurityEventType.java) 仍保留本地枚举（缺 ACCESS），与 archive [20260729-security-event-center](../../archive/2026/20260729-security-event-center/DESIGN.md) **D1**「api 为 SoT、account enum 逐步 deprecated」未闭环，存在漂移风险。
2. **recording 硬编码类型字符串**：[`DefaultPriorityClassifier`](../../../../ingot-framework/ingot-security/ingot-security-recording/src/main/java/com/ingot/framework/security/recording/runtime/DefaultPriorityClassifier.java) 用 string switch 维护默认优先级表。`ingot-security-recording` 故意不依赖 `ingot-security-api` / account-core（框架 SPI、`eventType` 为 open String），故无法直接引用枚举——硬编码是分层约束下的现状，而非疏忽。
3. 与 [20260811-security-event-edge-dedup-lock-shortcut](../../archive/2026/20260811-security-event-edge-dedup-lock-shortcut/README.md) **正交**：边沿去重不依赖类型合并；本 change 不阻塞其实施。

## 目标

- 事件 **code 字面量**有唯一轻量来源，供 recording 默认表与 api 枚举共用。
- **生产者侧**仅保留一份类型安全枚举（api SoT）；删除 account-core 重复枚举。
- recording 保持 String + open code + `default → BEST_EFFORT`，**不**引入对 service-api / 业务域的依赖。

## 范围

### 包含

- 新建轻量模块 `ingot-security-event-codes`（仅 `SecurityEventCodes` / `SecurityEventCategoryCodes` 常量）
- `DefaultPriorityClassifier`（及同类 category 硬编码若适用）改引用常量
- `ingot-security-api` 的 `SecurityEventType` / `SecurityEventCategory` 的 `code` 字段引用同一常量
- 删除 account-core `SecurityEventType`；用例与 `AccountSecurityEvent` 改用 api 枚举（或经薄适配一次性迁完）
- 更新 `specs/current/framework/security-event-recording`：写明 code 常量模块 + api enum SoT

### 不包含

- 修改默认优先级表语义（BEST_EFFORT / DURABLE 归属不变）
- 边沿去重 / 锁定短路（归属 [20260811-...](../../archive/2026/20260811-security-event-edge-dedup-lock-shortcut/README.md)）
- recording SPI、Store、Transport 行为变更
- 为每个新事件类型引入「自动同步 classifier」的 codegen（本期手工维护常量 + 枚举 + switch）

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 依赖与关系

- 兑现 archive [20260729-security-event-center](../../archive/2026/20260729-security-event-center/README.md) D1。
- 与 [20260811-security-event-edge-dedup-lock-shortcut](../../archive/2026/20260811-security-event-edge-dedup-lock-shortcut/README.md) 正交，可并行；建议在边沿 change 合入后或并行窗口实施，避免同一批 account-core 文件冲突。
- 依赖基线：[security-event-recording](../../current/framework/security-event-recording/SPEC.md)。

## 完成记录

- 完成日期：
- 关联提交或 PR：
- 更新的 current capability：`framework/security-event-recording`（及如有需要的 security-event-center / account-protection 一句引用）
- 与原设计的差异：
- 取消原因：
