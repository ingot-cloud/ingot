# Docker 多环境 Dockerfile 配置说明

## 📋 概述

为所有微服务创建了三个环境（dev、test、prod）的 Dockerfile，确保每个环境都有针对性的优化配置。

## 🎯 服务列表和端口

| 服务 | 端口 | 目录 |
|------|------|------|
| ingot-gateway | 7980 | `ingot-service/ingot-gateway/src/main/docker/{env}/` |
| ingot-auth | 5100 | `ingot-service/ingot-auth/src/main/docker/{env}/` |
| ingot-member-provider | 5300 | `ingot-service/ingot-member/ingot-member-provider/src/main/docker/{env}/` |
| ingot-pms-provider | 5200 | `ingot-service/ingot-pms/ingot-pms-provider/src/main/docker/{env}/` |

## 📁 目录结构

```
ingot-service/
├── ingot-gateway/
│   └── src/main/docker/
│       ├── dev/
│       │   └── Dockerfile
│       ├── test/
│       │   └── Dockerfile
│       └── prod/
│           └── Dockerfile
│
├── ingot-auth/
│   └── src/main/docker/
│       ├── dev/
│       │   └── Dockerfile
│       ├── test/
│       │   └── Dockerfile
│       └── prod/
│           └── Dockerfile
│
├── ingot-member/
│   └── ingot-member-provider/
│       └── src/main/docker/
│           ├── dev/
│           │   └── Dockerfile
│           ├── test/
│           │   └── Dockerfile
│           └── prod/
│               └── Dockerfile
│
└── ingot-pms/
    └── ingot-pms-provider/
        └── src/main/docker/
            ├── dev/
            │   └── Dockerfile
            ├── test/
            │   └── Dockerfile
            └── prod/
                └── Dockerfile
```

## 🔧 环境配置对比

### 开发环境（dev）

**特点**：
- ✅ 基础镜像：`openjdk:17-jdk-slim`
- ✅ 包含完整调试工具（curl、netcat）
- ✅ 启用远程调试（端口 5005）
- ✅ 较小的内存配置（256m-512m）
- ✅ 使用 dev profile
- ✅ 创建 `/ingot-data` 目录

**JVM 参数**：
```bash
-Xmx512m
-Xms256m
-Xdebug
-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=*:5005
-Dspring.profiles.active=dev
```

**暴露端口**：
- 服务端口（7980/5100/5300/5200）
- 调试端口（5005）

### 测试环境（test）

**特点**：
- ✅ 基础镜像：`openjdk:17-jdk-slim`
- ✅ 包含必要工具（curl）
- ✅ 中等内存配置（512m-1g）
- ✅ 启用基本 JVM 调优（G1GC）
- ✅ 使用 test profile
- ✅ 创建 `/ingot-data` 和日志目录
- ✅ 包含健康检查

**JVM 参数**：
```bash
-Xmx1g
-Xms512m
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/app/logs/heapdump.hprof
-Dspring.profiles.active=test
```

**健康检查**：
```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=60s \
    CMD curl -f http://localhost:{PORT}/actuator/health || exit 1
```

### 生产环境（prod）

**特点**：
- ✅ 基础镜像：`openjdk:17-jdk-alpine`（更小更安全）
- ✅ 使用非 root 用户运行（spring:spring）
- ✅ 较大的内存配置（1g-2g）
- ✅ 完整的 JVM 性能调优
- ✅ 使用 prod profile
- ✅ 创建 `/ingot-data` 和日志目录（设置权限）
- ✅ 完善的健康检查
- ✅ 支持多架构（amd64/arm64）
- ✅ GC 日志记录

**JVM 参数**：
```bash
-server
-Xmx2g
-Xms1g
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
-XX:ParallelGCThreads=4
-XX:ConcGCThreads=2
-XX:InitiatingHeapOccupancyPercent=45
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/app/logs/heapdump.hprof
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-XX:+PrintGCTimeStamps
-Xloggc:/app/logs/gc.log
-XX:+UseGCLogFileRotation
-XX:NumberOfGCLogFiles=5
-XX:GCLogFileSize=10M
-Dspring.profiles.active=prod
-Duser.timezone=Asia/Shanghai
```

**健康检查**：
```dockerfile
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=90s \
    CMD curl -f http://localhost:{PORT}/actuator/health || exit 1
```

**安全配置**：
```dockerfile
# 创建非 root 用户
RUN addgroup -S spring && adduser -S spring -G spring

# 设置权限
RUN mkdir -p /ingot-data /app/logs && \
    chown -R spring:spring /app /ingot-data

# 切换用户
USER spring:spring
```

## 🚀 使用方法

### 1. 构建镜像

使用 ingot-assemble-plugin：

```bash
# 开发环境
./gradlew :ingot-service:ingot-gateway:dockerBuildDev
./gradlew :ingot-service:ingot-auth:dockerBuildDev
./gradlew :ingot-service:ingot-member:ingot-member-provider:dockerBuildDev
./gradlew :ingot-service:ingot-pms:ingot-pms-provider:dockerBuildDev

# 测试环境
./gradlew :ingot-service:ingot-gateway:dockerBuildTest

# 生产环境
./gradlew :ingot-service:ingot-gateway:dockerBuildProd
```

### 2. 运行容器

