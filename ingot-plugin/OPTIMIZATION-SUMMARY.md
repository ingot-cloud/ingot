# ingot-assemble-plugin 优化总结

最后更新：2025-12-15

## 🎉 重大优化

### 1. **Tag 重命名为 Env**

**原因**：`tag` 命名不够准确，`env`（环境）更能准确表达配置的实际含义。

**更改内容**：
| 项目 | 旧名称 | 新名称 |
|------|--------|--------|
| 类名 | `Tag` | `Env` |
| 配置字段 | `tags` | `envs` |
| 配置方法 | `tag()` | `env()` |
| 文件名 | `Tag.groovy` | `Env.groovy` |

### 2. **移除默认配置，强制环境配置**

**原因**：实际使用中，几乎所有项目都需要区分环境，单一默认配置很少使用。

**移除的字段**：
- ❌ `DockerExtension.name`
- ❌ `DockerExtension.saveName`

**新的要求**：
- ✅ `envs` 配置变为必填项（至少配置一个环境）
- ✅ 每个环境必须独立配置 `name`
- ✅ `saveName` 由各环境独立配置（可选）

### 3. **优化配置结构**

#### 全局配置（所有环境共享，可被覆盖）
- `registry` - 镜像仓库地址
- `username` - 仓库用户名
- `password` - 仓库密码
- `platform` - 构建平台
- `dockerCmd` - Docker 命令路径
- `dockerfileDir` - Dockerfile 基础目录（通常不需要）

#### 环境配置（每个环境独立）
- `name` - **镜像名称（必填）**
- `dockerfileDir` - Dockerfile 目录（可选，自动匹配环境名称）
- `saveName` - 保存文件名（可选）
- 可覆盖所有全局配置

## 📊 配置对比

### 旧配置方式

```groovy
ingotAssemble {
    docker {
        registry "docker-registry.ingotcloud.top"
        username JY_DOCKER_REGISTRY_USERNAME
        password JY_DOCKER_REGISTRY_PASSWORD
        name "ingot/gateway"  // 默认配置（已移除）
        
        tags = [  // 旧字段名
            "dev": [
                dockerfileDir: "src/main/docker/dev"
            ]
        ]
    }
}
```

### 新配置方式（推荐）

```groovy
ingotAssemble {
    docker {
        // 全局配置
        registry "docker-registry.ingotcloud.top"
        username JY_DOCKER_REGISTRY_USERNAME
        password JY_DOCKER_REGISTRY_PASSWORD
        platform "linux/amd64"
        
        // 环境配置（必填）
        envs = [  // 新字段名
            "dev": [
                name: "ingot/gateway-dev"  // 必填
                // dockerfileDir 自动使用 src/main/docker/dev
            ],
            "test": [
                name: "ingot/gateway-test"
            ],
            "prod": [
                name: "ingot/gateway",
                platform: "linux/amd64,linux/arm64"  // 覆盖全局配置
            ]
        ]
    }
}
```

### DSL 方式

```groovy
ingotAssemble {
    docker {
        registry "docker-registry.ingotcloud.top"
        username JY_DOCKER_REGISTRY_USERNAME
        password JY_DOCKER_REGISTRY_PASSWORD
        
        env("dev") {  // 新方法名
            name = "ingot/gateway-dev"  // 必填
        }
        
        env("prod") {
            name = "ingot/gateway"
            platform = "linux/amd64,linux/arm64"
        }
    }
}
```

## 🎯 优化效果

### 配置更清晰

| 方面 | 优化前 | 优化后 |
|------|--------|--------|
| 命名准确性 | ❌ tag（标签）含义模糊 | ✅ env（环境）语义明确 |
| 配置结构 | ❌ 默认配置 + tags 混合 | ✅ 全局配置 + envs 分离 |
| 必填项 | ⚠️ 可能忘记配置 name | ✅ 强制每个环境配置 name |
| 配置独立性 | ⚠️ 依赖默认配置 | ✅ 每个环境独立配置 |

### 使用更规范

**优化前的问题**：
1. ❌ `tag` 命名不够直观
2. ❌ 默认配置很少使用，但占用了顶级字段
3. ❌ 环境可能忘记配置镜像名称
4. ❌ `name` 和 `saveName` 在顶级会引起混淆

