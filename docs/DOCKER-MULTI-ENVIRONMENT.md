# Docker 多环境构建配置指南

> 📚 **插件完整文档**: [ingot-assemble-plugin README](../ingot-plugin/ingot-assemble-plugin/README.md)  
> 📁 **配置示例**: [examples 目录](../ingot-plugin/ingot-assemble-plugin/examples/)

## 📋 概述

`ingot-assemble-plugin` 支持为不同环境（开发、测试、生产）构建不同的 Docker 镜像，每个环境可以使用独立的：

- ✅ Dockerfile（位于不同目录）
- ✅ 镜像名称和标签
- ✅ 构建平台（linux/amd64、linux/arm64 等）
- ✅ 镜像仓库地址
- ✅ 认证信息
- ✅ 其他 Docker 构建参数

## 🔧 配置方式

### 方式一：Map 配置（推荐用于简单场景）

```groovy
apply plugin: 'com.ingot.plugin.assemble'

ingotAssemble {
    docker {
        // 全局默认配置
        registry "docker-registry.ingotcloud.top"
        username JY_DOCKER_REGISTRY_USERNAME
        password JY_DOCKER_REGISTRY_PASSWORD
        dockerCmd "docker"
        platform "linux/amd64"
        
        // 多环境配置（Map 方式）
        tags = [
            "dev": [
                name: "ingot/gateway-dev",
                dockerfileDir: "src/main/docker/dev",
                platform: "linux/amd64"
            ],
            "test": [
                name: "ingot/gateway-test",
                dockerfileDir: "src/main/docker/test",
                platform: "linux/amd64",
                saveName: "gateway-test.tar"
            ],
            "prod": [
                name: "ingot/gateway",
                dockerfileDir: "src/main/docker/prod",
                platform: "linux/amd64,linux/arm64",
                saveName: "gateway-prod.tar"
            ]
        ]
    }
}
```

### 方式二：DSL 配置（推荐用于复杂场景）

```groovy
apply plugin: 'com.ingot.plugin.assemble'

ingotAssemble {
    docker {
        // 全局默认配置
        registry "docker-registry.ingotcloud.top"
        username JY_DOCKER_REGISTRY_USERNAME
        password JY_DOCKER_REGISTRY_PASSWORD
        dockerCmd "docker"
        platform "linux/amd64"
        
        // 开发环境配置
        tag("dev") {
            name = "ingot/gateway-dev"
            dockerfileDir = "src/main/docker/dev"
            platform = "linux/amd64"
        }
        
        // 测试环境配置
        tag("test") {
            name = "ingot/gateway-test"
            dockerfileDir = "src/main/docker/test"
            platform = "linux/amd64"
            saveName = "gateway-test.tar"
            // 可以覆盖仓库地址
            // registry = "test-harbor.ingotcloud.top"
        }
        
        // 生产环境配置
        tag("prod") {
            name = "ingot/gateway"
            dockerfileDir = "src/main/docker/prod"
            platform = "linux/amd64,linux/arm64"
            saveName = "gateway-prod.tar"
            // 可以使用独立的生产仓库
            // registry = "prod-harbor.ingotcloud.top"
            // username = PROD_DOCKER_USERNAME
            // password = PROD_DOCKER_PASSWORD
        }
    }
}
```

## 📁 目录结构

```
src/main/
├── java/
│   └── com/ingot/cloud/gateway/
│       └── InGatewayApplication.java
├── resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-test.yml
│   └── application-prod.yml
└── docker/
    ├── dev/
    │   └── Dockerfile          # 开发环境 Dockerfile
    ├── test/
    │   └── Dockerfile          # 测试环境 Dockerfile
    └── prod/
        └── Dockerfile          # 生产环境 Dockerfile
```

## 📝 Dockerfile 示例

### 开发环境 Dockerfile

**`src/main/docker/dev/Dockerfile`**

```dockerfile
FROM openjdk:17-jdk-slim

LABEL maintainer="ingot-cloud"
LABEL environment="development"

WORKDIR /app

# 复制构建好的 JAR 文件
COPY *.jar app.jar

# 暴露端口
EXPOSE 8080

# 开发环境 JVM 参数（较小的内存配置）
ENV JAVA_OPTS="-Xmx512m -Xms256m \
    -Dspring.profiles.active=dev \
    -Djava.security.egd=file:/dev/./urandom"

# 启动应用
CMD java $JAVA_OPTS -jar app.jar
```

