# ingot-assemble-plugin

Gradle 插件，用于简化 Spring Boot 应用的 Docker 镜像构建、推送和保存，支持多环境配置。

## ✨ 特性

- 🚀 **简化 Docker 构建流程** - 自动生成 Docker 构建任务
- 🌍 **多环境支持** - 为开发、测试、生产等环境配置不同的 Docker 构建参数
- 🔧 **灵活配置** - 每个环境可独立配置 Dockerfile、镜像名称、仓库地址等
- 📦 **多平台构建** - 支持 linux/amd64、linux/arm64 等多平台镜像构建
- 🔐 **安全认证** - 支持私有镜像仓库认证
- 💾 **离线部署** - 支持将镜像保存为 tar 文件

## 📦 安装

### 方式一：通过插件仓库（推荐）

在项目根目录的 `settings.gradle` 中添加：

```groovy
pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
```

在需要使用的模块的 `build.gradle` 中应用插件：

```groovy
plugins {
    id 'com.ingot.plugin.assemble' version '0.1.0'
}
```

### 方式二：本地安装

```bash
cd ingot-plugin/ingot-assemble-plugin
./gradlew publishToMavenLocal
```

然后在项目中应用：

```groovy
apply plugin: 'com.ingot.plugin.assemble'
```

## 🚀 快速开始

### Step 1: 创建 Dockerfile 目录结构

```bash
mkdir -p src/main/docker/{dev,test,prod}
```

### Step 2: 创建 Dockerfile

插件支持**智能 Dockerfile 目录解析**：

- 如果指定了 `dockerfileDir`，则使用该目录（相对于 `src/main/docker`）
- 如果未指定 `dockerfileDir`，会自动使用 `src/main/docker/{tag}` 目录
- 如果没有 tag，则使用 `src/main/docker` 目录

**推荐目录结构**（自动匹配）：

```
src/main/docker/
├── dev/
│   └── Dockerfile          # 开发环境（tag="dev" 时自动使用）
├── test/
│   └── Dockerfile          # 测试环境（tag="test" 时自动使用）
└── prod/
    └── Dockerfile          # 生产环境（tag="prod" 时自动使用）
```

**开发环境：** `src/main/docker/dev/Dockerfile`
```dockerfile
# 使用 Amazon Corretto 17（OpenJDK 已停止维护）
FROM amazoncorretto:17

LABEL maintainer="ingot-cloud"
LABEL environment="development"

WORKDIR /app

# 复制 JAR 文件
COPY *.jar app.jar

# 暴露端口
EXPOSE 8080

# 开发环境 JVM 参数
ENV JAVA_OPTS="-Xmx512m -Xms256m \
    -Dspring.profiles.active=dev \
    -Djava.security.egd=file:/dev/./urandom"

# 启动应用
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

**生产环境：** `src/main/docker/prod/Dockerfile`
```dockerfile
# 使用 Amazon Corretto 17 Alpine 版本（体积更小）
FROM amazoncorretto:17-alpine

LABEL maintainer="ingot-cloud"
LABEL environment="production"

WORKDIR /app

# 复制 JAR 文件
COPY *.jar app.jar

# 创建非 root 用户（安全最佳实践）
RUN addgroup -S spring && adduser -S spring -G spring && \
    chown spring:spring app.jar

# 切换到非 root 用户
USER spring:spring

# 暴露端口
EXPOSE 8080

# 生产环境 JVM 参数（优化配置）
ENV JAVA_OPTS="-Xmx2g -Xms1g \
    -XX:+UseG1GC \
    -XX:MaxGCPauseMillis=200 \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/tmp/heapdump.hprof \
    -Dspring.profiles.active=prod \
    -Djava.security.egd=file:/dev/./urandom"

# 健康检查（可选）
HEALTHCHECK --interval=30s --timeout=3s --start-period=90s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 启动应用
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### Step 3: 配置插件

创建或编辑 `ingot-assemble.gradle` 文件：

```groovy
apply plugin: 'com.ingot.plugin.assemble'

ingotAssemble {
    docker {
        // 全局配置（可选，可被环境覆盖）
        registry "docker-registry.ingotcloud.top"
        username JY_DOCKER_REGISTRY_USERNAME
        password JY_DOCKER_REGISTRY_PASSWORD
        platform "linux/amd64"
        
        // 环境配置（必填）
        envs = [
            "dev": [
                name: "ingot/your-app-dev"  // 环境独立配置镜像名称
                // dockerfileDir 可以省略，会自动使用 src/main/docker/dev
            ],
            "prod": [
                name: "ingot/your-app",  // 环境独立配置镜像名称
                // dockerfileDir 可以省略，会自动使用 src/main/docker/prod
                platform: "linux/amd64,linux/arm64"  // 可覆盖全局配置
            ]
        ]
    }
}
```

