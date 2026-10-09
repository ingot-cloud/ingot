# 公共访问控制模块命名

> 状态：validating（命名调整与限定自动化验证完成，主 change 验收保持原状态）
> 批准：2026-10-09，用户要求选择更明确的公共模块名称并修改，授权本轮命名调整。
> 本增量属于已批准的 IAM active change，主 change 保持 implementing。

## 需求与范围

公共模块承载业务操作准入、资源注册、对象及数据范围、字段权限执行和跨服务授权客户端。模块名称应明确表达业务访问控制职责，便于与 OAuth2/OIDC 授权服务器区分。

本轮仅调整模块目录、Gradle 项目名及依赖别名，同步接入说明和可复核命令。此前讨论的字段注解、脱敏规则、可编辑/可筛选公共处理及缓存预热仍需单独完成设计与实施，不在本次命名调整中实现。

## 设计与兼容

| 项目 | 原值 | 新值 |
|---|---|---|
| 模块目录与产物名称 | `ingot-authorization` | `ingot-access-control` |
| Gradle 项目路径 | `:ingot-framework:ingot-authorization` | `:ingot-framework:ingot-access-control` |
| 依赖别名 | `ingot.framework_authorization` | `ingot.framework_access_control` |

- `ingot-access-control` 表达公共业务访问控制职责；OAuth2/OIDC 协议实现继续位于 `ingot-security-authorization-server`。
- Java 包 `com.ingot.framework.authorization`、类型名称、自动装配类名及配置键保持原有契约；本轮不改变授权判断、缓存策略、HTTP 接口或数据库。
- 仓库内 IAM provider 和 iam-ops 样板统一引用新路径，不保留旧 Gradle 项目别名或空壳模块。框架尚未正式上线；外部构建若使用旧模块坐标，需随本轮更新为新名称。
- 已记录的测试数量及历史结论保留；接入说明和复核命令使用新路径。同步前端 active change 中对应的后端来源副本，只更新模块引用。

## 任务与验收

- [x] ACN01：确认模块职责、名称与兼容范围，记录本轮用户授权。
- [x] ACN02：移动完整模块，更新构建引用、公共模块说明及相关接入文档，确认没有失效的旧构建引用。
- [x] ACN03：通过公共模块现有测试、IAM provider 与 iam-ops 编译，以及变更格式检查；记录验证证据。

验证命令：

```sh
./gradlew :ingot-framework:ingot-access-control:test :ingot-service:ingot-iam:ingot-iam-provider:compileJava :examples:iam-ops:compileJava --offline
./gradlew :ingot-framework:ingot-access-control:jar --offline --console=plain
git diff --check
```

## 实施与验证证据

2026-10-09：ACN01–ACN03 完成，25 个原有受版本控制的模块文件完整移动且内容与原路径 HEAD 一致；新增模块 README 说明职责与接入方式。仓库内构建、样板、资源接入规范及相关 active 文档同步新名称，前端仓库仅同步 7 份文档引用。

公共模块 17 项测试通过（RemoteAuthorizationTest 6、ResourceAuthorizationTest 6、RoleFieldAuthorizationTest 5），没有失败、错误或跳过。IAM provider 与 iam-ops 编译通过；新模块 jar 构建通过，产物名称为 `ingot-access-control-*.jar`，其中 Java 类及 Spring 自动装配入口保持原契约。两仓库 `git diff --check` 通过，旧模块名称仅保留在本文件的新旧映射说明中。

本轮没有修改授权行为、数据库、运行时配置或前端代码，也没有部署。2026-10-09，用户随后明确授权提交本次模块命名和文档同步改动。主 change 的既有产品验收项和 current 基线保持原有状态。
