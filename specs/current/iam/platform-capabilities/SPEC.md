# 平台能力与页面归属

## 应用与正式入口

字典归平台治理的系统设置；安全中心独立为 `PLATFORM / platform:security`。平台治理包含组织与租户、应用与配置、平台管理和系统设置；组织安全与合规承载字段策略与组织授权审计。

| 页面 | 浏览器 URL | canonical viewPath |
|---|---|---|
| 字典 | `/platform/settings/dictionaries` | `platform.settings.dictionaries` |
| 凭证策略 | `/security/credential-policies` | `security.credential.policies` |
| 账号保护 | `/security/account-protection` | `security.account.protection` |
| 访问防护 | `/security/access-protection` | `security.access.protection` |
| 会话管理 | `/security/sessions` | `security.sessions` |
| 平台授权审计 | `/platform/authorization-audit` | `platform.iam.authorization.audit` |
| 组织授权审计 | `/org/authorization-audit` | `org.iam.authorization.audit` |
| 字段策略 | `/org/field-policies` | `org.iam.field.policies` |

正式 URL 显式配置，旧 URL 不作为功能入口。字段策略由 org 插件承载；两域审计包装页复用 admin-common 无路由组件，固定域并拒绝伪造域筛选。租户宿主不装配 security 插件。

## 安全管理面与 SDK 执行

安全资源完整键为 `PLATFORM / platform:security / resourceCode`。六资源十六操作仅支持 ALL，不开放字段权限或对象候选；操作码保留 `iam-platform:` 前缀。

| resourceCode | 精确操作后缀 | Security HTTP 前缀 |
|---|---|---|
| `credential-policy` | read / create / update / delete | `/platform/security/credential/policy-config` |
| `lockout-policy` | read / update | `/platform/security/account/lockout-policies` |
| `login-failure-policy` | read / update | `/platform/security/access/login-failure-policies` |
| `security-policy` | read / create / update / delete | `/platform/security/policy` |
| `session` | read / revoke | `/platform/security/sessions` |
| `session-policy` | read / update | `/platform/security/session/concurrency-policies` |

网关前缀为 `/api/security`。并发策略新增、更新及删除统一要求 session-policy:update。Security 通过 AuthorizationAccess 和 SecurityGlobalAccess 检查精确操作、资源键、平台身份和 ALL 目标范围。READ_ONLY 读取使用 SDK 最长 30 秒热窗口，MUTATING 写入必须 fresh；到期、停用、撤权和依赖故障按既有授权规则拒绝，不能以 JWT 角色、页面按钮或授权降级代替执行门禁。

访问防护和会话管理入口按各自两种 read 的 ANY 放行；只展示有读取资格的 Tab，默认首个可访问 Tab，仅请求当前激活且有权限的面板。会话客户端选择沿用独立的 `platform:develop:client:query`，页面权限不隐式增加目录读取资格。

## App 用户管理

`PLATFORM / platform:member` 是独立应用。根目录 `member.root` 使用 `layout.main`，对三个页面的读取权限按 ANY 判定。每个页面各自要求对应的操作。

| 页面 | 浏览器 URL | viewPath | 读取权限 |
|---|---|---|---|
| 用户管理 | `/member/users` | `member.user` | `platform:member:user:query` |
| 角色管理 | `/member/roles` | `member.role` | `platform:member:role:query` |
| 权限管理 | `/member/permissions` | `member.permission` | `platform:member:permission:query` |

Member 管理接口位于 `/api/member/v1/platform/member`。IAM 注册 `user`、`role`、`permission` 三个资源的 20 项精确操作，范围为 ALL。接口校验可信平台身份、资源键、精确操作和全局范围。读取遵循 SDK 热窗口，写入按请求实时求值。普通平台角色经 IAM 发布和分配获得操作权限，系统超级管理员适用现有动态授权。租户身份或无法取得有效授权的请求会被拒绝。

用户角色和角色权限分别使用独立接口回显，更新时全量替换。显式传空数组清除可维护的绑定。平台会话中的新增关联行沿用 Member 关系表的历史默认租户存储值，授权仍根据可信平台身份判定。

新库由 IAM 正式种子生成目录。已有库在选定的 IAM 库执行 036，支持预演和重复执行，冲突时回滚事务，保留已有角色版本和授权分配。回退通过禁用应用入口保留目录及授权引用。[036 执行说明](../../../../databases/iam/migrations/README.md#036-app-用户管理应用)

## 字典、审计与字段策略

字典 `/v1/platform/dictionaries` 使用 `iam-platform:dictionary:read/create/update/delete`；启停与排序为 update，内置约束保持。租户与应用选择分别要求 `iam-platform:tenant:read`、`iam-platform:application:read`。

平台与组织审计分别为 `/v1/platform/authorization/audits`、`/v1/tenant/authorization/audits`，使用 `iam-platform:audit:read`、`iam-tenant:audit:read`；查询限定可信当前域，筛选不能替代 domain/tenantId。公共审计组件仅接收固定域、精确 read 操作和数据适配器，不依赖插件。

组织字段策略入口使用 `iam-tenant:field-policy:read`，保存使用 `iam-tenant:field-policy:update`，策略按资源读取和保存。普通通讯录浏览、通讯录可见范围及其预览接口已退出。IAM 网关前缀为 `/api/iam`。详见 [租户资源字段策略](../tenant-field-policy/SPEC.md)。

## 内部授权身份恢复

三个 POST 内部端点 `/inner/authorization/snapshot`、`/inner/authorization/v2/evaluate`、`/inner/authorization/v2/preview` 使用优先于通用 INNER 的专用安全链，同时检查内部调用来源、转发 Bearer 签名及在线会话身份。请求体和内部账号头不能替代当前认证成员，不从 JWT 角色文本构造身份。

snapshot 返回实时改密状态，基础授权链不再次调用改密门禁，避免递归授权；evaluate/preview 继续执行本地求值的强制改密限制。缺失或无效 Token、会话撤销、请求身份不匹配及依赖故障拒绝。其他内部接口保持既有安全链。

## 目录升级与兼容

正式目录来自 `tools/iam/generate_database.py`，内置执行资源使用显式注册。已有库使用 `020_platform_capability_completion.sql` 幂等升级，可通过 `@iam_capability_dry_run=TRUE` 预演并在事务末尾回滚；脚本输出应用、资源、操作及页面清单，发布时由操作者显式执行。

升级按自然键预检，保留原资源、操作、页面 ID，以及角色版本、授权分配、委派上限和审计引用。路径、注册键、routeName 和父目录定向迁移，菜单人工图标、排序、显隐、缓存、props、route_params 和 access_mode 保留；访问防护、会话管理按各自读取资格的 ANY 放行。旧库的通讯录和字段策略目录按适用条件执行 `033`、`034`。跨应用自定义关联、路径或标识冲突导致整笔业务更新回滚，不关闭外键，不静默丢失人工配置。

普通角色不自动补权，继续通过既有发布与分配流程配置。发布顺序为备份停写、020 补丁、同批 IAM/Security/两管理台、授权及路由缓存失效、等待 SDK 热窗口并重新 bootstrap。回滚停止新版写入，按完整备份和输出 ID 恢复目录及关联并回退应用，不重建 IAM 库。
