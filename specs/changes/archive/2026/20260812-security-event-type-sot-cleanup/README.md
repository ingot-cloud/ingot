# 安全事件类型 SoT 收敛与 code 常量解耦

> 状态：completed

## 元数据

- Change ID：`20260812-security-event-type-sot-cleanup`
- 领域：`security`（security-event-recording、security-event-center、account）
- 负责人：jy
- 创建日期：2026-08-12
- 目标发布日期：TBD

## 背景

1. **双份 `SecurityEventType`**：[`ingot-security-api`](../../../../../ingot-service/ingot-security/ingot-security-api/src/main/java/com/ingot/cloud/security/api/model/enums/SecurityEventType.java) 已是跨模块 wire SoT（含 ACCESS / `LOGIN_FAIL_*`），account-core 本地枚举已删除（兑现 archive [20260729-security-event-center](../20260729-security-event-center/DESIGN.md) **D1**）。
2. **recording 硬编码类型字符串**：已改为引用 [`SecurityEventCodes`](../../../../../ingot-framework/ingot-security/ingot-security-event-codes/src/main/java/com/ingot/framework/security/event/codes/SecurityEventCodes.java)。[`DefaultPriorityClassifier`](../../../../../ingot-framework/ingot-security/ingot-security-recording/src/main/java/com/ingot/framework/security/recording/runtime/DefaultPriorityClassifier.java) 仍不依赖 `ingot-security-api`（框架 SPI、`eventType` 为 open String）。
3. 与 [20260811-security-event-edge-dedup-lock-shortcut](../20260811-security-event-edge-dedup-lock-shortcut/README.md) **正交**：边沿去重不依赖类型合并。

## 目标

- 事件 **code 字面量**有唯一轻量来源，供 recording 默认表与 api 枚举共用。
- **生产者侧**仅保留一份类型安全枚举（api SoT）；删除 account-core 重复枚举。
- recording 保持 String + open code + `default → BEST_EFFORT`，**不**引入对 service-api / 业务域的依赖。

## 范围

### 包含

- 新建轻量模块 `ingot-security-event-codes`（仅 `SecurityEventCodes` / `SecurityEventCategoryCodes` 常量）
- `DefaultPriorityClassifier`（及同类 category 硬编码若适用）改引用常量
- `ingot-security-api` 的 `SecurityEventType` / `SecurityEventCategory` 的 `code` 字段引用同一常量
- 删除 account-core `SecurityEventType`；用例与 `AccountSecurityEvent` 改用 api 枚举
- 更新 `specs/current/framework/security-event-recording`：写明 code 常量模块 + api enum SoT

### 不包含

- 修改默认优先级表语义（BEST_EFFORT / DURABLE 归属不变）
- 边沿去重 / 锁定短路（归属 [20260811-...](../20260811-security-event-edge-dedup-lock-shortcut/README.md)）
- recording SPI、Store、Transport 行为变更
- 为每个新事件类型引入「自动同步 classifier」的 codegen（本期手工维护常量 + 枚举 + switch）

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 依赖与关系

- 兑现 archive [20260729-security-event-center](../20260729-security-event-center/README.md) D1。
- 与 [20260811-security-event-edge-dedup-lock-shortcut](../20260811-security-event-edge-dedup-lock-shortcut/README.md) 正交。
- 依赖基线：[security-event-recording](../../../../current/framework/security-event-recording/SPEC.md)。

## 完成记录

- 完成日期：2026-08-22
- 关联提交或 PR：工作区实施（随代码一并提交）
- 更新的 current capability：
  - `specs/current/framework/security-event-recording/`
  - `specs/current/security/security-event-center/README.md`（一句指向 recording SPEC）
  - `specs/current/security/account-protection/README.md`（一句：账号域使用 api 枚举）
- 与原设计的差异：无
- 取消原因：—
