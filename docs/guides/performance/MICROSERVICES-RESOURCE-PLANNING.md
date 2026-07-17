# 微服务资源规划指南

> 针对多服务共存场景的资源分配和配置优化

## 📊 典型部署架构

### 单机多服务部署（8C16G）

```
┌─────────────────────────────────────────────┐
│         8C16G 物理服务器                     │
├─────────────────────────────────────────────┤
│  系统 + Docker + 其他开销：2G               │
├─────────────────────────────────────────────┤
│  ┌─────────────────────────────────────┐   │
│  │  Gateway (网关服务)       2.5G      │   │
│  ├─────────────────────────────────────┤   │
│  │  Auth (认证服务)          2.5G      │   │
│  ├─────────────────────────────────────┤   │
│  │  PMS (权限管理服务)       2.5G      │   │
│  ├─────────────────────────────────────┤   │
│  │  Member (用户服务)        2.5G      │   │
│  ├─────────────────────────────────────┤   │
│  │  其他服务                 2G        │   │
│  └─────────────────────────────────────┘   │
│  MySQL: 1G  │  Redis: 512M  │  其他: 500M │
└─────────────────────────────────────────────┘
```

---

## 🎯 资源分配策略

### 内存分配（16G 总内存）

| 组件 | 分配 | 占比 | 说明 |
|------|------|------|------|
| **系统预留** | 2G | 12.5% | OS + Docker + 缓冲 |
| **Gateway** | 2.5G | 15.6% | 流量入口，高并发 |
| **Auth** | 2.5G | 15.6% | 认证频繁，Token 生成 |
| **PMS** | 2.5G | 15.6% | 业务服务 |
| **Member** | 2.5G | 15.6% | 业务服务 |
| **MySQL** | 1G | 6.25% | 如在同机 |
| **Redis** | 512M | 3.1% | 如在同机 |
| **其他服务** | 2G | 12.5% | 弹性分配 |
| **预留缓冲** | 512M | 3.1% | 应对突发 |

### CPU 分配（8 核心）

| 组件 | CPU 限制 | 说明 |
|------|----------|------|
| **Gateway** | 2 核 | 流量入口 |
| **Auth** | 2 核 | 计算密集（加密） |
| **PMS** | 1.5 核 | 业务服务 |
| **Member** | 1.5 核 | 业务服务 |
| **其他** | 1 核 | 弹性分配 |

**注意**：CPU 限制是软限制，可以超额分配（总和可以 > 8 核）

---

## ⚙️ 各服务配置建议

### 1. Gateway（网关服务）

#### JVM 配置
```dockerfile
ENV JAVA_OPTS="-server \
               -Xms2g \
               -Xmx2g \
               -XX:MetaspaceSize=256m \
               -XX:MaxMetaspaceSize=256m \
               -XX:MaxDirectMemorySize=256m \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=100 \
               -XX:InitiatingHeapOccupancyPercent=45 \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=80.0 \
               -XX:+AlwaysPreTouch"
```

#### Docker Compose 配置
```yaml
gateway:
  mem_limit: 2560m
  mem_reservation: 2g
  cpus: '2.0'
  environment:
    JAVA_OPTS: "-Xms2g -Xmx2g"
```

#### 线程池配置（application.yml）
```yaml
server:
  undertow:
    threads:
      io: 4          # Gateway 需要较多 IO 线程
      worker: 200    # 降低工作线程数
```

#### 连接池配置
```yaml
# Gateway 不直接连数据库，主要是 HTTP 客户端
spring:
  cloud:
    gateway:
      httpclient:
        pool:
          max-connections: 500    # 降低最大连接数
```

---

### 2. Auth（认证服务）

#### JVM 配置
```dockerfile
ENV JAVA_OPTS="-server \
               -Xms2g \
               -Xmx2g \
               -XX:MetaspaceSize=256m \
               -XX:MaxMetaspaceSize=256m \
               -XX:MaxDirectMemorySize=256m \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=150 \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=80.0"
```

