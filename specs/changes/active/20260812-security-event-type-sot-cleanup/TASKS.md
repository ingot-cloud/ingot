# Tasks

> 状态：`draft`。全部 **已定决策** 确认且 change 状态为 `approved` 后方可开始编码。

## 已定决策

| 决策 | 结论 | 依据 |
|------|------|------|
| code 模块 | 新建 `ingot-security-event-codes` | DESIGN D1 |
| 枚举 SoT | 仅保留 `ingot-security-api` | archive D1；DESIGN D2 |
| account-core | 删本地 enum，改用 api | DESIGN D3 |
| recording | 只依赖 codes，不依赖 api | DESIGN D4 |
| 优先级语义 | 不改归属表 | DESIGN D5 |
| 与 edge-dedup | 正交；冲突时后迁枚举 | DESIGN D6 |

## Phase 0 · SDD 与评审

- [x] T0-1：四工件齐备并登记 `specs/README.md` §7
  - 依赖：无
  - 验收：本目录 README/REQUIREMENTS/DESIGN/TASKS 存在；§7 有条目
- [ ] T0-2：评审 → `approved`
  - 依赖：T0-1
  - 验收：README 状态 `approved`；无开放决策

## Phase 1 · code 模块与 recording / api

- [ ] T1-1：创建 `ingot-security-event-codes` + `SecurityEventCodes` / `SecurityEventCategoryCodes` + `ingot.gradle` 登记
  - 依赖：T0-2
  - 验收：常量值与现网 api 枚举 code 逐字一致
- [ ] T1-2：api 枚举改引用常量
  - 依赖：T1-1
  - 验收：`ingot-security-api` 编译；`fromCode` 单测或既有校验通过
- [ ] T1-3：`DefaultPriorityClassifier`（及 `SecurityEventProperties` 类别码若适用）改引用常量
  - 依赖：T1-1
  - 验收：`DefaultPriorityClassifierTest` 通过；无生产路径裸事件类型字面量

## Phase 2 · account-core 去重

- [ ] T2-1：account-core 依赖 api；`AccountSecurityEvent` / 用例 / 测试改用 api `SecurityEventType`
  - 依赖：T1-2
  - 验收：相关模块编译；account-core 测试通过
- [ ] T2-2：删除 account-core `...domain.model.enums.SecurityEventType`
  - 依赖：T2-1
  - 验收：全仓仅 api 一份 `SecurityEventType` 枚举（grep）

## Phase 3 · 基线与验收

- [ ] T3-1：更新 `specs/current/framework/security-event-recording`
  - 依赖：T1–T2
  - 验收：SPEC 写明 code 模块 + api enum SoT + recording 不依赖 api
- [ ] T3-2：归档本 change
  - 依赖：T3-1、验证任务
  - 验收：移入 `specs/changes/archive/2026/`；§7 更新

## 验证任务

- [ ] V1：`ingot-security-event-codes`、`ingot-security-recording`、`ingot-security-api`、`ingot-security-account-core` 及相关 adapter 测试
- [ ] V2：确认 `ingot-security-recording` 依赖树不含 `ingot-security-api`
- [ ] V3：grep 无残留 `com.ingot.framework.security.account.domain.model.enums.SecurityEventType`

## 完成检查

- [ ] 实现与 DESIGN 一致
- [ ] REQUIREMENTS 验收标准全部满足
- [ ] Current 已更新
- [ ] Change 已记录完成信息并归档
