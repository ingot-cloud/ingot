# 物理下线 account_security_event 表

> 状态：completed

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260811-security-drop-account-security-event` |
| 领域 | `security`（account-protection、security-event-center） |
| 负责人 | jy |
| 创建日期 | 2026-08-11 |
| 完成日期 | 2026-08-11 |

## 背景

[20260806-security-event-legacy-cleanup](../20260806-security-event-legacy-cleanup/README.md) 已删除对 `account_security_event` 的运行时写入与 retention；权威审计表为 canonical `security_event`。本 change 完成旧表物理下线与仓库残留清理。

## 目标

- 在 `ingot_core` / `ingot_member` **物理删除** `account_security_event`。
- 同步仓库基线、模块 DDL、过时脚本与误导性注释。
- 更新 `specs/current`：不再描述「历史只读保留」。

## 范围

### 包含

- migration `013` + rollback（重建空表，不恢复数据）
- `databases/ingot_core.sql` / `ingot_member.sql` 去掉该表
- 删除 `ingot-security-account-adapter` 旁路 `account_security_event.sql`
- 删除过时 shadow 对账脚本
- 修正 `DeleteAccountUseCaseService` 等仍引用旧表名的注释
- 更新 current：`account-protection`、`security-event-center`、`security-event-recording`

### 不包含

- 改名或删除领域对象 `AccountSecurityEvent` / `AccountSecurityEventRecordMapper` / `CompositeSecurityEventPort`
- 修改 `ingot.security.event` / recording SPI
- DROP `account_lock_state`
- 将旧表历史行在线回填至 `security_event`
- 改写已执行的历史 migration `009` 正文

## 破坏性假设

旧表历史行**不再需要在线保留**。若合规需留存，上线前由运维自行 `mysqldump`；本 change 不做回填。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)
- [功能验收清单](./FUNCTIONAL-TEST-CHECKLIST.md)

## 依赖与关系

- 前置：已归档 [20260806-security-event-legacy-cleanup](../20260806-security-event-legacy-cleanup/README.md)（停写旧表）。
- 与 [20260811-security-event-edge-dedup-lock-shortcut](../../active/20260811-security-event-edge-dedup-lock-shortcut/README.md) 正交。

## 完成记录

- 完成日期：2026-08-11
- 关联提交或 PR：（随代码一并提交）
- 更新的 current capability：
  - `specs/current/security/account-protection/`
  - `specs/current/security/security-event-center/`
  - `specs/current/framework/security-event-recording/`
- 与原设计的差异：无
- 取消原因：
