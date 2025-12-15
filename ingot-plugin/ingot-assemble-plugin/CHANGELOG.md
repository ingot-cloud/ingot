# 更新日志

## [未发布] - 2025-12-15

### 🎉 重大更新

#### 1. Tag 重命名为 Env（环境）

**更改原因**：`tag` 命名不够准确，`env`（环境）更能表达配置的实际含义。

**更改内容**：
- ✅ 类名：`Tag` → `Env`
- ✅ 配置字段：`tags` → `envs`
- ✅ 配置方法：`tag()` → `env()`
- ✅ 所有文档和示例都已更新

**迁移示例**：

```groovy
// 旧配置
docker {
    tags = [
        "dev": [name: "ingot/app-dev"]
    ]
}

// 新配置
docker {
    envs = [
        "dev": [name: "ingot/app-dev"]
    ]
}
```

```groovy
// 旧配置（DSL）
tag("dev") {
    name = "ingot/app-dev"
}

// 新配置（DSL）
env("dev") {
    name = "ingot/app-dev"
}
```

#### 2. 移除默认 Docker 配置

**更改原因**：实际使用中，几乎所有项目都需要区分环境，默认配置很少使用。

**更改内容**：
- ❌ 移除 `DockerExtension.name` 字段
- ❌ 移除 `DockerExtension.saveName` 字段
- ✅ `envs` 配置变为必填项
- ✅ 每个环境必须独立配置 `name`

**影响**：
- ⚠️ 如果之前使用默认配置，需要改为环境配置
- ✅ 每个环境的配置更独立、更清晰

**迁移示例**：

```groovy
// 旧配置（默认配置）
docker {
    registry "docker-registry.ingotcloud.top"
    name "ingot/gateway"  // ❌ 不再支持
}

// 新配置（必须使用环境配置）
docker {
    registry "docker-registry.ingotcloud.top"
    
    envs = [
        "prod": [
            name: "ingot/gateway"  // ✅ 在环境中配置
        ]
    ]
}
```

#### 3. 优化配置结构

**全局配置**（可被环境覆盖）：
- ✅ `registry` - 仓库地址
- ✅ `username` - 用户名
- ✅ `password` - 密码
- ✅ `platform` - 构建平台
- ✅ `dockerCmd` - Docker 命令

**环境配置**（每个环境独立）：
- ✅ `name` - 镜像名称（**必填**）
- ✅ `dockerfileDir` - Dockerfile 目录（可选，自动匹配）
- ✅ `saveName` - 保存文件名（可选）
- ✅ 可覆盖所有全局配置

### 🚀 新增功能

#### 智能 Dockerfile 目录解析

`ShiftDockerfileTask` 支持智能的 Dockerfile 目录解析：

**解析规则**：
1. 如果指定了 `dockerfileDir`，则使用该目录（相对于 `src/main/docker`）
2. 如果未指定 `dockerfileDir`，会自动使用 `src/main/docker/{envName}`
3. 支持绝对路径

**示例**：

```groovy
envs = [
    "dev": [
        name: "ingot/app-dev"
        // 自动使用 src/main/docker/dev/
    ],
    "prod": [
        name: "ingot/app",
        dockerfileDir: "production"  // 使用 src/main/docker/production/
    ]
]
```

### ⚡ 性能优化

#### 替换废弃的 Gradle API

- ✅ `project.buildDir` → `project.layout.buildDirectory`
- ✅ 符合 Gradle 8.x 最佳实践

#### 优化输出目录结构

- ✅ 输出目录：`output/{项目名}/{版本}` → `output/{项目名}`
- ✅ 避免每次版本变更创建新目录
- ✅ Dockerfile 不会被重复复制

### 📚 文档更新

- ✅ 更新所有文档，tag → env
- ✅ 重写配置示例
- ✅ 更新 examples 目录
- ✅ 添加迁移指南

## 迁移指南

### 从旧版本迁移

#### 1. 更新配置字段名

**必须修改**：
```groovy
// 旧配置
docker {
    tags = [...]
}

// 新配置
docker {
    envs = [...]
}
```

**如果使用 DSL 方式**：
```groovy
// 旧配置
tag("dev") {...}

// 新配置
env("dev") {...}
```

#### 2. 移除默认配置

**如果使用了默认配置**：
```groovy
// 旧配置（不再支持）
docker {
    name "ingot/gateway"
    saveName "gateway.tar"
}

// 新配置
docker {
    envs = [
        "default": [
            name: "ingot/gateway",
            saveName: "gateway.tar"
        ]
    ]
}
```

#### 3. 更新环境配置

**每个环境必须配置 name**：
```groovy
// 旧配置（可能有些环境没有配置 name）
tags = [
    "dev": [
        dockerfileDir: "src/main/docker/dev"
        // name 使用默认值（不再支持）
    ]
]

// 新配置（每个环境必须配置 name）
envs = [
    "dev": [
        name: "ingot/app-dev",  // 必填
        // dockerfileDir 可以省略，自动使用 src/main/docker/dev
    ]
]
```

### 完整的迁移示例

**旧配置**：
```groovy
ingotAssemble {
    docker {
        registry "docker-registry.ingotcloud.top"
        username JY_DOCKER_REGISTRY_USERNAME
        password JY_DOCKER_REGISTRY_PASSWORD
        name "ingot/gateway"  // 默认配置
        
        tags = [
            "dev": [
                dockerfileDir: "src/main/docker/dev"
            ],
            "prod": [
                dockerfileDir: "src/main/docker/prod",
                platform: "linux/amd64,linux/arm64"
            ]
        ]
    }
}
```

**新配置**：
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
                name: "ingot/gateway-dev"  // 必须独立配置
                // dockerfileDir 自动使用 src/main/docker/dev
            ],
            "prod": [
                name: "ingot/gateway",  // 必须独立配置
                platform: "linux/amd64,linux/arm64"  // 覆盖全局配置
                // dockerfileDir 自动使用 src/main/docker/prod
            ]
        ]
    }
}
```

## 兼容性

- ⚠️ **不兼容**：需要手动迁移配置
  - `tags` → `envs`
  - 移除默认 `name` 配置
  - 每个环境必须配置 `name`
  
- ✅ **兼容**：行为保持一致
  - 全局配置的继承机制
  - 智能目录解析
  - 任务命名规则

## 测试建议

迁移后建议进行以下测试：

```bash
# 1. 清理旧的构建输出
./gradlew clean

# 2. 查看任务是否正确生成
./gradlew tasks --group=ingot

# 3. 测试环境构建
./gradlew dockerBuildDev

# 4. 验证输出目录
ls -la output/your-project-name/

# 5. 验证 Dockerfile
cat output/your-project-name/Dockerfile
```

## 下一步计划

- [ ] 支持自定义任务命名规则
- [ ] 支持镜像标签版本管理策略
- [ ] 增加构建缓存优化
- [ ] 支持 Docker Compose 配置

---

**完整文档**: [README.md](./README.md)  
**配置示例**: [examples/](./examples/)