### 测试环境 Dockerfile

**`src/main/docker/test/Dockerfile`**

```dockerfile
FROM openjdk:17-jdk-slim

LABEL maintainer="ingot-cloud"
LABEL environment="testing"

WORKDIR /app

COPY *.jar app.jar

EXPOSE 8080

# 测试环境 JVM 参数（中等内存配置）
ENV JAVA_OPTS="-Xmx1g -Xms512m \
    -Dspring.profiles.active=test \
    -Djava.security.egd=file:/dev/./urandom"

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

CMD java $JAVA_OPTS -jar app.jar
```

### 生产环境 Dockerfile

**`src/main/docker/prod/Dockerfile`**

```dockerfile
FROM openjdk:17-jdk-alpine

LABEL maintainer="ingot-cloud"
LABEL environment="production"

WORKDIR /app

COPY *.jar app.jar

# 创建非 root 用户（安全最佳实践）
RUN addgroup -S spring && adduser -S spring -G spring

# 更改文件所有者
RUN chown spring:spring app.jar

# 切换到非 root 用户
USER spring:spring

EXPOSE 8080

# 生产环境 JVM 参数（优化配置）
ENV JAVA_OPTS="-Xmx2g -Xms1g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof \
    -Dspring.profiles.active=prod \
    -Djava.security.egd=file:/dev/./urandom"

# 健康检查
HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

CMD java $JAVA_OPTS -jar app.jar
```

## 🚀 使用命令

### 查看所有任务

```bash
./gradlew tasks --group=ingot
```

输出示例：

```
Ingot tasks
-----------
dockerBuildDev - Build Docker image 'ingot/gateway-dev' for dev environment
dockerBuildProd - Build Docker image 'ingot/gateway' for prod environment
dockerBuildTest - Build Docker image 'ingot/gateway-test' for test environment
dockerPushDev - Push Docker image 'ingot/gateway-dev' for dev environment
dockerPushProd - Push Docker image 'ingot/gateway' for prod environment
dockerPushTest - Push Docker image 'ingot/gateway-test' for test environment
dockerSaveDev - Save Docker image 'ingot/gateway-dev' for dev environment
dockerSaveProd - Save Docker image 'ingot/gateway' for prod environment
dockerSaveTest - Save Docker image 'ingot/gateway-test' for test environment
shiftDockerfileDev - Shift dockerfile for dev environment
shiftDockerfileProd - Shift dockerfile for prod environment
shiftDockerfileTest - Shift dockerfile for test environment
```

### 构建镜像

```bash
# 构建开发环境
./gradlew dockerBuildDev

# 构建测试环境
./gradlew dockerBuildTest

# 构建生产环境
./gradlew dockerBuildProd

# 在多模块项目中构建指定模块的镜像
./gradlew :ingot-service:ingot-gateway:dockerBuildProd
```

### 推送镜像

```bash
# 构建并推送测试环境镜像
./gradlew dockerPushTest

# 构建并推送生产环境镜像
./gradlew dockerPushProd
```

### 保存镜像（用于离线部署）

```bash
# 保存生产环境镜像为 tar 文件
./gradlew dockerSaveProd

# 镜像文件将保存在构建输出目录中
# 例如：build/ingot-assemble/ingot-gateway/gateway-prod.tar
```

### 在 CI/CD 中使用

```bash
# Jenkins/GitLab CI 示例
./gradlew clean build dockerPushProd -Dorg.gradle.daemon=false

# GitHub Actions 示例
./gradlew clean build dockerPushProd --no-daemon
```

## 🔍 配置说明

### DockerExtension 全局配置

| 配置项 | 类型 | 必填 | 默认值 | 说明 |
|--------|------|------|--------|------|
| `dockerCmd` | String | 否 | `docker` | Docker 命令路径 |
| `platform` | String | 否 | `linux/amd64` | 构建平台，支持多平台：`linux/amd64,linux/arm64` |
| `registry` | String | 否 | 空 | 镜像仓库地址 |
| `username` | String | 否 | 空 | 仓库登录用户名 |
| `password` | String | 否 | 空 | 仓库登录密码 |
| `dockerfileDir` | String | 否 | 空 | Dockerfile 所在目录 |
| `name` | String | 否 | 空 | 镜像名称 |
| `saveName` | String | 否 | 空 | 保存的镜像文件名 |

