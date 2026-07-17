# ingot-assemble-plugin 配置示例

本目录包含 `ingot-assemble-plugin` 的各种配置示例，涵盖从基础到高级的使用场景。

## 📁 示例文件

| 文件 | 说明 | 适用场景 |
|------|------|----------|
| `basic-multi-env.gradle` | 基础多环境配置（推荐） | 为多个环境（开发、测试、生产）构建不同的镜像 |
| `multi-env-dsl.gradle` | 多环境 DSL 配置 | 使用 DSL 风格配置多环境 |
| `advanced-multi-registry.gradle` | 多仓库高级配置 | 不同环境推送到不同的镜像仓库 |

## 🚀 快速使用

### 1. 选择合适的示例

根据你的需求选择相应的示例文件：

- **新手用户**：从 `basic-multi-env.gradle` 开始（推荐）
- **喜欢 DSL 风格**：使用 `multi-env-dsl.gradle`
- **复杂场景**：参考 `advanced-multi-registry.gradle`

### 2. 复制配置

将选择的示例文件内容复制到你的模块目录下，创建 `ingot-assemble.gradle` 文件：

```bash
# 例如：在 ingot-gateway 模块中
cp examples/basic-multi-env.gradle ingot-service/ingot-gateway/ingot-assemble.gradle
```

### 3. 修改配置

根据实际情况修改配置：

- 修改镜像名称：`name: "ingot/your-app"`
- 修改仓库地址：`registry: "your-registry.com"`
- 修改 Dockerfile 路径：`dockerfileDir: "src/main/docker/prod"`
- 配置认证信息：`username` 和 `password`

### 4. 应用配置

在模块的 `build.gradle` 中引入配置：

```groovy
// build.gradle
apply from: 'ingot-assemble.gradle'
```

### 5. 执行构建

```bash
# 查看可用任务
./gradlew tasks --group=ingot

# 构建镜像
./gradlew dockerBuildProd
```

## 📖 示例详解

### basic-multi-env.gradle（推荐）

**适用场景**：为多个环境（开发、测试、生产）构建不同的镜像

**配置特点**：
- ✅ 使用 Map 配置，简洁直观
- ✅ 每个环境必须独立配置镜像名称
- ✅ 支持配置继承和覆盖
- ✅ 每个环境可以有独立的 Dockerfile
- ✅ 自动目录匹配（env="dev" → src/main/docker/dev）

**生成的任务**：
- 开发环境：`dockerBuildDev`, `dockerPushDev`, `dockerSaveDev`
- 测试环境：`dockerBuildTest`, `dockerPushTest`, `dockerSaveTest`
- 生产环境：`dockerBuildProd`, `dockerPushProd`, `dockerSaveProd`

### multi-env-dsl.gradle

**适用场景**：使用更符合 Gradle 风格的 DSL 配置

**配置特点**：
- ✅ 使用 `env()` 方法配置，更符合 Gradle 风格
- ✅ 支持代码补全和类型检查（IDE）
- ✅ 适合复杂配置和扩展
- ✅ 配置逻辑更清晰

**与 Map 方式的对比**：

```groovy
// Map 方式
envs = [
    "dev": [
        name: "ingot/gateway-dev"
    ]
]

// DSL 方式
env("dev") {
    name = "ingot/gateway-dev"
}
```

### advanced-multi-registry.gradle

**适用场景**：不同环境需要推送到不同的镜像仓库

**配置特点**：
- ✅ 支持多个镜像仓库
- ✅ 每个环境可以有独立的认证信息
- ✅ 适合企业级应用场景
- ✅ 支持多环境隔离（开发、测试、预发布、生产）

**使用案例**：
- 开发环境 → 内网开发仓库
- 测试环境 → Harbor 测试仓库
- 预发布环境 → 生产仓库（测试项目）
- 生产环境 → 生产仓库（正式项目）

## 🔐 认证信息配置

### 方式一：gradle.properties（推荐）

在项目根目录或用户目录的 `gradle.properties` 中配置：