#### Docker Compose 配置
```yaml
auth:
  mem_limit: 2560m
  mem_reservation: 2g
  cpus: '2.0'
```

#### 数据库连接池配置
```yaml
spring:
  datasource:
    druid:
      initial-size: 10
      min-idle: 10
      max-active: 50     # 认证服务，连接数适中
      max-wait: 5000
```

#### 线程池配置
```yaml
server:
  undertow:
    threads:
      io: 4
      worker: 150      # 认证服务，适中
```

---

### 3. PMS/Member（业务服务）

#### JVM 配置
```dockerfile
ENV JAVA_OPTS="-server \
               -Xms2g \
               -Xmx2g \
               -XX:MetaspaceSize=256m \
               -XX:MaxMetaspaceSize=384m \
               -XX:MaxDirectMemorySize=256m \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -XX:InitiatingHeapOccupancyPercent=45 \
               -XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=80.0 \
               -XX:+AlwaysPreTouch \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/logs/heapdump.hprof \
               -Xlog:gc*:file=/app/logs/gc.log:time,level,tags:filecount=5,filesize=10M"
```

#### Docker Compose 配置
```yaml
pms:
  mem_limit: 2560m
  mem_reservation: 2g
  cpus: '1.5'
  
member:
  mem_limit: 2560m
  mem_reservation: 2g
  cpus: '1.5'
```

#### 数据库连接池配置
```yaml
spring:
  datasource:
    druid:
      initial-size: 10
      min-idle: 10
      max-active: 50     # 每个服务 50 个连接
      max-wait: 5000
      remove-abandoned: true
      remove-abandoned-timeout: 60
```

#### 线程池配置
```yaml
server:
  undertow:
    threads:
      io: 4
      worker: 120      # 业务服务，适中

spring:
  task:
    execution:
      pool:
        core-size: 10
        max-size: 50    # 异步线程池也要降低
        queue-capacity: 200
```

---

## 📐 容量计算

### 数据库连接数计算

假设 MySQL 配置 `max_connections = 500`：

| 服务 | 实例数 | 每实例连接 | 总连接 |
|------|--------|-----------|--------|
| Auth | 1 | 50 | 50 |
| PMS | 1 | 50 | 50 |
| Member | 1 | 50 | 50 |
| 其他服务 | 3 | 30 | 90 |
| **总计** | - | - | **240** |
| **预留** | - | - | 260 |

✅ 240 < 500，连接数充足

### QPS 能力估算

**单机多服务场景（8C16G）**：

| 服务 | 单服务 QPS | 说明 |
|------|-----------|------|
| Gateway | 2000-3000 | 主要是路由转发 |
| Auth | 300-500 | 认证计算密集 |
| PMS | 300-500 | 业务逻辑 |
| Member | 300-500 | 业务逻辑 |

**总体能力**：
- 如果请求均匀分布：**总 QPS ≈ 400-600**
- 如果热点服务（如 Auth）：**受限于最慢服务**

### 1000 用户场景重新评估

```
期望 QPS = 1000 ÷ (5 + 0.2) ≈ 192 QPS
峰值 QPS = 192 × 3 = 576 QPS

单机能力：400-600 QPS（多服务共存）
所需服务器：2 台（主备 + 负载均衡）

部署方案：
- 服务器 1：Gateway + Auth + PMS + Member
- 服务器 2：Gateway + Auth + PMS + Member
- 负载均衡：Nginx 或 硬件 LB
```

---

## 🔧 优化后的配置文件

### Docker Compose 完整示例

