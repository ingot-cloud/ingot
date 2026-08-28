# 平台优化路线图

> 跟踪 Ingot 平台级能力演进与持续优化。条目进入开发时链接 `specs/changes/active/` change；完成后链接 `specs/current/` 或 `docs/modules/`。

**最后 review**：2026-08-28

## 状态说明

| 状态 | 含义 |
|------|------|
| `done` | 已验收上线 |
| `implementing` | 正在开发 |
| `in-spec` | 规格已编写，待或正在评审 |
| `planned` | 已纳入路线，尚未启动规格 |
| `cancelled` | 已取消（保留记录） |

## 阶段定义

| 阶段 | 含义 |
|------|------|
| **Now** | 当前迭代或近期必须完成 |
| **Next** | 下一里程碑 |
| **Later** | 中长期规划 |

---

## Now — 当前重点

| ID | 主题 | 动机 | 状态 | 关联 |
|----|------|------|------|------|
| R-2026-001 | 微服务性能与资源规划 | 8C16G 单机多实例、1000 并发场景稳定性 | done | [优化总览](../guides/performance/OPTIMIZATION-SUMMARY.md) · [资源规划](../guides/performance/MICROSERVICES-RESOURCE-PLANNING.md) |
| R-2026-002 | 服务假死排查体系 | 生产故障快速定位与应急 | done | [排查指南](../guides/troubleshooting/TROUBLESHOOTING-SERVICE-HANG.md) · [快速参考](../guides/troubleshooting/QUICK-REFERENCE-TROUBLESHOOTING.md) |
| R-2026-003 | 应用中心化授权 | 多租户 SaaS 权限模型统一 | done | [spec](../../specs/current/pms/application-authorization/) · [PMS 模块](../modules/pms/FUNCTION.md) |
| R-2026-004 | JWK 密钥管理与多密钥验签 | JWT 密钥轮换与安全 | done | [spec](../../specs/current/security/jwk-management/) · [JWK 配置](../modules/authorization-server/JWK-CONFIGURATION.md) |
| R-2026-005 | 传输层信封加密 | API 敏感数据 HYBRID 加密与防重放 | done | [spec](../../specs/current/security/transport-crypto/) |
| R-2026-006 | 网关 Header 约定 | 内部头命名与安全约束统一 | done | [spec](../../specs/current/gateway/header-conventions/) |
| R-2026-007 | 网关限流与安全策略 | 动态限流、黑白名单、挑战执行面 | done | [模块文档](../modules/security-center/GATEWAY-RATE-LIMIT.md) · [E2E 用例](../../test-case/security-policy-e2e.md) |
| R-2026-024 | 账号保护全用户闭环 | Member 登录失败锁定、安全事件与 lockout 策略土台 | done | [spec](../../specs/current/security/account-protection/) · [change](../../specs/changes/archive/2026/20260724-security-account-protection/) |

---

## Next — 下一里程碑

| ID | 主题 | 动机 | 状态 | 关联 |
|----|------|------|------|------|
| R-2026-010 | Spring Framework 6.x 全栈对齐 | 基线升级、`-parameters` 编译与废弃 API 清理 | planned | [升级指南](../guides/upgrade/Upgrading-to-Spring-Framework-6.x.md) |
| R-2026-011 | OSS 模块统一抽象完善 | 多存储后端扩展与迁移体验 | planned | [架构](../modules/object-storage-service/oss-architecture.md) · [重构总结](../modules/object-storage-service/oss-refactoring-summary.md) |
| R-2026-012 | TSS 任务调度框架推广 | 统一 XXL-JOB 集成与使用规范 | planned | [架构](../modules/task-scheduler-system/tss-architecture.md) · [实施总结](../modules/task-scheduler-system/tss-implementation-summary.md) |
| R-2026-013 | 字典 extra 字段团队约定 | 避免各开发者随意命名扩展字段 | planned | [使用指南](../modules/dict/USAGE.md) |
| R-2026-014 | Docker 多环境构建标准化 | 简化 CI/CD 与多环境镜像管理 | planned | [部署指南](../guides/deployment/DOCKER-MULTI-ENVIRONMENT.md) · [Dockerfile 迁移](../guides/deployment/DOCKERFILE-MIGRATION-GUIDE.md) |
| R-2026-025 | 统一安全事件中心 | 跨模块安全事实统一模型与中心入库，支撑后续安全概览 | done | [change](../../specs/changes/archive/2026/20260729-security-event-center/) · [current](../../specs/current/security/security-event-center/) · [roadmap](./themes/security-center-roadmap.md) |
| R-2026-026 | 访问防护补全 | 网关策略执行面收口、Sentinel 统一限流、3.1 四维度防爆破与 remote 弹性 | done | [change](../../specs/changes/archive/2026/20260729-security-access-protection/) · [current](../../specs/current/security/access-protection/) · [roadmap](./themes/security-center-roadmap.md) |
| R-2026-027 | 统一分层缓存框架 | 消除 credential/gateway/access/dict 四处重复的分层缓存与降级实现，补齐 TTL 兜底与共享快照层 | done | [spec](../../specs/current/framework/layered-cache/) · [change](../../specs/changes/archive/2026/20260730-framework-layered-cache/) |
| R-2026-028 | 会话安全（L5） | 在线会话 sid 模型、强制下线彻底性、安全中心管理面、并发策略可降级 | done | [change](../../specs/changes/archive/2026/20260817-security-session-safety/) · [current](../../specs/current/security/session-safety/) · [roadmap](./themes/security-center-roadmap.md) |
| R-2026-029 | 挑战验证（L6） | 图形/滑块验证码以 412 + PassToken 接入登录与敏感接口；触发策略 remote/local | done | [change](../../specs/changes/archive/2026/20260827-security-challenge-verification/) · [current](../../specs/current/security/challenge-verification/) · [roadmap](./themes/security-center-roadmap.md) |