**优化后的改进**：
1. ✅ `env` 命名准确，语义清晰
2. ✅ 所有配置都在环境中，结构统一
3. ✅ 每个环境必须配置 name，避免遗漏
4. ✅ 配置层级清晰，不会混淆

## 📁 文件变更

### 源代码

| 文件 | 操作 | 说明 |
|------|------|------|
| `extension/Tag.groovy` | 重命名 | → `Env.groovy` |
| `extension/Env.groovy` | 更新 | 更新类名和注释 |
| `extension/DockerExtension.groovy` | 更新 | 移除 name/saveName，tags→envs |
| `AssemblePlugin.groovy` | 更新 | 更新导入和逻辑，强制环境配置 |

### 文档

| 文件 | 操作 | 说明 |
|------|------|------|
| `README.md` | 更新 | 更新所有 tag → env |
| `CHANGELOG.md` | 重写 | 新的更新日志和迁移指南 |
| `examples/basic-multi-env.gradle` | 新增 | 替代 basic-single-env.gradle |
| `examples/multi-env-dsl.gradle` | 更新 | 更新为 env 配置 |
| `examples/advanced-multi-registry.gradle` | 更新 | 更新为 env 配置 |
| `examples/basic-single-env.gradle` | 删除 | 不再需要单环境示例 |
| `examples/multi-env-map.gradle` | 删除 | 合并到 basic-multi-env.gradle |
| `examples/README.md` | 更新 | 更新示例说明 |

## 🔄 迁移步骤

### 步骤 1：更新字段名

```groovy
// 查找并替换
tags → envs
tag( → env(
```

### 步骤 2：移除默认配置

```groovy
// 删除这些配置
docker {
    name "..."  // ❌ 删除
    saveName "..."  // ❌ 删除
}
```

### 步骤 3：添加环境配置

```groovy
// 确保每个环境都有 name
envs = [
    "dev": [
        name: "ingot/app-dev"  // ✅ 必须添加
    ]
]
```

### 步骤 4：测试

```bash
# 查看任务是否正确生成
./gradlew tasks --group=ingot

# 测试构建
./gradlew dockerBuildDev
```

## ⚠️ 注意事项

### 不兼容变更

1. **必须手动迁移**：
   - 字段名 `tags` → `envs`
   - 方法名 `tag()` → `env()`
   - 移除默认 `name` 配置

2. **新的验证规则**：
   - 必须配置至少一个环境
   - 每个环境必须配置 `name`
   - 如果没有配置环境，构建时会显示警告

### 迁移建议

1. ✅ 建议在测试环境先迁移
2. ✅ 确保每个环境都配置了 `name`
3. ✅ 运行 `./gradlew tasks --group=ingot` 验证任务
4. ✅ 测试构建流程是否正常

## 📈 后续优化计划

- [ ] 支持环境继承关系（如 test 继承 dev 的配置）
- [ ] 支持环境变量插值
- [ ] 支持自定义任务命名模板
- [ ] 支持镜像标签版本策略
- [ ] 增加配置验证和错误提示

## 🎓 最佳实践

### 1. 环境命名建议

```groovy
envs = [
    "dev",      // ✅ 开发环境
    "test",     // ✅ 测试环境
    "staging",  // ✅ 预发布环境
    "prod"      // ✅ 生产环境
]
```

### 2. 镜像命名建议

```groovy
envs = [
    "dev": [
        name: "company/app-dev"     // 环境后缀
    ],
    "test": [
        name: "company/app-test"    // 环境后缀
    ],
    "prod": [
        name: "company/app"          // 无后缀（生产环境）
    ]
]
```

### 3. 目录结构建议

```
src/main/docker/
├── dev/
│   └── Dockerfile
├── test/
│   └── Dockerfile
└── prod/
    └── Dockerfile
```

**优势**：环境名称与目录名称一致，自动匹配，无需手动配置 `dockerfileDir`。

## 📚 相关文档

- [README.md](../ingot-assemble-plugin/README.md) - 完整使用文档
- [CHANGELOG.md](../ingot-assemble-plugin/CHANGELOG.md) - 详细变更日志
- [examples/](../ingot-assemble-plugin/examples/) - 配置示例

---

**总结**：通过这次优化，插件的配置更加清晰、规范、易用。虽然需要手动迁移，但带来的好处是配置更直观、更不容易出错。