```yaml
version: '3.8'

services:
  # ========== 网关服务 ==========
  gateway:
    image: ingot-gateway:latest
    container_name: ingot-gateway
    restart: always
    ports:
      - "8080:8080"
    networks:
      - ingot-network
    environment:
      - JAVA_OPTS=-Xms2g -Xmx2g -XX:+UseG1GC
      - SPRING_PROFILES_ACTIVE=prod
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2560M
        reservations:
          cpus: '1.0'
          memory: 2G
    volumes:
      - /data/logs/gateway:/app/logs
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
  
  # ========== 认证服务 ==========
  auth:
    image: ingot-auth:latest
    container_name: ingot-auth
    restart: always
    ports:
      - "5100:5100"
    networks:
      - ingot-network
    environment:
      - JAVA_OPTS=-Xms2g -Xmx2g -XX:+UseG1GC
      - SPRING_PROFILES_ACTIVE=prod
      - MYSQL_HOST=mysql
      - REDIS_HOST=redis
    deploy:
      resources:
        limits:
          cpus: '2.0'
          memory: 2560M
        reservations:
          cpus: '1.0'
          memory: 2G
    volumes:
      - /data/logs/auth:/app/logs
      - /data/ingot-data:/ingot-data
    depends_on:
      - mysql
      - redis
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5100/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
  
  # ========== PMS 服务 ==========
  pms:
    image: ingot-pms:latest
    container_name: ingot-pms
    restart: always
    ports:
      - "5200:5200"
    networks:
      - ingot-network
    environment:
      - JAVA_OPTS=-Xms2g -Xmx2g -XX:+UseG1GC
      - SPRING_PROFILES_ACTIVE=prod
      - MYSQL_HOST=mysql
      - REDIS_HOST=redis
    deploy:
      resources:
        limits:
          cpus: '1.5'
          memory: 2560M
        reservations:
          cpus: '0.5'
          memory: 2G
    volumes:
      - /data/logs/pms:/app/logs
      - /data/ingot-data:/ingot-data
    depends_on:
      - mysql
      - redis
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5200/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
  
  # ========== Member 服务 ==========
  member:
    image: ingot-member:latest
    container_name: ingot-member
    restart: always
    ports:
      - "5300:5300"
    networks:
      - ingot-network
    environment:
      - JAVA_OPTS=-Xms2g -Xmx2g -XX:+UseG1GC
      - SPRING_PROFILES_ACTIVE=prod
      - MYSQL_HOST=mysql
      - REDIS_HOST=redis
    deploy:
      resources:
        limits:
          cpus: '1.5'
          memory: 2560M
        reservations:
          cpus: '0.5'
          memory: 2G
    volumes:
      - /data/logs/member:/app/logs
      - /data/ingot-data:/ingot-data
    depends_on:
      - mysql
      - redis
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:5300/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
  
  # ========== MySQL ==========
  mysql:
    image: mysql:8.0
    container_name: ingot-mysql
    restart: always
    ports:
      - "3306:3306"
    networks:
      - ingot-network
    environment:
      - MYSQL_ROOT_PASSWORD=root
      - MYSQL_DATABASE=ingot
    deploy:
      resources:
        limits:
          cpus: '1.0'
          memory: 1G
        reservations:
          memory: 512M
    volumes:
      - /data/mysql:/var/lib/mysql
      - ./databases:/docker-entrypoint-initdb.d
    command:
      - --max-connections=500
      - --innodb-buffer-pool-size=512M
      - --character-set-server=utf8mb4
      - --collation-server=utf8mb4_unicode_ci
  
  # ========== Redis ==========
  redis:
    image: redis:7-alpine
    container_name: ingot-redis
    restart: always
    ports:
      - "6379:6379"
    networks:
      - ingot-network
    deploy:
      resources:
        limits:
          cpus: '0.5'
          memory: 512M
        reservations:
          memory: 256M
    volumes:
      - /data/redis:/data
    command: redis-server --maxmemory 400m --maxmemory-policy allkeys-lru

networks:
  ingot-network:
    driver: bridge

volumes:
  mysql-data:
  redis-data:
```

---

## 📊 监控和调优

### 资源使用监控

