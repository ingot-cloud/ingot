# 配置文件示例

本目录包含针对 **8C16G 服务器、1000 并发用户、微服务多实例共存场景** 优化的配置文件示例。

> ⚠️ **重要提示**：配置已针对**单机运行多个微服务**（Gateway + Auth + PMS + Member 等）的场景进行优化，避免资源争抢。

## 📁 文件列表

### 1. application-prod-optimized.yml

**用途**: Spring Boot 应用生产环境配置  
**优化内容**:
- ✅ 数据库连接池：max-active 100
- ✅ Undertow 线程池：worker 256
- ✅ 异步任务线程池：max-size 100
- ✅ Redis 连接池：max-active 50
- ✅ Druid 监控和慢 SQL 检测
- ✅ Spring Boot Actuator 监控端点

**使用方法**:
```bash
# 复制到应用配置目录
cp application-prod-optimized.yml /path/to/your/app/src/main/resources/
# 或通过环境变量指定
java -jar app.jar --spring.config.location=application-prod-optimized.yml
```

### 2. gateway-routes-optimized.yml

**用途**: Spring Cloud Gateway 路由和限流配置  
**优化内容**:
- ✅ 限流：500 QPS 稳定速率，1000 QPS 突发容量
- ✅ 熔断：50% 失败率触发熔断
- ✅ 重试：GET 请求重试 2 次
- ✅ 超时：30 秒响应超时
- ✅ 连接池：最大 1000 连接

**使用方法**:
```yaml
# 合并到网关配置文件
spring:
  cloud:
    gateway:
      routes:
        # 复制 routes 配置
```

## 🎯 配置对比

### 数据库连接池配置对比

| 配置项 | 原配置 | 优化后 | 说明 |
|--------|--------|--------|------|
| max-active | 30 | **50** | 考虑多服务共存，适度配置 |
| max-wait | 3000ms | 5000ms | 避免过早超时 |
| remove-abandoned-timeout | 180s | 60s | 更快检测连接泄漏 |
| pool-prepared-statements | 未设置 | true | 启用 PSCache 提升性能 |

**⚠️ 连接数说明**：假设单机运行 4 个服务（Gateway/Auth/PMS/Member），每服务 50 个连接，总计 200 个连接，需确保 MySQL `max_connections ≥ 300`（预留余量）。

### 网关限流配置对比

| 配置项 | 原配置 | 优化后 | 说明 |
|--------|--------|--------|------|
| replenishRate | 200 | **300** | 每秒放入令牌数（考虑多服务场景）|
| burstCapacity | 300 | **500** | 令牌桶容量（考虑多服务场景）|
| 熔断策略 | 无 | 有 | 50% 失败率触发 |
| 重试策略 | 无 | 有 | GET 请求重试 2 次 |

## 📊 性能预期

### 单机性能（8C16G，多服务共存）

**单个服务性能**：

| 场景 | 单服务 QPS | 响应时间 | 说明 |
|------|-----------|----------|------|
| 简单查询 | 500-800 | 50-100ms | 单表查询 |
| 中等复杂度 | 300-500 | 100-200ms | 多表查询 |
| 复杂业务 | 100-300 | 200-500ms | 复杂逻辑 |

**整机能力（多服务共存）**：

| 服务组合 | 总 QPS | CPU | 内存 |
|---------|--------|-----|------|
| Gateway+Auth+PMS+Member | 400-600 | 70% | 75% |

⚠️ **注意**：总 QPS 受限于最慢的服务（通常是 Auth 认证服务）。

### 集群配置建议

**1000 并发用户场景（微服务多实例共存）**:
- **2 台应用服务器**（8C16G，负载均衡）
- **期望 QPS**: 200-400（平均）
- **峰值 QPS**: 600-800（3-4 倍峰值系数）
- **单机能力**: 400-600 QPS（多服务共存）
- **余量**: 50%-100%

**部署架构**：
```
服务器 1：Gateway + Auth + PMS + Member
服务器 2：Gateway + Auth + PMS + Member
数据库：独立部署（推荐）或与应用同机
负载均衡：Nginx 或硬件 LB
```

## ⚠️ 微服务场景特别说明

### 资源分配（8C16G 单机）

假设单机部署 4 个服务 + MySQL + Redis：

| 服务 | JVM 堆内存 | 容器限制 | CPU 限制 |
|------|-----------|----------|----------|
| Gateway | 2G | 2.5G | 2.0 核 |
| Auth | 2G | 2.5G | 2.0 核 |
| PMS | 2G | 2.5G | 1.5 核 |
| Member | 2G | 2.5G | 1.5 核 |
| MySQL | - | 1G | 1.0 核 |
| Redis | - | 512M | 0.5 核 |
| 系统预留 | - | 2G | - |

**总计**：14G 内存（预留 2G 缓冲），CPU 可超额分配。

### 连接数规划

```sql
-- MySQL 连接数配置
SET GLOBAL max_connections = 500;

-- 连接数分配
Gateway: 0 (不直连数据库)
Auth:    50
PMS:     50
Member:  50
其他:    50
---------
总计:    200（预留 300 余量）
```

---

