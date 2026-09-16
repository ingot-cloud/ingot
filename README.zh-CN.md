# Ingot

[English](./README.md) | [简体中文](./README.zh-CN.md)

Ingot 是一个面向 SaaS 与多租户应用的企业级微服务平台。项目提供认证与权限管理、租户隔离、安全策略、数据访问、缓存、对象存储、任务调度等可复用的服务和框架模块。

项目主要面向基于 Spring 生态构建业务系统的团队，旨在提供一致的安全模型与面向生产环境的基础能力，减少各业务服务重复建设通用基础设施的成本。

## 核心特性

- **认证与权限管理**：提供 OAuth 2.0/OIDC 授权、JWT 与 JWK 管理、应用授权、角色、权限和数据权限能力。
- **多租户能力**：支持租户上下文传递，以及基于 MyBatis-Plus 的 SaaS 数据隔离。
- **安全基础设施**：涵盖凭证策略、传输加密、防重放、账户保护、网关访问策略和安全事件记录。
- **可复用基础能力**：提供分层缓存、字典、事件驱动失效、对象存储、分布式 ID、验证码、社交登录和任务调度组件。
- **云原生服务**：包含 API 网关、授权服务器、BFF、权限管理、会员管理和安全中心等服务。
- **生产运维支持**：提供 Docker Compose 与 Docker Swarm 部署资源、多环境 Nacos 配置、数据库迁移、性能优化和故障排查文档。

## 项目架构

| 区域 | 模块 | 用途 |
| --- | --- | --- |
| 云服务 | `ingot-service` | 网关、认证、BFF、权限、会员、安全中心及测试服务 |
| 基础框架 | `ingot-framework` | 安全、数据、缓存、租户、对象存储、OpenAPI、任务调度和集成组件 |
| Gradle 插件 | `ingot-plugin` | MyBatis-Plus 与服务装配相关的构建约定 |
| 配置 | `nacos` | 按环境维护的服务配置与公共配置 |
| 数据库 | `databases` | 基础结构、初始化数据、迁移和回滚脚本 |
| 部署 | `deploy` | 中间件与应用服务部署资源 |
| 文档 | `docs` | 产品需求、模块说明、运维指南和工程规范 |
| 规格 | `specs` | SDD 工作流中的活动变更与已验收行为基线 |

### 主要服务

| 服务 | 职责 |
| --- | --- |
| `ingot-gateway` | 对外 API 网关与网关层安全策略执行 |
| `ingot-auth` | OAuth 2.0/OIDC 授权与令牌服务 |
| `ingot-bff` | 面向前端的认证与会话流程 |
| `ingot-iam-provider` | 权限、角色、应用、组织和租户管理 |
| `ingot-member-provider` | 会员与用户领域能力 |
| `ingot-security-provider` | 集中的安全策略与安全事件能力 |

## 技术栈

| 组件 | 版本或选型 |
| --- | --- |
| Java | 21 |
| Gradle | 8.12.1（包含 Wrapper） |
| Spring Boot | 3.5.7 |
| Spring Cloud | 2025.0.0 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| Spring Authorization Server | 1.5.3 |
| MyBatis-Plus | 3.5.10.1 |
| 服务发现与配置中心 | Nacos |
| 持久化与缓存 | MySQL、Redis、Caffeine |

依赖版本以 [`versions.gradle`](./versions.gradle) 中的配置为准。

## 快速开始

### 环境要求

- JDK 21
- Git
- Docker 与 Docker Compose（运行项目提供的中间件或容器部署方案时需要）

### 克隆并构建

```bash
git clone https://github.com/ingot-cloud/ingot.git
cd ingot
./gradlew clean build
```

查看全部模块和 Gradle 任务：

```bash
./gradlew projects
./gradlew tasks
```

### 本地运行

服务运行依赖 MySQL、Redis 和 Nacos，并需要准备相应的数据库与配置数据：

- 中间件定义：[`deploy/middleware/standalone`](./deploy/middleware/standalone/)
- 数据库结构与迁移：[`databases`](./databases/)
- Nacos 配置集：[`nacos`](./nacos/)

依赖准备完成后，可以通过 Gradle 运行单个服务。例如：

```bash
./gradlew :ingot-service:ingot-gateway:bootRun
```

如需使用容器部署应用服务，请查看[部署指南](./deploy/services/README.md)。在本地环境之外使用仓库提供的配置前，请检查并替换其中的示例凭证、密钥、网络配置和存储路径。

## 文档导航

- [文档中心](./docs/README.md)：产品方向、模块文档、部署、性能和故障排查
- [产品愿景](./docs/requirements/VISION.md)：项目目标、用户、核心能力和非目标
- [模块文档](./docs/modules/README.md)：按模块组织的实现与使用说明
- [运维指南](./docs/guides/README.md)：部署、配置、性能、升级和故障排查
- [当前规格](./specs/current/README.md)：已验收的系统行为
- [Spec-Driven Development 工作流](./specs/README.md)：贡献和变更管理流程

## 开发流程

本仓库对影响业务行为、数据模型、公共接口或部署兼容性的变更采用 Spec-Driven Development。实施此类变更前，请阅读 [SDD 工作流](./specs/README.md)，检索当前规格与活动变更，并确保对应的 change 已通过审批。

常用验证命令：

```bash
./gradlew test
./gradlew clean build
```

## 开源协议

Ingot 基于 [Apache License 2.0](./LICENSE) 开源。
