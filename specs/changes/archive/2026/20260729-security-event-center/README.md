# 统一安全事件中心（L3）

> 状态：completed（已验收，已更新 current，已归档）

## 元数据

| 项 | 值 |
|---|---|
| Change ID | `20260729-security-event-center` |
| 领域 | `security` |
| 负责人 | jy |
| 创建日期 | 2026-07-29 |
| 目标发布日期 | TBD |
| Roadmap | [安全中心分阶段落地 Roadmap](../../../../docs/requirements/themes/security-center-roadmap.md) · L3 |
| 需求来源 | [安全中心服务需求 §九](../../../../docs/requirements/themes/security-center-service.md) |

## 目标

在 `ingot-security` 侧建立**统一安全事件模型与中心化存储**，并打通现有事件生产者的上报链路，形成阶段一 L3 最小闭环：

1. **统一契约**：在 `ingot-security-api` 定义跨模块共享的事件 DTO 与类型枚举，对齐需求第九章核心字段。
2. **中心存储**：`ingot_security.security_event` 表 + 内网入库 API。
3. **账号域接入**：`SecurityEventPort` 组合实现——`mode=remote` 时本地 `account_security_event` 双写并异步转发安全中心。
4. **网关接入**：封禁/限流违规审计改走统一上报 API，停止向 `gateway_blacklist_event` 写入新数据。

**核心原则**（承接 L1/L2 与 roadmap 横切约定）：

- **本地审计不丢**：`mode=remote` 时仍双写本地，中心库为聚合副本。
- **主链路不阻塞**：Feign 上报异步执行，失败仅 warn，不影响登录/锁定/封禁主流程。
- **最小闭环**：本期只做 ingest + storage，不做 Platform 查询 API 与大盘聚合。

## 依赖

| 前置闭环 | 说明 |
|----------|------|
| L1 凭证安全收口 | `20260717-security-credential-closure`；凭证类事件（PASSWORD_*）已由账号域发布 |
| L2 账号保护全用户闭环 | `20260724-security-account-protection`；AUTH/ACCOUNT 类事件与 `SecurityEventPort` 已覆盖 ADMIN + Member |

## 范围

**包含：**

- `ingot-security-api`：`SecurityEventReportDTO`、统一 `SecurityEventType` / `SecurityEventCategory` 枚举、`RemoteSecurityEventService` Feign。
- `ingot-security-provider`：`security_event` 表 migration、Entity/Mapper/Service、`InnerSecurityEventAPI` 入库。
- `ingot-security-account-adapter`：`AccountSecurityEvent → SecurityEventReportDTO` 映射、`RemoteSecurityEventPortAdapter`、`CompositeSecurityEventPort`、自动配置与 `ingot.security.event.*` 属性。
- `ingot-gateway`：`BlacklistEventReporter` 改调统一 Feign；`SentinelBlockHandler` DTO 映射。
- Nacos 降级配置：`enabled` / `mode` / 类别开关 + 动态刷新验证说明。
- **过期清理**：本地 `account_security_event` 与中心 `security_event` 定时 retention（可独立配置天数）。
- P0 事件类型：账号域已有 11 种 + 网关 ACCESS 2 种（见 [REQUIREMENTS](./REQUIREMENTS.md)）。

**不包含（非目标）：**

- Platform 分页查询 / 导出 / 管理面 API（阶段二安全概览或独立 change）。
- 安全概览大盘、告警触发、风险规则输入、跨租户聚合统计。
- 历史数据从 `account_security_event` / `gateway_blacklist_event` 回填。
- 补发 LOGOUT、TOKEN_REFRESH、PASSWORD_EXPIRED 等已定义但未接线类型。
- MQ 异步总线、事件订阅推送、读写分离检索优化。
- 安全审计（需求第十章）与风险命中（需求 7.4）——与事件中心职责分离，后续独立闭环。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 决策结论（T0 已闭合）

| ID | 决议 |
|----|------|
| D1 | `SecurityEventType` / `SecurityEventCategory` SoT 在 `ingot-security-api` |
| D2 | 停止向 `gateway_blacklist_event` 写入；旧表与 GET `/events` 只读保留 |
| D3 | `mode=remote` 始终双写本地 `account_security_event` |
| D4 | migration 编号 `010` |
| D5 | security 未部署 / Feign 不可用 → 静默跳过，等同 local |
| D6 | `reportBlacklist` 转调 `SecurityEventService` 统一入库 |

## 完成记录

- 完成日期：代码完成 2026-07-29；验收通过 2026-07-29
- 关联提交或 PR：`64c3eda0`（refactor: 统一安全事件上报）及同系列提交
- 更新的 current capability：`specs/current/security/security-event-center/`（README + SPEC，新建）
- 与原设计的差异：
  - **`enabled` 语义收紧**：`enabled=false` 时本地与中心均不上报（原 REQUIREMENTS S5 写「本地仍写入」，实施中改为总开关，已写入 current SPEC）。
  - **远程上报**：由无界单线程池改为 `AsyncSecurityEventReporter` 有界队列 + 攒批 + 满队列丢弃，防 OOM。
  - **`CompositeSecurityEventPort` 始终装配**：`remotePort` 可空；enabled/mode 判断集中在一处。
  - **Composite Port Bean 修复**：`DefaultSecurityEventPortAdapter` 不可单独 `@Bean`，避免 remote 双写失效。
  - V1–V7 验收以手工集成 / DB 直查 / migration 执行为准；P2 单元测试未全量补齐（记入 current 已知限制）。
- 取消原因：—

## 后续跟踪（拆出为新 change）

1. **Platform 读侧**：安全事件分页查询 / 导出 API（阶段二安全概览前置）。
2. **历史回填**：`account_security_event` / `gateway_blacklist_event` → `security_event`（若合规需要）。
3. **dead-letter / 重试**：远程队列丢弃事件的补偿策略（按运营需求评估）。