## 🔧 应用配置步骤

### 步骤 1：备份现有配置

```bash
# 备份现有配置文件
cp application-prod.yml application-prod.yml.backup
cp gateway-routes.yml gateway-routes.yml.backup
```

### 步骤 2：更新配置

```bash
# 应用服务配置
cp application-prod-optimized.yml application-prod.yml

# 网关配置（合并到现有配置）
# 手动合并 gateway-routes-optimized.yml 到 gateway 配置文件
```

### 步骤 3：更新 Dockerfile

```bash
# 使用优化版本的 Dockerfile
cd ingot-service/ingot-pms/ingot-pms-provider/src/main/docker/prod/
cp Dockerfile Dockerfile.backup
cp Dockerfile.optimized Dockerfile
```

### 步骤 4：重新构建和部署

```bash
# 构建镜像
./gradlew :ingot-service:ingot-pms:ingot-pms-provider:build
docker build -t ingot-pms:optimized .

# 部署
docker-compose up -d
# 或
docker run -d --name ingot-pms \
  -p 5200:5200 \
  -v /data/ingot-data:/ingot-data \
  -v /data/logs:/app/logs \
  ingot-pms:optimized
```

### 步骤 5：验证配置

```bash
# 1. 检查应用是否启动成功
docker logs -f ingot-pms

# 2. 检查健康状态
curl http://localhost:5200/actuator/health

# 3. 查看 Druid 监控
# 访问：http://localhost:5200/druid/index.html

# 4. 查看指标
curl http://localhost:5200/actuator/metrics
```

## 🧪 压力测试

### 使用 JMeter

```bash
# 1. 创建测试计划
# 线程组：500 并发用户
# HTTP 请求：/your-api
# 聚合报告：查看吞吐量和响应时间

# 2. 运行测试
jmeter -n -t test_plan.jmx -l result.jtl -e -o report

# 3. 查看报告
open report/index.html
```

### 使用 wrk

```bash
# 100 并发，持续 30 秒
wrk -t8 -c100 -d30s http://localhost:5200/your-api

# 查看结果
# Latency：延迟分布
# Req/Sec：每秒请求数
```

## 📈 监控配置

### Prometheus + Grafana

```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'ingot-pms'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:5200']
```

### 告警规则

参考文档：`../TROUBLESHOOTING-SERVICE-HANG.md` 第 6.3 节

## 🔍 故障排查

如果应用出现问题，使用诊断脚本：

```bash
# 运行诊断脚本
./bin/troubleshoot.sh ingot-pms

# 查看诊断报告
cat troubleshoot_*/REPORT.md
```

详细排查步骤参考：`../TROUBLESHOOTING-SERVICE-HANG.md`

## 📝 注意事项

### 1. 环境变量

配置文件中使用了环境变量，需要在部署时设置：

```bash
export MYSQL_HOST=your-mysql-host
export MYSQL_PORT=3306
export MYSQL_DATABASE=ingot_pms
export MYSQL_USERNAME=your-username
export MYSQL_PASSWORD=your-password
export REDIS_HOST=your-redis-host
export REDIS_PASSWORD=your-redis-password
```

或使用 Docker Compose：

```yaml
services:
  ingot-pms:
    environment:
      - MYSQL_HOST=mysql
      - MYSQL_PORT=3306
      - MYSQL_DATABASE=ingot_pms
      - MYSQL_USERNAME=root
      - MYSQL_PASSWORD=root
      - REDIS_HOST=redis
      - REDIS_PORT=6379
```

### 2. MySQL 配置

确保 MySQL 配置匹配：

```sql
-- 检查最大连接数
SHOW VARIABLES LIKE 'max_connections';
-- 建议：500

-- 检查缓冲池大小
SHOW VARIABLES LIKE 'innodb_buffer_pool_size';
-- 建议：8G（物理内存的 50-70%）
```

### 3. Redis 配置

```bash
# 检查最大连接数
redis-cli CONFIG GET maxclients
# 建议：10000

# 检查内存限制
redis-cli CONFIG GET maxmemory
# 建议：4G
```

### 4. 监控检查清单

部署后检查：
- [ ] 应用启动成功
- [ ] 健康检查通过
- [ ] Druid 监控可访问
- [ ] Actuator 端点可访问
- [ ] 日志正常输出
- [ ] 数据库连接正常
- [ ] Redis 连接正常
- [ ] 网关路由正常

## 🔗 相关文档

- [故障排查指南](../TROUBLESHOOTING-SERVICE-HANG.md)
- **[微服务资源规划指南](../MICROSERVICES-RESOURCE-PLANNING.md)** ⭐ 重点阅读
- [优化后的 Dockerfile](../../ingot-service/ingot-pms/ingot-pms-provider/src/main/docker/prod/Dockerfile)
- [诊断脚本](../../bin/troubleshoot.sh)

## 📞 支持

如有问题，请参考：
1. 故障排查文档
2. 运行诊断脚本
3. 查看应用日志
4. 联系团队支持

---

**更新日期**: 2025-12-24  
**版本**: v1.0  
**适用场景**: 8C16G 服务器，1000 并发用户，**微服务多实例共存**

