# Ingot 文档中心

> 产品需求、模块说明、运维指南与工程规范

## 文档分类

| 分类 | 目录 | 说明 |
|------|------|------|
| 需求与路线图 | [requirements/](./requirements/) | 原始产品意图、平台优化路线、跨域主题 |
| 模块文档 | [modules/](./modules/) | 已实现模块的架构、用法与 API 参考 |
| 运维与指南 | [guides/](./guides/) | 部署、性能、排障、升级与配置示例 |
| 工程规范 | [standards/](./standards/) | 编码与注释等开发规范 |

### 与 `specs/` 的关系

- **requirements/**：回答「为什么做、做什么、优先级如何」——产品层叙事与长期 backlog。
- **specs/changes/**：回答「这次变更怎么验收」——可实施的 REQUIREMENTS / DESIGN / TASKS。
- **specs/current/**：回答「线上现在是什么行为」——已验收的系统基线。
- **modules/**：回答「怎么用、怎么运维」——面向开发者和运维的实现说明。

路线图条目进入开发时，应在 `specs/changes/active/` 创建 change 并在 ROADMAP 中链接。

---

## 快速导航

### 需求与路线图

| 文档 | 说明 |
|------|------|
| [产品愿景与原始需求](./requirements/VISION.md) | 平台定位、核心能力与非目标 |
| [平台优化路线图](./requirements/ROADMAP.md) | 持续优化路线与状态跟踪 |
| [跨域主题索引](./requirements/themes/) | 跨模块能力主题（按需扩展） |

### 模块文档

| 模块 | 说明 |
|------|------|
| [PMS / 权限与租户](./modules/pms/) | 功能说明、数据权限、租户隔离 |
| [认证授权](./modules/authorization-server/) | OAuth2、JWK、Token、BFF 流程 |
| [凭证安全](./modules/credential-security/) | 密码策略、策略加载器 |
| [字典](./modules/dict/) | 三级缓存、跨节点失效 |
| [安全策略中心](./modules/security-center/) | 网关限流、黑白名单 |
| [对象存储](./modules/object-storage-service/) | OSS 架构与迁移 |
| [任务调度](./modules/task-scheduler-system/) | TSS 架构与用法 |
| [社交模块](./modules/social/) | InvalidationBus 约定 |

完整索引见 [modules/README.md](./modules/README.md)。

### 运维与指南

| 分类 | 重点文档 |
|------|----------|
| 性能优化 | [优化总览](./guides/performance/OPTIMIZATION-SUMMARY.md)、[资源规划](./guides/performance/MICROSERVICES-RESOURCE-PLANNING.md)、[配置对比](./guides/performance/CONFIGURATION-COMPARISON.md) |
| 故障排查 | [排查指南](./guides/troubleshooting/TROUBLESHOOTING-SERVICE-HANG.md)、[快速参考](./guides/troubleshooting/QUICK-REFERENCE-TROUBLESHOOTING.md) |
| 部署 | [Docker 多环境](./guides/deployment/DOCKER-MULTI-ENVIRONMENT.md)、[环境变量说明](./guides/deployment/ENV-INSTRUCTIONS.md) |
| 升级 | [Spring Framework 6.x](./guides/upgrade/Upgrading-to-Spring-Framework-6.x.md) |
| 配置示例 | [config-examples/](./guides/config-examples/) |

完整索引见 [guides/README.md](./guides/README.md)。

### 工程规范

- [Java 类型级 Javadoc 规范](./standards/Javadoc.md)

---

## 快速开始

### 场景 1：了解产品方向

```bash
cat docs/requirements/VISION.md
cat docs/requirements/ROADMAP.md
```

### 场景 2：首次部署

1. 阅读 [微服务资源规划](./guides/performance/MICROSERVICES-RESOURCE-PLANNING.md)
2. 复制 [优化配置示例](./guides/config-examples/application-prod-optimized.yml)
3. 参考 [Docker 多环境部署](./guides/deployment/DOCKER-MULTI-ENVIRONMENT.md)

### 场景 3：服务出现问题

1. 运行诊断脚本：`./bin/troubleshoot.sh <容器名>`
2. 查看 [快速参考卡片](./guides/troubleshooting/QUICK-REFERENCE-TROUBLESHOOTING.md)
3. 深入排查：[故障排查指南](./guides/troubleshooting/TROUBLESHOOTING-SERVICE-HANG.md)

### 场景 4：开发新功能

1. 检索 [specs/current/](../specs/current/) 了解线上事实
2. 检索 [specs/changes/active/](../specs/changes/active/) 识别并行变更
3. 在 ROADMAP 中确认优先级，按 SDD 创建 active change

---

## 配置速查（8C16G 单机多服务）

| 配置项 | 推荐值 | 说明 |
|--------|--------|------|
| JVM 堆内存 | 2G | 每个服务 |
| 数据库连接池 | 50 | 每个服务 |
| Undertow IO 线程 | 4 | 每个服务 |
| Undertow Worker 线程 | 120 | 每个服务 |
| 网关限流 | 300 QPS | 稳定速率 |
| 网关突发 | 500 QPS | 突发容量 |

详见 [微服务资源规划](./guides/performance/MICROSERVICES-RESOURCE-PLANNING.md)。

---

**维护者**：ingot-cloud 团队  
**最后更新**：2026-07-17
