# 产品愿景与原始需求

> Ingot 是一个面向 SaaS 与多租户场景的企业级微服务平台，提供统一的认证授权、权限管理、安全策略与基础能力框架。

## 1. 背景与问题

企业在构建 B 端 SaaS 或内部管理平台时，通常需要重复实现：

- 多租户隔离与租户级配置
- 统一认证（OAuth2）与细粒度权限控制
- 网关层安全策略（限流、黑白名单、挑战）
- 字典、对象存储、任务调度等通用基础能力
- 可观测、可排障的生产运维体系

Ingot 将这些能力沉淀为可复用的微服务与框架模块，降低业务系统的重复建设成本。

## 2. 目标用户与场景

| 用户 | 典型场景 |
|------|----------|
| 平台管理员 | 管理租户、应用授权、全局安全策略 |
| 租户管理员 | 在本租户内配置角色、权限、组织架构 |
| 业务开发者 | 基于 Ingot 框架快速构建业务微服务 |
| 运维工程师 | 部署、监控、性能调优与故障排查 |

## 3. 核心能力清单

### 3.1 多租户与权限（PMS）

- **应用中心化授权**：以应用为资源归属与租户授权边界；菜单负责导航，权限负责访问能力。
- **菜单与权限模型**：菜单分目录、菜单、按钮；支持应用（App）级授权，租户管理员可细粒度分配。
- **角色与数据权限**：角色分配功能权限；支持全部、自定义、本部门及子部门、本部门、本人等数据范围。
- **租户隔离**：请求头携带租户信息；支持默认租户与 MyBatis-Plus 数据隔离。

详见 [modules/pms/](../modules/pms/)、[specs/current/pms/application-authorization/](../../specs/current/pms/application-authorization/)。

### 3.2 认证与安全

- **OAuth2 授权服务器**：标准 OAuth2 / OIDC 流程，JWT 签发与验签。
- **JWK 密钥管理**：多密钥轮换与验签选择。
- **传输层加密**：HYBRID 信封加密与防重放。
- **凭证安全**：多租户密码策略、策略加载与 RPC/REST 接口。
- **网关安全策略**：限流、黑白名单、挑战与动态配置。
- **BFF 设备指纹**：前端请求携带设备指纹 Header，增强会话安全。

详见 [modules/authorization-server/](../modules/authorization-server/)、[modules/credential-security/](../modules/credential-security/)、[modules/security-center/](../modules/security-center/)。

### 3.3 基础能力框架

- **字典模块**：三级缓存 + Redis 跨节点失效总线。
- **对象存储**：统一 OSS 抽象，支持 MinIO 等后端，可扩展新存储实现。
- **任务调度（TSS）**：统一任务调度框架，支持 XXL-JOB 等集成。
- **社交登录**：微信等第三方登录，与字典共用 InvalidationBus 约定。

详见 [modules/dict/](../modules/dict/)、[modules/object-storage-service/](../modules/object-storage-service/)、[modules/task-scheduler-system/](../modules/task-scheduler-system/)、[modules/social/](../modules/social/)。

### 3.4 生产就绪

- 微服务多实例共存的资源规划与 JVM / 连接池 / 网关限流优化。
- 服务假死排查流程、自动诊断脚本与配置示例。
- Docker 多环境构建与部署指南。

详见 [guides/performance/](../guides/performance/)、[guides/troubleshooting/](../guides/troubleshooting/)、[guides/deployment/](../guides/deployment/)。

## 4. 约束与非目标

**约束**：

- Java 17+，Spring Cloud 2024.x 技术栈。
- 多租户场景下数据隔离为默认要求。
- 公共接口变更需遵循 SDD 流程（specs/changes）。

**非目标**：

- 不提供通用低代码 / 表单设计器。
- 不替代专业 IAM 产品的全部企业级功能（如复杂审批流、SOX 合规套件）。
- 不在本仓库维护前端 UI 组件库（仅提供 BFF 与 API 契约）。

## 5. 成功指标

| 指标 | 目标 |
|------|------|
| 单机部署 | 8C16G 可稳定运行 4–5 个核心微服务 |
| 并发能力 | 支撑约 1000 并发用户（配合资源规划与限流） |
| 租户隔离 | 跨租户数据零泄漏 |
| 故障恢复 | 提供 30 秒内可执行的诊断路径 |
| 变更可追溯 | 业务行为变更均可在 specs 中找到对应 change |

---

**维护者**：ingot-cloud 团队  
**最后更新**：2026-07-17