**💡 智能目录解析**：
- `env="dev"` → 自动使用 `src/main/docker/dev/`
- `env="prod"` → 自动使用 `src/main/docker/prod/`
- 也可以手动指定 `dockerfileDir: "custom/path"`（相对于 `src/main/docker`）

**或者使用 DSL 方式：**

```groovy
ingotAssemble {
    docker {
        // 全局配置
        registry "docker-registry.ingotcloud.top"
        username JY_DOCKER_REGISTRY_USERNAME
        password JY_DOCKER_REGISTRY_PASSWORD
        platform "linux/amd64"
        
        // 环境配置（DSL 方式）
        env("dev") {
            name = "ingot/your-app-dev"  // 必填
            // dockerfileDir 可省略，自动使用 src/main/docker/dev
        }
        
        env("prod") {
            name = "ingot/your-app"  // 必填
            // dockerfileDir 可省略，自动使用 src/main/docker/prod
            platform = "linux/amd64,linux/arm64"  // 覆盖全局配置
        }
    }
}
```

### Step 4: 执行构建

```bash
# 查看可用任务
./gradlew tasks --group=ingot

# 构建开发环境镜像
./gradlew dockerBuildDev

# 构建并推送生产环境镜像
./gradlew dockerPushProd

# 保存镜像为文件（用于离线部署）
./gradlew dockerSaveProd
```

## 📖 配置说明

### 全局配置（DockerExtension）

全局配置可以被所有环境继承，环境可以覆盖全局配置：

| 配置项 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| `dockerCmd` | String | `docker` | 否 | Docker 命令路径 |
| `platform` | String | `linux/amd64` | 否 | 构建平台，多平台用逗号分隔 |
| `registry` | String | `""` | 否 | 镜像仓库地址 |
| `username` | String | `""` | 否 | 仓库登录用户名 |
| `password` | String | `""` | 否 | 仓库登录密码 |
| `dockerfileDir` | String | `""` | 否 | Dockerfile 基础目录（通常不需要配置） |
| `envs` | Map | `{}` | **是** | 环境配置（必须至少配置一个环境） |

### 环境配置（Env）

每个环境必须独立配置，可以继承并覆盖全局配置：

| 配置项 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| `name` | String | **是** | 镜像名称（每个环境必须独立配置） |
| `dockerfileDir` | String | 否 | Dockerfile 目录（相对于 `src/main/docker`，不设置则自动使用环境名称作为子目录） |
| `registry` | String | 否 | 镜像仓库地址（可选，不设置则使用全局配置） |
| `platform` | String | 否 | 构建平台（可选，不设置则使用全局配置） |
| `username` | String | 否 | 仓库用户名（可选，不设置则使用全局配置） |
| `password` | String | 否 | 仓库密码（可选，不设置则使用全局配置） |
| `dockerCmd` | String | 否 | Docker 命令（可选，不设置则使用全局配置） |
| `saveName` | String | 否 | 保存的镜像文件名（可选） |

**配置优先级：** `环境配置` > `全局配置`

## 🎯 使用场景

### 场景 1：基础多环境配置

```groovy
ingotAssemble {
    docker {
        // 全局配置
        registry "docker-registry.ingotcloud.top"
        username JY_DOCKER_REGISTRY_USERNAME
        password JY_DOCKER_REGISTRY_PASSWORD
        platform "linux/amd64"
        
        // 环境配置（必填）
        envs = [
            "dev": [
                name: "ingot/gateway-dev"
                // 自动使用 src/main/docker/dev
            ],
            "test": [
                name: "ingot/gateway-test"
                // 自动使用 src/main/docker/test
            ],
            "prod": [
                name: "ingot/gateway",
                platform: "linux/amd64,linux/arm64"  // 覆盖全局配置
                // 自动使用 src/main/docker/prod
            ]
        ]
    }
}
```

**生成的任务：**
- 开发环境：`dockerBuildDev`, `dockerPushDev`, `dockerSaveDev`
- 测试环境：`dockerBuildTest`, `dockerPushTest`, `dockerSaveTest`
- 生产环境：`dockerBuildProd`, `dockerPushProd`, `dockerSaveProd`

### 场景 2：不同环境使用不同仓库

