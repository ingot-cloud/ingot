# 公共业务访问控制

`ingot-access-control` 为业务服务提供操作准入、资源注册、对象及数据范围校验、字段权限处理，以及 IAM 跨服务授权客户端。OAuth2/OIDC 协议及令牌签发由 `ingot-security-authorization-server` 承载。

Gradle 接入使用 `implementation project(ingot.framework_access_control)`；模块路径为 `:ingot-framework:ingot-access-control`。Java 类型继续使用 `com.ingot.framework.authorization` 包，原自动装配和配置键保持。

业务服务通过 `AuthorizationAccess` / `@RequireIamAction` 检查精确操作准入，通过 `ScopeSql` 与对象校验执行数据范围，通过 `FieldPolicyProcessor` 执行字段投影及读写检查。当前注解只检查操作准入，范围与字段处理需要业务接口显式调用。

完整接入样板见 [iam-ops](../../examples/iam-ops/README.md)；当前执行契约见 [资源接入规范](../../.agents/skills/in-framework/references/iam-resource-extension.md)。
