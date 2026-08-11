# 安全事件 Legacy 清理

> 状态：approved | 2026-08-06

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
- 样例：[`example.yml`](../../../ingot-framework/ingot-security/ingot-security-recording/example.yml)

## 不在范围

- `account_security_event` 表物理删除（历史只读保留）
- ES/Kafka Store

## 验收

见 [FUNCTIONAL-TEST-CHECKLIST.md](./FUNCTIONAL-TEST-CHECKLIST.md)