```groovy
ingotAssemble {
    docker {
        // 默认配置
        platform "linux/amd64"
        
        envs = [
            "test": [
                name: "ingot/gateway-test",
                registry: "test-harbor.company.com",
                username: TEST_USERNAME,
                password: TEST_PASSWORD
            ],
            "prod": [
                name: "ingot/gateway",
                registry: "prod-harbor.company.com",
                username: PROD_USERNAME,
                password: PROD_PASSWORD,
                platform: "linux/amd64,linux/arm64"
            ]
        ]
    }
}
```

### 场景 3：使用 DSL 方式配置

```groovy
ingotAssemble {
    docker {
        registry "docker-registry.ingotcloud.top"
        username JY_DOCKER_REGISTRY_USERNAME
        password JY_DOCKER_REGISTRY_PASSWORD
        
        env("dev") {
            name = "ingot/gateway-dev"
        }
        
        env("prod") {
            name = "ingot/gateway"
            platform = "linux/amd64,linux/arm64"
            saveName = "gateway-prod.tar"
        }
    }
}
```

## 📋 常用命令

| 操作 | 命令 |
|------|------|
| 查看所有 Docker 任务 | `./gradlew tasks --group=ingot` |
| 构建开发环境镜像 | `./gradlew dockerBuildDev` |
| 构建测试环境镜像 | `./gradlew dockerBuildTest` |
| 构建生产环境镜像 | `./gradlew dockerBuildProd` |
| 推送测试环境镜像 | `./gradlew dockerPushTest` |
| 推送生产环境镜像 | `./gradlew dockerPushProd` |
| 保存镜像为文件 | `./gradlew dockerSaveDev` |
| 仅复制 Dockerfile | `./gradlew shiftDockerfileDev` |

## 📁 示例配置

查看 [examples](./examples) 目录获取更多配置示例：

- `basic-single-env.gradle` - 单环境基础配置
- `multi-env-map.gradle` - 多环境 Map 配置
- `multi-env-dsl.gradle` - 多环境 DSL 配置
- `advanced-multi-registry.gradle` - 多仓库高级配置

## 💡 最佳实践

### 1. 使用环境变量管理敏感信息

在 `gradle.properties` 中配置：

```properties
JY_DOCKER_REGISTRY_USERNAME=your-username
JY_DOCKER_REGISTRY_PASSWORD=your-password
```

### 2. 不同环境使用不同的基础镜像

- **开发/测试环境**：`openjdk:17-jdk-slim`（包含调试工具）
- **生产环境**：`openjdk:17-jdk-alpine`（体积更小，更安全）

### 3. 生产环境使用非 root 用户

```dockerfile
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
```

### 4. 添加健康检查

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
```

### 5. 多平台构建配置

生产环境支持多平台：

```groovy
tag("prod") {
    platform = "linux/amd64,linux/arm64"  // 同时支持 x86 和 ARM
}
```

**注意**：多平台构建需要启用 Docker buildx：

```bash
docker buildx create --use --name multi-platform-builder
docker buildx inspect --bootstrap
```

## 🔧 故障排查

### 问题 1：任务未找到

**错误**：`Task 'dockerBuildDev' not found`

**解决**：
1. 确认已应用插件：`apply plugin: 'com.ingot.plugin.assemble'`
2. 运行 `./gradlew tasks --group=ingot` 查看可用任务
3. 检查配置是否在 `project.afterEvaluate` 中生效

### 问题 2：Dockerfile 未找到

**错误**：`Dockerfile not found in src/main/docker/prod`

**解决**：
1. 检查 `dockerfileDir` 配置路径是否正确
2. 确认 Dockerfile 文件存在
3. 路径是相对于项目根目录的

### 问题 3：多平台构建失败

**错误**：`multiple platforms feature is currently not supported`

**解决**：
```bash
# 启用 Docker buildx
docker buildx create --use
docker buildx inspect --bootstrap
```

### 问题 4：认证失败

**错误**：`unauthorized: authentication required`

**解决**：
1. 检查 `username` 和 `password` 配置
2. 确保凭据在环境变量或 `gradle.properties` 中配置正确
3. 手动登录测试：`docker login <registry>`

## 📚 相关文档

- [完整使用指南](../../docs/DOCKER-MULTI-ENVIRONMENT.md)
- [Docker Buildx 文档](https://docs.docker.com/buildx/working-with-buildx/)
- [多阶段构建最佳实践](https://docs.docker.com/develop/develop-images/multistage-build/)

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 License

[LICENSE](../../LICENSE)

---

**提示**：所有 Tag 配置项都是可选的，未配置的项会自动继承全局默认值！
