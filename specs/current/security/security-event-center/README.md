# 统一安全事件中心

> 能力域：`security` / `security-event-center`

## 摘要

在统一契约与中心入库基础上，完成 **recording 管道** 与 **legacy 清理**：Publisher 分级投递、中心 admission、MySQL 权威 Store、Feign Transport。

- **统一发布**：PMS/Member/Gateway 经 `SecurityEventPublisher` + `target` 路由（无旧 `mode=local|remote`）。
- **中心 ingest**：`InnerSecurityEventAPI` → admission 队列/spool → 异步批量写入；`InnerSecurityPolicyAPI.reportBlacklist` 经同一 admission。
- **账号域**：`CompositeSecurityEventPort` → 统一 Publisher；仅写 canonical `security_event`（legacy `account_security_event` 已 DROP）。
- **网关 ACCESS**：`BlacklistEventReporter` → `SecurityEventReportPublisher`；默认 `target=center`。
- **Retention**：仅 `PurgeCanonicalSecurityEventTask` 清理 canonical 表。

## 关联模块

| 职责 | 路径 |
|---|---|
| Wire DTO / Feign | `ingot-security-api` |
| 中心 admission | `ingot-security-provider/.../admission/SecurityEventAdmissionService.java` |
| 账号组合 Port | `ingot-security-account-adapter/.../port/CompositeSecurityEventPort.java` |
| Feign Transport | `ingot-security-event-transport-feign` |
| MySQL Store | `ingot-security-event-store-mysql` |
| 框架运行时 | `ingot-security-recording` |

## 文档索引

- [SPEC](./SPEC.md)：配置、拓扑、链路与切换
- Recording 框架：[../../framework/security-event-recording/SPEC.md](../../framework/security-event-recording/SPEC.md)
- 来源变更：
  - `specs/changes/archive/2026/20260804-security-event-storage-pipeline/`
  - `specs/changes/archive/2026/20260806-security-event-legacy-cleanup/`
  - `specs/changes/archive/2026/20260811-security-drop-account-security-event/`（legacy 表 DROP）
