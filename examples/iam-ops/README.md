# 独立平台应用接入示例

此模块是可编译的 Spring 服务样板，未部署、未写入现有业务数据库或菜单。业务库只需自己的 `schema.sql`；不复制 IAM 的角色、分配和字段策略表。

## 接入步骤

1. 在应用目录创建 PLATFORM 应用 `iam-ops`、资源 `incident`、三个精确操作 `iam-ops:incident:read/update/export`（每项是完整编码，目录关联决定归属）。配置资源能力 ALL/SELF/OBJECT_SET；字段 title/contact 能力与 IncidentProvider 一致。
2. 业务服务依赖 `ingot-authorization`、`ingot-iam-api`、框架 Feign、安全与标准 Web 启动配置。注册 `IncidentProvider` Bean。启用 `RemoteIamAuthorizationService` Feign，并沿用项目线上认证恢复、Feign 原始身份头转发、服务发现和异常转换；不能匿名启用示例接口。
3. IAM 端以受控配置白名单注册完整 descriptor 和服务发现名 `iam-ops`，业务端配置同一专用密钥（环境变量/密钥系统提供，不提交值）。模板见 [resource-registration.example.yml](resource-registration.example.yml)。不得由请求提交 URL 或服务名。
4. 创建平台角色并显式分配固定版本；指定对象使用业务 UUID。角色字段权限步骤选择 incident 的 title/contact 可见性和可编辑性；字段随该固定版本和其操作范围继承，不另设查看者覆盖。
5. 编译：`./gradlew :examples:iam-ops:compileJava`。启动前由业务部署环境补齐数据源、服务发现、安全和 Redis/失效总线配置；本模块不提供绕过登录的演示模式。

## 执行方式

`IncidentAPI` 的列表/count/导出使用同一 ScopeSql；详情用真实 UUID/归属构造 ScopeTarget。写入先锁业务实体，fresh 求值，再检查目标与显式提交字段（包括 null）；仅投影登记字段，联系方式默认后端脱敏，不输出实体。执行时使用 FieldPolicyProcessor.forAction(decision, 精确操作)，不能直接读取兼容顶层 fields。搜索原值先检查整份操作策略，不能用当前页样本放行。空范围查询为空、未知操作拒绝、IAM 或对象 RPC 故障拒绝并返回 503。

新增创建操作时，生成业务 ID 并验证新对象归属后写入；转移归属同时验证旧目标和新目标。实际项目需要补业务校验、审计、限流、稳定排序、导出任务和真实脱敏器，不能把该样板直接当作完整工单产品。平台不配置部门；租户资源声明部门能力时必须提供可信 tenant 列及部门关系查询。本轮角色字段模型仅面向平台，租户继续原语义。

`HasAnyAuthority` 仍用于旧安全框架的 authority 门禁。`IamAccess` 是 IAM 服务内门面，包含在线身份、完整治理/委派准入和发号等 IAM 能力。新业务服务使用 `AuthorizationAccess` / `@RequireIamAction`；注解只证明精确操作准入，SQL/对象范围和字段读写仍必须显式执行，不会由注解自动完成。

验收清单见 [资源扩展验收](../../specs/changes/active/20260912-iam-identity-access-management/RESOURCE-EXTENSION-VERIFICATION.md)。
