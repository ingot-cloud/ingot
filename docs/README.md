# Ingot 文档中心

> 微服务性能优化、故障排查和运维指南

## 🎯 快速导航

### 🔥 新增文档（2025-12-24）

针对 **8C16G 服务器、1000 并发用户、微服务多实例共存** 场景的完整优化方案：

| 文档 | 说明 | 重要程度 |
|------|------|----------|
| [优化总览](./OPTIMIZATION-SUMMARY.md) | 📖 所有优化内容的汇总和导航 | ⭐⭐⭐ |
| [微服务资源规划](./MICROSERVICES-RESOURCE-PLANNING.md) | 🎯 资源分配策略、Docker Compose 配置 | ⭐⭐⭐ |
| [配置对比](./CONFIGURATION-COMPARISON.md) | ⚖️ 单服务 vs 多服务配置对比 | ⭐⭐⭐ |
| [故障排查指南](./TROUBLESHOOTING-SERVICE-HANG.md) | 🔍 完整的故障排查流程和解决方案 | ⭐⭐⭐ |
| [快速参考卡片](./QUICK-REFERENCE-TROUBLESHOOTING.md) | ⚡ 30秒快速诊断、常用命令速查 | ⭐⭐ |
| [配置示例](./config-examples/README.md) | 📝 优化后的配置文件和使用指南 | ⭐⭐ |
| [诊断脚本](../bin/troubleshoot.sh) | 🛠️ 一键自动诊断工具 | ⭐ |

---

## 📚 文档分类

### 1. 性能优化

#### 1.1 资源规划
- **[微服务资源规划指南](./MICROSERVICES-RESOURCE-PLANNING.md)**
  - 单机多服务资源分配
  - 内存、CPU、连接数规划
  - 不同规模的部署方案
  - Docker Compose 完整配置

- **[配置对比文档](./CONFIGURATION-COMPARISON.md)**
  - 单服务 vs 多服务配置
  - 配置决策树
  - 快速调整命令

#### 1.2 配置优化
- **[配置示例](./config-examples/README.md)**
  - [应用配置](./config-examples/application-prod-optimized.yml)
  - [网关配置](./config-examples/gateway-routes-optimized.yml)
  - 配置说明和对比

### 2. 故障排查

#### 2.1 详细指南
- **[服务假死排查指南](./TROUBLESHOOTING-SERVICE-HANG.md)**
  - 5 步排查流程
  - JVM 诊断（jstack、jmap、jstat）
  - 数据库连接池检查
  - 系统资源和网络诊断
  - 监控配置和告警规则
  - 应急处理方案

#### 2.2 快速参考
- **[快速参考卡片](./QUICK-REFERENCE-TROUBLESHOOTING.md)**
  - 30 秒快速诊断
  - 1 分钟应急处理
  - 15 分钟五步排查法
  - 关键指标阈值表
  - 常见问题速查
  - 可打印版本

### 3. 工具脚本

- **[自动诊断脚本](../bin/troubleshoot.sh)**
  - 一键收集诊断信息
  - 自动生成分析报告
  - 使用方法：`./bin/troubleshoot.sh <容器名>`

### 4. 业务功能文档

#### 4.1 认证授权
- [OAuth2 配置](./authorization-server/OAuth2.md)
- [JWK 配置](./authorization-server/JWK-CONFIGURATION.md)
- [JTI 清理方案](./authorization-server/JTI-AND-CLEANUP-SOLUTION.md)
- [在线用户查询](./authorization-server/ONLINE-USER-QUERY-GUIDE.md)
- [Token 优化](./authorization-server/TOKEN-OPTIMIZATION-GUIDE.md)

#### 4.2 权限管理
- [数据权限](./DataScope.md)
- [租户管理](./Tenant.md)
- [功能说明](./FUNCTION.md)

#### 4.3 部署相关
- [Docker 多环境部署](./DOCKER-MULTI-ENVIRONMENT.md)
- [Dockerfile 多环境配置](./DOCKERFILE-MULTI-ENV.md)
- [Dockerfile 迁移指南](./DOCKERFILE-MIGRATION-GUIDE.md)
- [环境变量说明](./ENV-INSTRUCTIONS.md)

#### 4.4 升级指南
- [Spring Framework 6.x 升级](./Upgrading-to-Spring-Framework-6.x.md)
- [开发路线图](./ROADMAP.md)

---

## 🚀 快速开始

### 场景 1：首次部署

1. **阅读资源规划**
   ```bash
   cat docs/MICROSERVICES-RESOURCE-PLANNING.md
   ```

2. **使用优化配置**
   ```bash
   # 复制配置文件
   cp docs/config-examples/application-prod-optimized.yml src/main/resources/
   ```

3. **部署服务**
   ```bash
   # 使用 Docker Compose
   docker-compose up -d
   ```

4. **验证部署**
   ```bash
   # 检查健康状态
   curl http://localhost:5200/actuator/health
   
   # 查看资源使用
   docker stats
   ```

### 场景 2：服务出现问题

1. **快速诊断**
   ```bash
   # 运行自动诊断脚本
   ./bin/troubleshoot.sh ingot-pms
   
   # 查看报告
   cat troubleshoot_*/REPORT.md
   ```

2. **查看快速参考**
   ```bash
   cat docs/QUICK-REFERENCE-TROUBLESHOOTING.md
   ```

