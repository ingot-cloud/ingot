# Requirements

## 用户场景

### US-1 框架与业务共用同一 code

- **触发**：新增或调整安全事件类型 code（例如既有 `ACCOUNT_LOCKED`）。
- **期望**：code 字符串只在 `ingot-security-event-codes` 定义一次；`SecurityEventType` 枚举与 `DefaultPriorityClassifier` 均引用该常量，避免「枚举有、优先级表漏」或反向漂移。

### US-2 账号域发布事件不再维护第二套枚举

- **触发**：account-core 用例 `publishEvent`（锁定、启禁、密码变更等）。
- **期望**：使用 `com.ingot.cloud.security.api.model.enums.SecurityEventType`（或其 code）；仓库中 **不再存在** account-core 包内的 `SecurityEventType` 枚举。

### US-3 recording 分层不破坏

- **触发**：仅引入 `ingot-security-recording` 的进程（无 Feign / 无 security-api）。
- **期望**：recording 仅依赖 `event-codes`（及既有 Spring/Jackson），**不**依赖 `ingot-security-api` 或 account-core；未知 eventType 仍默认 BEST_EFFORT。

## 业务规则

### R1 SoT 分层

| 层 | 职责 | 模块 |
|----|------|------|
| Code 字面量 | `public static final String` 唯一来源 | `ingot-security-event-codes` |
| 类型安全枚举（wire / 校验） | 枚举值 + `fromCode`；admission / Feign DTO | `ingot-security-api` |
| 默认优先级表 | 引用 code 常量做 switch / 集合 | `ingot-security-recording` |
| 账号域发布 | 使用 api 枚举（或 `.getCode()` 入 recording） | `ingot-security-account-core` |

### R2 禁止的依赖方向

- `ingot-security-recording` → `ingot-security-api`：**禁止**
- `ingot-security-recording` → `ingot-security-account-*`：**禁止**
- `ingot-security-event-codes` → 上述业务/api 模块：**禁止**（仅 JDK / 必要时极薄 commons）

### R3 兼容

- 线上已落库 / 已在途的 eventType **字符串不变**（常量值与现网 code 逐字相同）。
- 不改 `SecurityEventReportDTO` 字段形态；中心 `fromCode` 行为保持。

### R4 与边沿语义正交

- 本 change **不**改变边沿 / 电平触发规则（见 [20260811 R1.1](../20260811-security-event-edge-dedup-lock-shortcut/REQUIREMENTS.md)）。
- 优先级默认表归属（哪些 BEST_EFFORT / DURABLE）与现网 [`DefaultPriorityClassifier`](../../../../../ingot-framework/ingot-security/ingot-security-recording/src/main/java/com/ingot/framework/security/recording/runtime/DefaultPriorityClassifier.java) 一致，仅替换字面量为常量引用。

## 边界与非目标

- **不做** codegen 从枚举生成 classifier。
- **不做**把常量塞进 `ingot-commons`（避免全局污染）。
- **不**借本 change 修改 spool / Store / Transport。
- account-core 对 `ingot-security-api` 的依赖属于「框架账号域消费 wire 类型」的务实选择；若评审否决 domain→api，则备选为 account 只依赖 `event-codes` 并用 String/`AccountSecurityEvent` 自有类型承载——默认方案仍为 **直接用 api 枚举**（access-core 已如此），以结束双枚举。

## 验收标准

- [x] `ingot-security-event-codes` 模块存在且被 recording、api 依赖
- [x] `DefaultPriorityClassifier` 生产路径无裸事件类型字符串字面量（测试 fixture 可用常量）
- [x] api `SecurityEventType` / `SecurityEventCategory` 的 code 来自常量
- [x] 仓库内仅一份 `SecurityEventType` 枚举（api）；account-core 旧枚举已删除，编译与相关单测通过
- [x] recording 模块依赖树不含 `ingot-security-api`
- [x] `specs/current/framework/security-event-recording` 已写明 SoT；本 change 已归档