### Tag 环境配置

Tag 配置会**继承并覆盖** DockerExtension 的全局配置：

| 配置项 | 说明 | 覆盖规则 |
|--------|------|----------|
| `name` | 镜像名称 | 如果设置，覆盖全局 `name` |
| `dockerfileDir` | Dockerfile 目录 | 如果设置，覆盖全局 `dockerfileDir` |
| `registry` | 仓库地址 | 如果设置，覆盖全局 `registry` |
| `platform` | 构建平台 | 如果设置，覆盖全局 `platform` |
| `dockerCmd` | Docker 命令 | 如果设置，覆盖全局 `dockerCmd` |
| `username` | 登录用户名 | 如果设置，覆盖全局 `username` |
| `password` | 登录密码 | 如果设置，覆盖全局 `password` |
| `saveName` | 保存文件名 | 如果设置，覆盖全局 `saveName` |

## 💡 最佳实践

### 1. 使用环境变量存储敏感信息

在 `gradle.properties` 或环境变量中配置：

```properties
# gradle.properties (不要提交到 Git)
JY_DOCKER_REGISTRY_USERNAME=your-username
JY_DOCKER_REGISTRY_PASSWORD=your-password
```

### 2. 不同环境使用不同的基础镜像

- **开发环境**：使用 `openjdk:17-jdk-slim`（包含调试工具）
- **测试环境**：使用 `openjdk:17-jdk-slim`
- **生产环境**：使用 `openjdk:17-jdk-alpine`（体积更小，更安全）

### 3. 生产环境使用非 root 用户

```dockerfile
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
```

### 4. 添加健康检查

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1
```

### 5. 多平台构建

生产环境支持多平台构建：

```groovy
tag("prod") {
    platform = "linux/amd64,linux/arm64"  // 同时支持 x86 和 ARM
}
```

**注意**：多平台构建需要使用 `docker buildx`。

## 🔧 故障排查

### 问题 1：任务未找到

**错误**：`Task 'dockerBuildDev' not found`

**原因**：插件未正确应用或配置未生效

**解决**：
1. 确认已应用插件：`apply plugin: 'com.ingot.plugin.assemble'`
2. 运行 `./gradlew tasks --group=ingot` 查看可用任务
3. 确保在 `project.afterEvaluate` 中配置已生效

### 问题 2：Dockerfile 未找到

**错误**：`Dockerfile not found in src/main/docker/prod`

**解决**：
1. 检查 `dockerfileDir` 配置是否正确
2. 确认 Dockerfile 文件存在且路径正确
3. 确保 `shiftDockerfile` 任务成功执行

### 问题 3：多平台构建失败

**错误**：`multiple platforms feature is currently not supported`

**解决**：
1. 确保使用 `docker buildx`
2. 创建 buildx 实例：
   ```bash
   docker buildx create --use --name multi-platform-builder
   docker buildx inspect --bootstrap
   ```
3. 在配置中添加 `--load` 参数（仅单平台时）

### 问题 4：认证失败

**错误**：`unauthorized: authentication required`

**解决**：
1. 检查 `username` 和 `password` 配置
2. 确保环境变量或 `gradle.properties` 中有正确的凭据
3. 手动登录测试：`docker login <registry>`

## 📚 相关文档

- [Gradle Plugin 开发指南](../README.md)
- [Docker Buildx 文档](https://docs.docker.com/buildx/working-with-buildx/)
- [多阶段构建最佳实践](https://docs.docker.com/develop/develop-images/multistage-build/)

## 🎉 总结

通过 `ingot-assemble-plugin` 的多环境配置功能，你可以：

✅ 为每个环境使用独立的 Dockerfile  
✅ 灵活配置镜像名称、标签和仓库  
✅ 支持多平台构建（x86 和 ARM）  
✅ 统一管理 Docker 构建流程  
✅ 在 CI/CD 中轻松集成  

享受高效的 Docker 镜像构建体验！🚀
