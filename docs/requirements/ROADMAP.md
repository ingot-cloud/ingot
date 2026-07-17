# 平台优化路线图

> 跟踪 Ingot 平台级能力演进与持续优化。条目进入开发时链接 `specs/changes/active/` change；完成后链接 `specs/current/` 或 `docs/modules/`。

**最后 review**：2026-07-17

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

---

## Next — 下一里程碑

| ID | 主题 | 动机 | 状态 | 关联 |
|----|------|------|------|------|
| R-2026-010 | Spring Framework 6.x 全栈对齐 | 基线升级、`-parameters` 编译与废弃 API 清理 | planned | [升级指南](../guides/upgrade/Upgrading-to-Spring-Framework-6.x.md) |
| R-2026-011 | OSS 模块统一抽象完善 | 多存储后端扩展与迁移体验 | planned | [架构](../modules/object-storage-service/oss-architecture.md) · [重构总结](../modules/object-storage-service/oss-refactoring-summary.md) |
| R-2026-012 | TSS 任务调度框架推广 | 统一 XXL-JOB 集成与使用规范 | planned | [架构](../modules/task-scheduler-system/tss-architecture.md) · [实施总结](../modules/task-scheduler-system/tss-implementation-summary.md) |
| R-2026-013 | 字典 extra 字段团队约定 | 避免各开发者随意命名扩展字段 | planned | [使用指南](../modules/dict/USAGE.md) |
| R-2026-014 | Docker 多环境构建标准化 | 简化 CI/CD 与多环境镜像管理 | planned | [部署指南](../guides/deployment/DOCKER-MULTI-ENVIRONMENT.md) · [Dockerfile 迁移](../guides/deployment/DOCKERFILE-MIGRATION-GUIDE.md) |

---

## Later — 中长期

| ID | 主题 | 动机 | 状态 | 关联 |
|----|------|------|------|------|
| R-2026-020 | 可观测性增强 | 统一 metrics / tracing / 告警规则 | planned | [排查指南 § 监控](../guides/troubleshooting/TROUBLESHOOTING-SERVICE-HANG.md) |
| R-2026-021 | Token 与会话优化 | 降低 Redis 压力、提升在线用户查询效率 | planned | [Token 优化](../modules/authorization-server/TOKEN-OPTIMIZATION-GUIDE.md) |
| R-2026-022 | 凭证安全策略扩展 | 更多租户级策略模板与审计 | planned | [模块文档](../modules/credential-security/) |
| R-2026-023 | 社交登录能力扩展 | 除微信外更多 OAuth 提供商 | planned | [社交模块](../modules/social/) |

---

## 已完成归档（摘要）

以下能力已验收，详细规格见 `specs/current/` 或模块文档：

- PMS 应用中心化授权（2026-06）
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