```bash
# 查看所有容器资源使用
docker stats

# 查看特定服务
docker stats ingot-gateway ingot-auth ingot-pms ingot-member
```

**健康状态**：
- CPU 使用率：< 70%
- 内存使用率：< 80%
- 如果持续超过阈值，考虑：
  1. 优化代码
  2. 增加服务器
  3. 调整资源分配

### 动态调整资源

```bash
# 临时增加内存限制
docker update --memory=3g --memory-swap=3g ingot-pms

# 临时增加 CPU 限制
docker update --cpus=2.0 ingot-pms
```

---

## 🎯 不同规模的部署方案

### 方案 1：小规模（< 500 用户）
**1 台服务器（8C16G）**
```
Gateway + Auth + PMS + Member + MySQL + Redis
```

### 方案 2：中等规模（500-1500 用户）✅ **推荐**
**2 台应用服务器（8C16G）+ 1 台数据库服务器**
```
应用服务器 1/2：Gateway + Auth + PMS + Member
数据库服务器：MySQL + Redis
负载均衡：Nginx
```

### 方案 3：大规模（> 1500 用户）
**服务分离部署**
```
网关服务器 × 2：Gateway
业务服务器 × 2：Auth + PMS + Member
数据库服务器 × 1：MySQL（主从）
缓存服务器 × 1：Redis（哨兵/集群）
```

---

## 🚨 注意事项

### 1. 避免资源争抢

❌ **错误做法**：
```yaml
# 所有服务都配置 -Xmx6g，会导致 OOM
pms:
  environment:
    - JAVA_OPTS=-Xmx6g
member:
  environment:
    - JAVA_OPTS=-Xmx6g
```

✅ **正确做法**：
```yaml
# 根据服务器总内存合理分配
pms:
  environment:
    - JAVA_OPTS=-Xmx2g
  deploy:
    resources:
      limits:
        memory: 2560M
```

### 2. 数据库连接数

确保所有服务的连接数之和不超过 MySQL 的 `max_connections`：

```sql
-- 设置 MySQL 最大连接数
SET GLOBAL max_connections = 500;

-- 查看当前连接数
SHOW STATUS LIKE 'Threads_connected';
```

### 3. 端口管理

| 服务 | 端口 | 说明 |
|------|------|------|
| Gateway | 8080 | 对外统一入口 |
| Auth | 5100 | 内部服务 |
| PMS | 5200 | 内部服务 |
| Member | 5300 | 内部服务 |
| MySQL | 3306 | 数据库 |
| Redis | 6379 | 缓存 |

### 4. 日志管理

```bash
# 每个服务独立日志目录
/data/logs/
├── gateway/
├── auth/
├── pms/
└── member/

# 定期清理日志
find /data/logs -name "*.log" -mtime +30 -delete
```

---

## 📈 性能优化建议

### 优先级 1：高优先级

1. **启用服务间缓存**
   - Redis 缓存热点数据
   - 本地缓存（Caffeine）减少 Redis 调用

2. **数据库优化**
   - 添加索引
   - 优化慢 SQL
   - 使用连接池

3. **异步处理**
   - 非关键业务异步化
   - 使用消息队列（RabbitMQ/Kafka）

### 优先级 2：中优先级

1. **服务分离**
   - 高频服务独立部署
   - 读写分离

2. **网关优化**
   - 限流降级
   - 熔断机制

3. **监控告警**
   - Prometheus + Grafana
   - 及时发现问题

### 优先级 3：低优先级

1. **JVM 参数微调**
2. **容器参数优化**
3. **代码层面优化**

---

## 🔗 相关文档

- [故障排查指南](../troubleshooting/TROUBLESHOOTING-SERVICE-HANG.md)
- [配置示例](../config-examples/README.md)
- [快速参考](../troubleshooting/QUICK-REFERENCE-TROUBLESHOOTING.md)

---

**最后更新**：2025-12-24  
**版本**：v1.0  
**适用场景**：微服务架构，单机多服务部署

