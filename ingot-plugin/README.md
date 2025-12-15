# ingot-plugin

Ingot 项目的 Gradle 插件集合，用于简化项目构建、Docker 镜像管理、代码生成等任务。

## 📦 插件列表

### 1. ingot-assemble-plugin

简化 Spring Boot 应用的 Docker 镜像构建、推送和保存，支持多环境配置。

**主要功能**：
- 🚀 自动生成 Docker 构建任务
- 🌍 支持多环境配置（开发、测试、生产）
- 📦 支持多平台镜像构建（linux/amd64、linux/arm64）
- 🔐 支持私有镜像仓库认证

**文档**：[ingot-assemble-plugin/README.md](./ingot-assemble-plugin/README.md)

**快速开始**：

```groovy
apply plugin: 'com.ingot.plugin.assemble'

ingotAssemble {
    docker {
        registry "docker-registry.ingotcloud.top"
        username JY_DOCKER_REGISTRY_USERNAME
        password JY_DOCKER_REGISTRY_PASSWORD
        
        tags = [
            "dev": [
                name: "ingot/app-dev",
                dockerfileDir: "src/main/docker/dev"
            ],
            "prod": [
                name: "ingot/app",
                dockerfileDir: "src/main/docker/prod"
            ]
        ]
    }
}
```

### 2. ingot-mybatisplus-plugin

MyBatis Plus 代码生成器插件。

**主要功能**：
- 🔨 根据数据库表生成实体类、Mapper、Service 等代码
- ⚙️ 支持自定义模板和配置

**使用方式**：

```groovy
apply plugin: 'com.ingot.plugin.mybatis'

ingotMybatis {
    // 配置数据库连接等信息
}
```

## 🚀 安装和使用

### 本地安装

在插件目录下执行：

```bash
cd ingot-plugin
./gradlew publishToMavenLocal
```

### 在项目中使用

在模块的 `build.gradle` 中应用插件：

```groovy
apply plugin: 'com.ingot.plugin.assemble'
```

或使用新的 plugins DSL：

```groovy
plugins {
    id 'com.ingot.plugin.assemble' version '0.1.0'
}
```

## 📚 文档

- [ingot-assemble-plugin 完整文档](./ingot-assemble-plugin/README.md)
- [配置示例](./ingot-assemble-plugin/examples/)
- [Docker 多环境构建指南](../docs/DOCKER-MULTI-ENVIRONMENT.md)

## 🔧 开发

### 项目结构

```
ingot-plugin/
├── ingot-assemble-plugin/          # Docker 构建插件
│   ├── README.md                   # 插件文档
│   ├── examples/                   # 配置示例
│   │   ├── README.md
│   │   ├── basic-single-env.gradle
│   │   ├── multi-env-map.gradle
│   │   ├── multi-env-dsl.gradle
│   │   └── advanced-multi-registry.gradle
│   └── src/
│       └── main/
│           └── groovy/
│               └── com/ingot/plugin/assemble/
├── ingot-mybatisplus-plugin/       # MyBatis Plus 插件
│   └── src/
│       └── main/
│           └── groovy/
│               └── com/ingot/plugin/mybatis/
└── README.md                        # 本文件
```

### 构建插件

```bash
# 构建所有插件
./gradlew build

# 发布到本地 Maven 仓库
./gradlew publishToMavenLocal

# 清理构建
./gradlew clean
```

### 测试插件

在 `ingot-service` 中的任意模块应用插件进行测试：

```bash
# 测试 Docker 构建
./gradlew :ingot-service:ingot-gateway:dockerBuildDev

# 查看生成的任务
./gradlew :ingot-service:ingot-gateway:tasks --group=ingot
```

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！

## 📄 License

[LICENSE](../LICENSE)
