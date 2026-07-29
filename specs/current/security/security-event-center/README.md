# 统一安全事件中心（L3）

> 能力域：`security` / `security-event-center`

## 摘要

跨模块安全事件已具备统一契约、中心存储与上报接入，形成阶段一 L3 最小闭环（ingest + storage）：

- **统一契约**：`ingot-security-api` 定义 `SecurityEventReportDTO`、`SecurityEventType` / `SecurityEventCategory` 枚举、`RemoteSecurityEventService` Feign。
- **中心存储**：`ingot_security.security_event` 表 + `InnerSecurityEventAPI` 内网入库（单条与批量）。
- **账号域接入**：`CompositeSecurityEventPort` 按 `ingot.security.event.*` 决定本地 / 中心上报；`mode=remote` 时双写本地并异步转发中心。
- **网关接入**：`BlacklistEventReporter` 经统一 Feign 上报 ACCESS 类事件；停止向 `gateway_blacklist_event` 写入新数据。
- **过期清理**：本地 `account_security_event` 与中心 `security_event` 独立 retention 定时任务。
- **背压保护**：远程上报经 `AsyncSecurityEventReporter` 有界队列 + 攒批，队列满时丢弃并 warn，避免突发 OOM。

## 边界

- **包含**：P0 共 13 种事件类型（AUTH 2 + ACCOUNT 6 + CREDENTIAL 3 + ACCESS 2）的生产者上报与中心入库。
- **不含**：Platform 查询 / 导出 API、安全概览大盘、告警、风险规则、MQ 总线、历史回填、冷归档到对象存储。
- **与 L2 关系**：账号保护 UseCase 与 `SecurityEventPort` 接口不变；本能力在其上增加可选中心转发与统一模型。
- **与审计 / 风险**：安全事件为「系统运行安全事实」；管理员操作审计与风险命中属后续独立闭环。

## 所有者

- 契约与配置 SoT：`ingot-security-api`
- 中心入库：`ingot-security-provider`
- 账号域组合 Port：`ingot-security-account-adapter`
- 网关 ACCESS 上报：`ingot-gateway`

## 关联模块

| 职责 | 路径 |
|---|---|
| 配置属性 | `ingot-security-api/.../config/SecurityEventProperties.java` |
| 异步有界上报 | `ingot-security-api/.../support/AsyncSecurityEventReporter.java` |
| Feign 契约 | `ingot-security-api/.../rpc/RemoteSecurityEventService.java` |
| 中心入库 API | `ingot-security-provider/.../web/inner/InnerSecurityEventAPI.java` |
| 组合 Port | `ingot-security-account-adapter/.../port/CompositeSecurityEventPort.java` |
| 远程 Port | `ingot-security-account-adapter/.../port/RemoteSecurityEventPortAdapter.java` |
| 本地 retention | `ingot-security-account-adapter/.../task/AccountSecurityEventRetentionTask.java` |
| 中心 retention | `ingot-security-provider/.../task/SecurityEventRetentionTask.java` |
| 网关 Reporter | `ingot-gateway/.../security/BlacklistEventReporter.java` |

## 文档索引

- [SPEC](./SPEC.md)：配置语义、数据模型、上报链路、降级与 retention
- 来源变更：`specs/changes/archive/2026/20260729-security-event-center/`
