# 安全事件 Legacy 清理

> 状态：completed

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260806-security-event-legacy-cleanup` |
| 领域 | `security` / `framework` |
| 负责人 | jy |
| 创建日期 | 2026-08-06 |
| 完成日期 | 2026-08-11 |

## 背景

recording pipeline 已上线，旧 L3 `mode=local|remote`、双写 `account_security_event`、`AsyncSecurityEventReporter` 等未上生产，可一次性删除。

## 范围

- 删除 legacy port / retention / 中心直写 Service
- `mode`/`async` → 仅 `target`/`delivery.memory`
- Nacos 样例改为 `target` + `primary-store`
- `InnerSecurityPolicyAPI.reportBlacklist` 走 admission

## 追加（配置类合并）

- 删除 `ingot-security-api` 冗余 `SecurityEventProperties` 绑定
- 唯一配置类：`com.ingot.framework.security.recording.config.SecurityEventProperties`
- 样例：[`example.yml`](../../../../../ingot-framework/ingot-security/ingot-security-recording/example.yml)

## 不在范围

- `account_security_event` 表物理删除（历史只读保留）
- ES/Kafka Store

## 验收

见 [FUNCTIONAL-TEST-CHECKLIST.md](./FUNCTIONAL-TEST-CHECKLIST.md)（功能测试清单项均已通过）。

## 完成记录

- 完成日期：2026-08-11
- 关联提交或 PR：（随代码一并提交）
- 更新的 current capability：
  - `specs/current/framework/security-event-recording/`
  - `specs/current/security/security-event-center/`
  - `specs/current/security/account-protection/`（事件落库路径增量）
- 与原设计的差异：
  - Retention 让步探测改为分别注入 `MemoryRecordQueue` / `FileSpoolRecordQueue`，避免双 `RecordQueue` Bean 导致 `NoUniqueBeanDefinitionException`
  - 网关限流升级事件类型现网为 `RATE_LIMIT_VIOLATION`（边沿去重另见 [20260811-security-event-edge-dedup-lock-shortcut](../20260811-security-event-edge-dedup-lock-shortcut/README.md)）
