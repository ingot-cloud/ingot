# 统一安全事件中心（L4）

> 能力域：`security` / `security-event-center`

## 摘要

在 L3 统一契约与中心入库基础上，完成 **recording 管道迁移**：Publisher 分级投递、中心 admission、MySQL 权威 Store、Feign Transport 与 shadow 对账能力。

- **统一发布**：PMS/Member/Gateway 经 `SecurityEventPublisher` + `target` 路由。
- **中心 ingest**：`InnerSecurityEventAPI` → admission 队列/spool → 异步批量写入。
- **账号域**：`CompositeSecurityEventPort`；legacy `mode=remote` 迁移期双写 `account_security_event` + canonical Store shadow。
- **网关 ACCESS**：`BlacklistEventReporter` → `SecurityEventReportPublisher`；默认 `target=center`。
- **Retention**：canonical 表由 `PurgeCanonicalSecurityEventTask`；legacy 表任务保留至切换完成。

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
- 来源变更：`specs/changes/archive/2026/20260804-security-event-storage-pipeline/`