---

## Later — 中长期

| ID | 主题 | 动机 | 状态 | 关联 |
|----|------|------|------|------|
| R-2026-020 | 可观测性增强 | 统一 metrics / tracing / 告警规则 | planned | [排查指南 § 监控](../guides/troubleshooting/TROUBLESHOOTING-SERVICE-HANG.md) |
| R-2026-021 | Token 与会话优化 | 降低 Redis 压力、提升在线用户查询效率。**不得打断 JWT 瘦身后的 InUser 补全**，约束见下节 | planned | [Token 优化](../modules/authorization-server/TOKEN-OPTIMIZATION-GUIDE.md) · [L5 会话安全](../../specs/current/security/session-safety/) · [change](../../specs/changes/archive/2026/20260817-security-session-safety/) |
| R-2026-022 | 凭证安全策略扩展 | 更多租户级策略模板与审计 | planned | [模块文档](../modules/credential-security/) |
| R-2026-023 | 社交登录能力扩展 | 除微信外更多 OAuth 提供商 | planned | [社交模块](../modules/social/) |

### R-2026-021 开工约束（写 spec 前必读）

当前资源服务器还原登录身份的契约是：**JWT 只带定位字段，完整权限从会话补全**。

- JWT 瘦身：claims 只保留 `sid` / `userId` / `tenantId` / `scope` 等定位信息，不携带完整 `authorities`。
- 补全入口：`JwtInUserConverter` 按 `sid` 读 `token:sid:{sid}`（`OnlineToken`），把会话中的 `authorities`、`userType`、`deptIds` 与 JWT `scope` 合并后构建 `InUser`。业务微服务从 `SecurityAuthContext` 拿到的角色/权限依赖这一步，而不是 JWT 自身。
- **允许**：换权限存放位置（例如按用户/角色做共享缓存，会话只存引用或版本号），以降低每会话拷贝一份权限列表带来的 Redis 内存压力。
- **不允许**：从 `OnlineToken` 删掉 `authorities`（或停止在 Converter 里合并）却不提供等价补全路径。那样 JWT 瘦身仍然成立，但各微服务拿到的 `InUser` 会丢失角色与细粒度权限。
- 开工时必须把「Converter 构建的 `InUser` 仍含完整 authorities / userType / deptIds」写成验收标准；实现偏离须先改 spec 再改代码。

该约束来自 L5 会话安全实施期间的评审（2026-08-20），避免 021 被理解成「把会话主数据再瘦一刀」而破坏现网授权还原。

---

## 已完成归档（摘要）

以下能力已验收，详细规格见 `specs/current/` 或模块文档：

- PMS 应用中心化授权（2026-06）
- 账号保护全用户闭环 + lockout 策略土台（2026-07）
- 会话安全：sid 模型、强制下线彻底性、安全中心管理面、并发策略可降级（2026-08）
- JWK 密钥管理、传输层加密、网关 Header 约定
- 网关限流与安全策略执行面
- 微服务性能优化与故障排查文档体系（2025-12）

---

## 如何新增路线项

1. 在本文档对应阶段表格追加一行，分配 `R-YYYY-NNN` ID。
2. 在 [VISION.md](./VISION.md) 中确认与产品目标一致。
3. 开工前在 `specs/changes/active/` 创建 change，更新状态为 `in-spec` / `implementing`。
4. 验收后状态改为 `done`，链接 `specs/current/` 并更新 [modules/](../modules/) 文档。

跨域主题可同时在 [themes/](./themes/) 下维护专题说明。
