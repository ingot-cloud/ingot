# 功能验收清单：物理下线 account_security_event

> Change：`20260811-security-drop-account-security-event`  
> 前置：已执行 migration `012`（存在 `security_event`）；已部署 legacy cleanup 后的 recording 路径；准备执行 / 已执行 `013`。

## 1. DDL

| # | 场景 | 预期 |
|---|------|------|
| E1 | 在 `ingot_core` 执行 `SHOW TABLES LIKE 'account_security_event'` | 无结果 |
| E1b | 在 `ingot_member` 同上 | 无结果 |
| E1c | `account_lock_state`、`security_event` 仍存在 | 两库均存在 |

## 2. 运行时事件

| # | 场景 | 预期 |
|---|------|------|
| E2 | ADMIN 错密 / 正确登录各 1 次 | `ingot_core.security_event` 有对应行；无任何对已删表的 SQL 错误 |
| E3 | APP（Member）错密或正确登录 1 次 | `ingot_member.security_event` 有对应行 |

## 3. 仓库残留

| # | 检查 | 预期 |
|---|------|------|
| E4 | 模块无 `AccountSecurityEventEntity`、旧 `AccountSecurityEventMapper`、旁路 `sql/account_security_event.sql` | 均不存在（仓库侧已勾） |
| E4b | `databases/scripts/security_event_shadow_reconcile.sql` | 已删除（仓库侧已勾） |
| E4c | `databases/ingot_core.sql` / `ingot_member.sql` | 无该表 CREATE/INSERT（仓库侧已勾） |

## 4. 回滚演练（可选，非生产）

| # | 场景 | 预期 |
|---|------|------|
| E5 | 对测试库执行 `rollback_013` | 空表重建成功；服务仍只写 `security_event` |

## 签核

| 角色 | 姓名 | 日期 | 结果 |
|---|---|---|---|
| 开发 | jy | 2026-08-11 | PASS（归档） |
| QA | | | |