```properties
# 单环境
JY_DOCKER_REGISTRY_USERNAME=your-username
JY_DOCKER_REGISTRY_PASSWORD=your-password

# 多环境
DEV_DOCKER_USERNAME=dev-user
DEV_DOCKER_PASSWORD=dev-password
TEST_DOCKER_USERNAME=test-user
TEST_DOCKER_PASSWORD=test-password
PROD_DOCKER_USERNAME=prod-user
PROD_DOCKER_PASSWORD=prod-password
```

**注意**：不要将 `gradle.properties` 提交到版本控制系统！

### 方式二：环境变量

```bash
export JY_DOCKER_REGISTRY_USERNAME=your-username
export JY_DOCKER_REGISTRY_PASSWORD=your-password
```

### 方式三：CI/CD 平台的密钥管理

在 Jenkins、GitLab CI、GitHub Actions 等平台中配置密钥，然后在构建时注入：

```bash
./gradlew dockerPushProd \
  -PPROD_DOCKER_USERNAME=$DOCKER_USERNAME \
  -PPROD_DOCKER_PASSWORD=$DOCKER_PASSWORD
```

## 📁 目录结构建议

```
your-module/
├── build.gradle
├── ingot-assemble.gradle          # 插件配置文件
└── src/
    └── main/
        ├── java/
        ├── resources/
        └── docker/                 # Docker 相关文件
            ├── dev/
            │   └── Dockerfile      # 开发环境 Dockerfile
            ├── test/
            │   └── Dockerfile      # 测试环境 Dockerfile
            └── prod/
                └── Dockerfile      # 生产环境 Dockerfile
```

## 💡 使用技巧

### 技巧 1：为不同环境使用不同的基础镜像

```dockerfile
# 开发环境：Amazon Corretto 标准版（功能完整）
FROM amazoncorretto:17

# 生产环境：Amazon Corretto Alpine 版本（体积更小，更安全）
FROM amazoncorretto:17-alpine

# 如果需要 Debian 基础
FROM amazoncorretto:17-debian
```

**镜像对比**：

| 镜像 | 基础系统 | 大小 | 适用场景 |
|------|---------|------|----------|
| `amazoncorretto:17` | Amazon Linux 2 | ~400MB | 开发/测试环境 |
| `amazoncorretto:17-alpine` | Alpine Linux | ~200MB | 生产环境（推荐） |
| `amazoncorretto:17-debian` | Debian | ~350MB | 需要 Debian 生态 |

**注意**：OpenJDK 官方镜像已停止维护，建议使用 Amazon Corretto、Eclipse Temurin 等替代方案。

### 技巧 2：使用环境变量控制 Spring Profile

```dockerfile
# 开发环境
ENV JAVA_OPTS="-Dspring.profiles.active=dev"

# 生产环境
ENV JAVA_OPTS="-Dspring.profiles.active=prod"
```

### 技巧 3：生产环境使用非 root 用户

```dockerfile
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring
```

### 技巧 4：添加健康检查

```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1
```

### 技巧 5：多平台构建

```groovy
tag("prod") {
    platform = "linux/amd64,linux/arm64"  // 同时构建 x86 和 ARM
}
```

## 🔗 相关文档

- [插件完整文档](../README.md)
- [Docker 多环境构建指南](../../../docs/guides/deployment/DOCKER-MULTI-ENVIRONMENT.md)
- [Docker Buildx 文档](https://docs.docker.com/buildx/working-with-buildx/)

## ❓ 常见问题

### Q: 如何查看生成了哪些任务？

```bash
./gradlew tasks --group=ingot
```

### Q: 如何只复制 Dockerfile 不构建？

```bash
./gradlew shiftDockerfileDev
```

### Q: 如何在构建时指定认证信息？

```bash
./gradlew dockerPushProd \
  -Pusername=your-username \
  -Ppassword=your-password
```

### Q: Map 方式和 DSL 方式有什么区别？

两种方式功能完全相同，只是配置风格不同：
- **Map 方式**：更简洁，适合简单配置
- **DSL 方式**：更符合 Gradle 风格，适合复杂配置

选择你喜欢的方式即可。

---

如有问题，请参考[主文档](../README.md)或提交 Issue。