```bash
# 开发环境
docker run -d \
  --name ingot-gateway-dev \
  -p 7980:7980 \
  -p 5005:5005 \
  -v /data/ingot:/ingot-data \
  ingot/gateway-dev

# 测试环境
docker run -d \
  --name ingot-gateway-test \
  -p 7980:7980 \
  -v /data/ingot:/ingot-data \
  ingot/gateway-test

# 生产环境
docker run -d \
  --name ingot-gateway-prod \
  -p 7980:7980 \
  -v /data/ingot:/ingot-data \
  ingot/gateway
```

### 3. 使用 Docker Compose

```yaml
version: '3.8'

services:
  # Gateway - 开发环境
  gateway-dev:
    image: ingot/gateway-dev
    container_name: ingot-gateway-dev
    ports:
      - "7980:7980"
      - "5005:5005"  # 调试端口
    volumes:
      - /data/ingot:/ingot-data
    environment:
      - SPRING_PROFILES_ACTIVE=dev
    networks:
      - ingot-network

  # Auth - 开发环境
  auth-dev:
    image: ingot/auth-dev
    container_name: ingot-auth-dev
    ports:
      - "5100:5100"
      - "5006:5005"  # 调试端口
    volumes:
      - /data/ingot:/ingot-data
    environment:
      - SPRING_PROFILES_ACTIVE=dev
    networks:
      - ingot-network

networks:
  ingot-network:
    driver: bridge
```

## 📊 配置对比表

| 配置项 | dev | test | prod |
|--------|-----|------|------|
| 基础镜像 | openjdk:17-jdk-slim | openjdk:17-jdk-slim | openjdk:17-jdk-alpine |
| 内存配置 | 256m-512m | 512m-1g | 1g-2g |
| 远程调试 | ✅ | ❌ | ❌ |
| 调试工具 | ✅ 完整 | ✅ 基础 | ✅ 基础 |
| 健康检查 | ❌ | ✅ | ✅ |
| 非 root 用户 | ❌ | ❌ | ✅ |
| GC 调优 | ❌ | ✅ 基础 | ✅ 完整 |
| GC 日志 | ❌ | ❌ | ✅ |
| 多架构支持 | ❌ | ❌ | ✅ |
| /ingot-data | ✅ | ✅ | ✅ |
| 启动时间 | 快 | 中等 | 较慢（更多优化） |

## 💡 最佳实践

### 1. 开发环境

```bash
# 启用远程调试
docker run -d \
  -p 7980:7980 \
  -p 5005:5005 \
  -v $(pwd)/logs:/app/logs \
  -v /data/ingot:/ingot-data \
  ingot/gateway-dev

# 在 IDEA 中配置远程调试
# Run -> Edit Configurations -> Add New Configuration -> Remote JVM Debug
# Host: localhost
# Port: 5005
```

### 2. 测试环境

```bash
# 查看健康状态
curl http://localhost:7980/actuator/health

# 查看堆转储（如果发生 OOM）
docker exec ingot-gateway-test ls -lh /app/logs/heapdump.hprof
```

### 3. 生产环境

```bash
# 查看 GC 日志
docker exec ingot-gateway-prod tail -f /app/logs/gc.log

# 监控容器健康
docker inspect --format='{{.State.Health.Status}}' ingot-gateway-prod

# 导出堆转储
docker cp ingot-gateway-prod:/app/logs/heapdump.hprof ./
```

## 🔍 故障排查

### 问题 1：/ingot-data 目录权限问题

**现象**：应用无法写入 /ingot-data 目录

**解决方案**：
```bash
# 生产环境（非 root 用户）
docker run -d \
  -v /data/ingot:/ingot-data:rw \
  --user spring:spring \
  ingot/gateway

# 或者在宿主机上设置权限
sudo chown -R 1000:1000 /data/ingot
```

### 问题 2：健康检查失败

**现象**：容器状态显示 unhealthy

**排查步骤**：
```bash
# 查看健康检查日志
docker inspect --format='{{json .State.Health}}' ingot-gateway-prod | jq

# 手动执行健康检查
docker exec ingot-gateway-prod curl -f http://localhost:7980/actuator/health

# 查看应用日志
docker logs ingot-gateway-prod
```

### 问题 3：内存溢出

**现象**：应用频繁 OOM

**解决方案**：
```bash
# 1. 增加内存限制
docker run -d \
  -m 3g \
  -e JAVA_OPTS="-Xmx2.5g -Xms2g" \
  ingot/gateway-prod

# 2. 分析堆转储
docker cp ingot-gateway-prod:/app/logs/heapdump.hprof ./
# 使用 MAT 或 VisualVM 分析

# 3. 查看 GC 日志
docker exec ingot-gateway-prod cat /app/logs/gc.log
```

## 📈 性能调优建议

### 开发环境
- 快速启动，便于调试
- 无需过多优化

### 测试环境
- 模拟生产环境配置
- 开启基本的 GC 调优
- 包含性能监控

### 生产环境
- 完整的 JVM 调优
- GC 日志记录
- 健康检查和监控
- 使用非 root 用户
- 资源限制和隔离

## 🔗 相关文档

- [ingot-assemble-plugin 文档](../ingot-plugin/ingot-assemble-plugin/README.md)
- [Docker 多环境配置指南](./DOCKER-MULTI-ENVIRONMENT.md)
- [JVM 调优参考](https://docs.oracle.com/javase/17/gctuning/)

---

**注意**：所有 Dockerfile 都已确保创建 `/ingot-data` 目录，并配置了正确的服务端口。
