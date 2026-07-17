# Dockerfile 基础镜像迁移指南

从 OpenJDK 迁移到 Amazon Corretto

## 📌 为什么要迁移？

**OpenJDK 官方镜像已停止维护**：
- ❌ `openjdk:17-jdk-slim` - 2021年9月后停止更新
- ❌ `openjdk:17-jdk-alpine` - 2021年9月后停止更新
- ❌ 安全漏洞不再修复
- ❌ 没有新版本发布

**推荐的替代方案**：
- ✅ **Amazon Corretto** - AWS 维护，长期支持
- ✅ **Eclipse Temurin** - Eclipse Foundation 维护
- ✅ **Microsoft OpenJDK** - Microsoft 维护

## 🎯 推荐方案：Amazon Corretto

### 为什么选择 Amazon Corretto？

1. ✅ **免费长期支持**：AWS 提供长期支持，无需付费
2. ✅ **性能优化**：包含 AWS 的性能优化和补丁
3. ✅ **安全更新及时**：定期更新安全补丁
4. ✅ **生产级**：在 AWS 服务中大规模使用
5. ✅ **完全兼容**：100% 兼容 OpenJDK

### Amazon Corretto 镜像选项

| 镜像 | 基础系统 | 大小 | 包管理器 | 适用场景 |
|------|---------|------|----------|----------|
| `amazoncorretto:17` | Amazon Linux 2 | ~400MB | yum/dnf | 开发/测试环境 |
| `amazoncorretto:17-alpine` | Alpine Linux | ~200MB | apk | 生产环境（推荐） |
| `amazoncorretto:17-debian` | Debian | ~350MB | apt-get | 需要 Debian 生态 |
| `amazoncorretto:17-al2023` | Amazon Linux 2023 | ~380MB | dnf | 最新系统特性 |

## 🔄 迁移步骤

### 场景 1：从 openjdk:17-jdk-slim 迁移

**原 Dockerfile**：
```dockerfile
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY *.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m -Xms256m"
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**迁移后（推荐使用标准版）**：
```dockerfile
FROM amazoncorretto:17
WORKDIR /app
COPY *.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xmx512m -Xms256m"
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**变化**：
- ✅ 只需更改 FROM 镜像
- ✅ 其他配置完全兼容
- ✅ 无需修改命令

### 场景 2：从 openjdk:17-jdk-alpine 迁移

**原 Dockerfile**：
```dockerfile
FROM openjdk:17-jdk-alpine
WORKDIR /app
COPY *.jar app.jar
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
EXPOSE 8080
ENV JAVA_OPTS="-Xmx2g -Xms1g"
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**迁移后（使用 Alpine 版本）**：
```dockerfile
FROM amazoncorretto:17-alpine
WORKDIR /app
COPY *.jar app.jar
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
EXPOSE 8080
ENV JAVA_OPTS="-Xmx2g -Xms1g"
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**变化**：
- ✅ 只需更改 FROM 镜像
- ✅ Alpine 命令完全兼容
- ✅ 无需修改其他配置

### 场景 3：如果使用标准版本（非 Alpine）

**需要注意的命令差异**：

```dockerfile
# Alpine 风格（addgroup/adduser）
FROM amazoncorretto:17-alpine
RUN addgroup -S spring && adduser -S spring -G spring

# 标准 Linux 风格（groupadd/useradd）
FROM amazoncorretto:17
RUN groupadd -r spring && useradd -r -g spring spring
```

## 📝 完整的 Dockerfile 示例

### 开发环境 Dockerfile

**`src/main/docker/dev/Dockerfile`**

```dockerfile
# 使用 Amazon Corretto 17
FROM amazoncorretto:17

# 元数据
LABEL maintainer="your-team@company.com"
LABEL environment="development"
LABEL version="1.0"

WORKDIR /app

# 复制 JAR 文件
COPY *.jar app.jar

# 暴露端口
EXPOSE 8080

# 开发环境 JVM 参数（内存较小，快速启动）
ENV JAVA_OPTS="-Xmx512m -Xms256m \
    -Dspring.profiles.active=dev \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8"

# 启动应用
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 测试环境 Dockerfile

**`src/main/docker/test/Dockerfile`**

```dockerfile
# 使用 Amazon Corretto 17
FROM amazoncorretto:17

LABEL maintainer="your-team@company.com"
LABEL environment="testing"

WORKDIR /app

COPY *.jar app.jar

EXPOSE 8080

# 测试环境 JVM 参数（中等配置）
ENV JAVA_OPTS="-Xmx1g -Xms512m \
    -XX:+UseG1GC \
    -Dspring.profiles.active=test \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8"

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 生产环境 Dockerfile（推荐）

**`src/main/docker/prod/Dockerfile`**