3. **深入排查**
   ```bash
   # 参考详细指南
   cat docs/TROUBLESHOOTING-SERVICE-HANG.md
   ```

### 场景 3：性能优化

1. **对比配置**
   ```bash
   cat docs/CONFIGURATION-COMPARISON.md
   ```

2. **调整资源**
   ```bash
   # 根据监控数据调整配置
   docker update --memory=3g ingot-pms
   ```

3. **压力测试**
   ```bash
   # 使用 wrk 或 JMeter 测试
   wrk -t8 -c100 -d30s http://localhost:5200/your-api
   ```

---

## 📊 配置速查

### 微服务场景（8C16G 单机）

| 配置项 | 推荐值 | 说明 |
|--------|--------|------|
| **JVM 堆内存** | 2G | 每个服务 |
| **数据库连接池** | 50 | 每个服务 |
| **Undertow IO 线程** | 4 | 每个服务 |
| **Undertow Worker 线程** | 120 | 每个服务 |
| **网关限流** | 300 QPS | 稳定速率 |
| **网关突发** | 500 QPS | 突发容量 |

### 资源分配

```
16G 内存分配：
├─ 系统：      2G
├─ Gateway：   2.5G
├─ Auth：      2.5G
├─ PMS：       2.5G
├─ Member：    2.5G
├─ MySQL：     1G
├─ Redis：     512M
└─ 缓冲：      512M
```

---

## 🔍 常见问题

### Q1: 服务假死如何排查？

**A**: 按照以下步骤：
1. 运行诊断脚本：`./bin/troubleshoot.sh ingot-pms`
2. 查看快速参考：[QUICK-REFERENCE-TROUBLESHOOTING.md](./QUICK-REFERENCE-TROUBLESHOOTING.md)
3. 深入排查：[TROUBLESHOOTING-SERVICE-HANG.md](./TROUBLESHOOTING-SERVICE-HANG.md)

### Q2: 单机应该运行几个服务？

**A**: 8C16G 服务器推荐运行 4-5 个服务：
- Gateway + Auth + PMS + Member（核心服务）
- 如果数据库独立部署，可以再加 1-2 个服务

详见：[微服务资源规划](./MICROSERVICES-RESOURCE-PLANNING.md)

### Q3: 1000 用户需要多少服务器？

**A**: 推荐配置：
- 2 台应用服务器（8C16G）
- 1 台数据库服务器（4C8G）
- 1 台 Nginx 负载均衡

详见：[微服务资源规划](./MICROSERVICES-RESOURCE-PLANNING.md) 第 8 节

### Q4: 数据库连接池应该配置多少？

**A**: 计算公式：
```
单服务连接数 = MySQL max_connections × 0.6 ÷ 服务数量

示例：500 × 0.6 ÷ 4 = 75
建议配置：50-80
```

详见：[配置对比文档](./CONFIGURATION-COMPARISON.md)

---

## 🛠️ 工具使用

### 自动诊断脚本

```bash
# 基本使用
./bin/troubleshoot.sh ingot-pms

# 查看报告
cat troubleshoot_*/REPORT.md

# 分析堆转储（如果生成）
# 使用 Eclipse MAT 打开 troubleshoot_*/app_logs/heapdump.hprof
```

### 手动诊断命令

```bash
# 查看资源
docker stats --no-stream ingot-pms

# 查看日志
docker logs --tail 100 -f ingot-pms

# 进入容器
docker exec -it ingot-pms sh

# JVM 诊断
jstack $(pgrep java)        # 线程
jmap -heap $(pgrep java)    # 堆内存
jstat -gcutil $(pgrep java) # GC
```

---

## 📞 支持与反馈

### 遇到问题

1. 查看相关文档
2. 运行诊断脚本
3. 查看日志和监控
4. 提交 Issue（附带诊断报告）

### 文档反馈

如果文档有不清楚或错误的地方，欢迎提出：
- 提交 Issue
- 提交 PR
- 联系团队

---

## 📝 更新日志

### 2025-12-24

新增完整的性能优化和故障排查方案：

**新增文档**：
- ✅ 微服务资源规划指南
- ✅ 配置对比文档
- ✅ 服务假死排查指南
- ✅ 快速参考卡片
- ✅ 优化总览文档
- ✅ 配置示例和说明

**新增工具**：
- ✅ 自动诊断脚本

**配置优化**：
- ✅ Dockerfile JVM 参数优化
- ✅ 数据库连接池配置
- ✅ Web 容器线程池配置
- ✅ 网关限流配置
- ✅ Docker Compose 完整配置

---

## 📚 扩展阅读

### 官方文档
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/html/)
- [Spring Cloud Gateway](https://docs.spring.io/spring-cloud-gateway/docs/current/reference/html/)
- [Alibaba Druid](https://github.com/alibaba/druid/wiki)

### 性能优化
- [JVM Performance Tuning](https://docs.oracle.com/en/java/javase/17/gctuning/)
- [MySQL Performance](https://dev.mysql.com/doc/refman/8.0/en/optimization.html)
- [Docker Performance](https://docs.docker.com/config/containers/resource_constraints/)

### 监控工具
- [Prometheus](https://prometheus.io/docs/)
- [Grafana](https://grafana.com/docs/)
- [Skywalking](https://skywalking.apache.org/docs/)

---

**维护者**：ingot-cloud 团队  
**最后更新**：2025-12-24  
**文档版本**：v1.0