```dockerfile
# 使用 Amazon Corretto 17 Alpine 版本（体积小，安全）
FROM amazoncorretto:17-alpine

LABEL maintainer="your-team@company.com"
LABEL environment="production"

WORKDIR /app

# 复制 JAR 文件
COPY *.jar app.jar

# 创建非 root 用户（安全最佳实践）
RUN addgroup -S spring && \
    adduser -S spring -G spring && \
    chown spring:spring app.jar

# 切换到非 root 用户
USER spring:spring

# 暴露端口
EXPOSE 8080

# 生产环境 JVM 参数（优化配置）
ENV JAVA_OPTS="-Xmx2g -Xms1g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+ParallelRefProcEnabled \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof \
    -Dspring.profiles.active=prod \
    -Djava.security.egd=file:/dev/./urandom \
    -Dfile.encoding=UTF-8"

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 启动应用
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

## 🔍 命令对比表

### 用户和组管理

| 操作 | Alpine (apk) | Amazon Linux 2 (yum) | Debian (apt) |
|------|-------------|---------------------|--------------|
| 创建组 | `addgroup -S group` | `groupadd -r group` | `groupadd -r group` |
| 创建用户 | `adduser -S user -G group` | `useradd -r -g group user` | `useradd -r -g group user` |
| 删除用户 | `deluser user` | `userdel user` | `userdel user` |

### 包管理器

| 操作 | Alpine (apk) | Amazon Linux 2 (yum) | Debian (apt) |
|------|-------------|---------------------|--------------|
| 更新索引 | `apk update` | `yum update` | `apt-get update` |
| 安装包 | `apk add package` | `yum install package` | `apt-get install package` |
| 删除包 | `apk del package` | `yum remove package` | `apt-get remove package` |
| 清理缓存 | `rm -rf /var/cache/apk/*` | `yum clean all` | `apt-get clean` |

### 健康检查工具

```dockerfile
# Alpine（需要安装 curl 或 wget）
RUN apk add --no-cache curl
HEALTHCHECK CMD curl -f http://localhost:8080/actuator/health || exit 1

# Amazon Linux 2（需要安装 curl）
RUN yum install -y curl && yum clean all
HEALTHCHECK CMD curl -f http://localhost:8080/actuator/health || exit 1

# 或者使用 wget（Alpine 自带）
HEALTHCHECK CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
```

## 🎯 最佳实践

### 1. 多阶段构建（如果需要编译）

```dockerfile
# 构建阶段
FROM gradle:8.5-jdk17 AS builder
WORKDIR /workspace
COPY . .
RUN gradle clean build -x test

# 运行阶段
FROM amazoncorretto:17-alpine
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar

RUN addgroup -S spring && adduser -S spring -G spring && \
    chown spring:spring app.jar

USER spring:spring
EXPOSE 8080

ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC"
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 2. 时区配置

```dockerfile
# Amazon Corretto 标准版
FROM amazoncorretto:17
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# Amazon Corretto Alpine 版
FROM amazoncorretto:17-alpine
ENV TZ=Asia/Shanghai
RUN apk add --no-cache tzdata && \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && \
    echo $TZ > /etc/timezone
```

### 3. 字体支持（如果需要生成PDF等）

```dockerfile
# Amazon Corretto 标准版
FROM amazoncorretto:17
RUN yum install -y fontconfig dejavu-sans-fonts && yum clean all

# Amazon Corretto Alpine 版
FROM amazoncorretto:17-alpine
RUN apk add --no-cache fontconfig ttf-dejavu
```

### 4. 安全加固

```dockerfile
FROM amazoncorretto:17-alpine

# 创建非 root 用户
RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app
COPY *.jar app.jar

# 设置文件权限
RUN chown spring:spring app.jar && \
    chmod 500 app.jar

# 只读文件系统（如果可能）
# docker run --read-only --tmpfs /tmp ...

USER spring:spring
EXPOSE 8080

CMD ["java", "-jar", "app.jar"]
```

## 🚀 性能优化

### JVM 参数推荐

```dockerfile
# 开发环境（快速启动）
ENV JAVA_OPTS="-Xmx512m -Xms256m \
    -XX:+UseSerialGC \
    -Djava.security.egd=file:/dev/./urandom"

# 生产环境（高吞吐量）
ENV JAVA_OPTS="-Xmx2g -Xms1g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+ParallelRefProcEnabled \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseCGroupMemoryLimitForHeap \
    -Djava.security.egd=file:/dev/./urandom"

# 生产环境（低延迟）
ENV JAVA_OPTS="-Xmx4g -Xms4g \
    -XX:+UseZGC \
    -XX:+ZGenerational \
    -Djava.security.egd=file:/dev/./urandom"
```

## 📊 迁移验证清单

- [ ] 更新 FROM 镜像为 Amazon Corretto
- [ ] 验证用户创建命令（Alpine vs 标准 Linux）
- [ ] 测试健康检查是否正常
- [ ] 验证时区配置（如果需要）
- [ ] 测试应用启动和运行
- [ ] 验证性能和内存使用
- [ ] 检查安全扫描结果
- [ ] 更新 CI/CD 配置

## 🔗 相关资源

- [Amazon Corretto 官网](https://aws.amazon.com/corretto/)
- [Amazon Corretto Docker Hub](https://hub.docker.com/_/amazoncorretto)
- [Amazon Corretto GitHub](https://github.com/corretto)
- [Eclipse Temurin](https://adoptium.net/) - 另一个推荐的替代方案

## ❓ 常见问题

### Q: 是否必须迁移？
A: 强烈推荐。OpenJDK 官方镜像已停止更新，存在安全风险。

### Q: 迁移会影响应用吗？
A: 不会。Amazon Corretto 100% 兼容 OpenJDK，应用无需修改。

### Q: Alpine 版本和标准版本选哪个？
A: 生产环境推荐 Alpine 版本（体积小、安全），开发环境用标准版本（工具完整）。

### Q: 如何验证迁移成功？
A: 
```bash
# 构建镜像
docker build -t myapp:test .

# 运行容器
docker run -d -p 8080:8080 myapp:test

# 验证 Java 版本
docker exec <container-id> java -version

# 验证应用健康
curl http://localhost:8080/actuator/health
```

### Q: 性能有差异吗？
A: Amazon Corretto 包含 AWS 的性能优化，通常性能更好或相当。

---

**建议**：尽快完成迁移，确保应用安全和稳定运行。
