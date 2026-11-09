# 平台能力与页面归属

## 应用与正式入口

字典归平台治理的系统设置。IAM 的五个内置应用为平台治理 `iam-platform`、组织治理 `iam-tenant`、开发者平台 `platform-developer`、安全中心 `platform-security` 和 App 用户管理 `platform-member`。操作按 `<app-code>:<resource-code>:<operation>` 命名，执行时按可信域、应用和资源完整键校验精确操作及适用范围。

| 页面 | 浏览器 URL | canonical viewPath |
|---|---|---|
| 字典 | `/platform/settings/dictionaries` | `platform.settings.dictionaries` |
| 凭证策略 | `/security/credential-policies` | `security.credential.policies` |
| 账号保护 | `/security/account-protection` | `security.account.protection` |
| 访问防护 | `/security/access-protection` | `security.access.protection` |
| 会话管理 | `/security/sessions` | `security.sessions` |
| 平台成员与用户组 | `/platform/iam/personnel` | `platform.iam.personnel` |
| 平台授权审计 | `/platform/authorization-audit` | `platform.iam.authorization.audit` |
| 组织授权审计 | `/org/authorization-audit` | `org.iam.authorization.audit` |
| 字段策略 | `/org/field-policies` | `org.iam.field.policies` |

正式 URL 显式配置，旧 URL 不作为功能入口。字段策略由 org 插件承载；两域审计包装页复用 admin-common 无路由组件，固定域并拒绝伪造域筛选。租户宿主不装配 security 插件。

## 安全管理面与 SDK 执行

安全资源完整键为 `PLATFORM / platform-security / resourceCode`。七资源二十操作仅支持 ALL，操作码使用 `platform-security:` 前缀。

| resourceCode | 精确操作后缀 | Security HTTP 前缀 |
|---|---|---|
| `credential-policy` | read / create / update / delete | `/platform/security/credential/policy-config` |
| `account-lockout-policy` | read / update | `/platform/security/account/lockout-policies` |
| `login-failure-policy` | read / update | `/platform/security/access/login-failure-policies` |
| `access-policy` | read / create / update / delete / refresh | `/platform/security/policy` |
| `block-event` | read | `/platform/security/policy/events` |
| `session` | read / revoke | `/platform/security/sessions` |
| `session-concurrency-policy` | read / create / update / delete | `/platform/security/session/concurrency-policies` |

网关前缀为 `/api/security`。并发策略新增、更新及删除分别校验对应操作。刷新访问策略需要读取和刷新两项资格。Security 通过 AuthorizationAccess 和 SecurityGlobalAccess 检查精确操作、资源键、平台身份和 ALL 目标范围。READ_ONLY 读取使用 SDK 最长 30 秒热窗口，MUTATING 写入必须 fresh。到期、停用、撤权和依赖故障按既有授权规则拒绝。

访问防护按访问防护策略、登录失败防护和封禁记录的读取权限取 ANY。会话管理按在线会话和并发策略的读取权限取 ANY。两个入口只展示有读取资格的 Tab，默认首个可访问 Tab，仅请求当前激活且有权限的面板。会话客户端选择需要 `platform-developer:oauth-client:read-list`。

## App 用户管理

`PLATFORM / platform-member` 是独立应用。根目录 `member.root` 使用 `layout.main`，对三个页面的读取权限按 ANY 判定。每个页面各自要求对应的操作。

| 页面 | 浏览器 URL | viewPath | 读取权限 |
|---|---|---|---|
| App 用户 | `/member/users` | `member.user` | `platform-member:user:read-list` |
| App 角色 | `/member/roles` | `member.role` | `platform-member:role:read` |
| App 权限 | `/member/permissions` | `member.permission` | `platform-member:permission:read` |

Member 管理接口位于 `/api/member/v1/platform/member`。IAM 注册 `user`、`role`、`permission` 三个资源的 25 项精确操作，范围为 ALL。接口校验可信平台身份、资源键、精确操作和全局范围。读取遵循 SDK 热窗口，写入按请求实时求值。普通平台角色经 IAM 发布和分配获得操作权限，系统超级管理员适用现有动态授权。App 用户启用、停用、锁定、解锁分别授权。App 角色及权限更新按提交字段检查 `update`、`change-status`，同时修改时需要两项资格。

用户角色和角色权限分别使用独立接口回显，更新时全量替换。显式传空数组清除可维护的绑定。平台会话中的新增关联行沿用 Member 关系表的历史默认租户存储值，授权仍根据可信平台身份判定。

新库由 IAM 正式种子生成目录。036 是旧 `platform:member` 目录版本的增量脚本，执行范围见[历史升级说明](../../../../databases/iam/migrations/README.md#036-app-用户管理应用)。

## 平台成员、用户组与权限诊断

平台成员与用户组菜单按 `iam-platform:member:read` 和 `iam-platform:group:read` 取 ANY，Tab 各自检查读取资格。组列表、总数、详情和写入按实际操作的组范围判断。组成员列表及计数再叠加成员读取范围，成员资料遵循字段策略。组预览和更新省略 `selection` 时保留原关联，显式提交时按完整替换校验，包含不可维护的既有成员会拒绝。

两域的角色与授权工作区提供独立的权限诊断 Tab，要求 `iam-platform:authorization-diagnosis:run` 或 `iam-tenant:authorization-diagnosis:run`。默认诊断当前可信成员，诊断他人需成员读取及目标范围。业务对象候选和提交按资源完整键、目标读取及对象范围校验。诊断只读，展示内容遵循现有披露权限。

## 平台创建流程的账号候选

`GET /v1/platform/accounts` 可选 `excludePlatformMembers`，默认 `false`。添加平台成员时使用 `true`，同时要求全局账号读取、精确查找及平台成员创建资格。候选和总数按读取与查找范围的交集计算，排除已有平台成员记录的账号，包括暂停及已移出。全局账号列表和创建组织选择所有者继续使用普通分页，已有平台成员资格的账号仍可作为组织所有者。

选择时通过 `POST /v1/platform/accounts/lookup` 按完整登录名精确核对用途。添加平台成员指定平台域，在查找和创建时复核成员资格。启用带过滤的前端前先部署支持该参数的 IAM 服务。

## 业务图片上传

IAM 与 Member 分别使用 `POST /api/iam/v1/assets/uploads` 与 `POST /api/member/v1/assets/uploads`。表单按固定用途提交文件和场景所需目标，服务端校验身份、操作、范围及适用字段权限。上传返回短时一次性 `uploadId`，业务表单通过 `avatarUploadId` 或 `iconUploadId` 提交并重验资格。业务写入与消费凭证在同一事务，未绑定对象过期清理。库内只保存 `bucket/objectName`，响应按对象字段契约签发临时链接。

## 字典、审计与字段策略

字典 `/v1/platform/dictionaries` 使用 `iam-platform:dictionary:read/create/update/delete`；启停与排序为 update，内置约束保持。租户与应用选择分别要求 `iam-platform:tenant:read`、`iam-platform:application:read`。

平台与组织审计分别为 `/v1/platform/authorization/audits`、`/v1/tenant/authorization/audits`，使用 `iam-platform:authorization-audit:read`、`iam-tenant:authorization-audit:read`。服务端从可信身份确定查询域和租户。公共组件接收固定域的数据适配器。

两端按 `(occurred_at, id)` 倒序使用 `limit` 和不透明 `cursor` 分页，`limit` 默认 20，范围 1–100。返回 `items`、`hasMore`、适用的 `nextCursor` 和页级 `references`。筛选与游标条件在同一查询中生效，时间为起点包含、终点不包含。引用区分可见、无权查看、已删除，名称受当前读取、对象范围和适用字段权限约束。历史差异值保持兼容读取。

新库包含审计游标索引。已有库在前后端同批发布前按需执行 032，回退时前后端使用配对版本。数据库索引与迁移见[持久化基线](../database-persistence/SPEC.md)。

组织字段策略入口使用 `iam-tenant:field-policy:read`，保存使用 `iam-tenant:field-policy:update`，策略按资源读取和保存。普通通讯录浏览、通讯录可见范围及其预览接口已退出。IAM 网关前缀为 `/api/iam`。详见 [租户资源字段策略](../tenant-field-policy/SPEC.md)。

## 内部授权身份恢复

三个 POST 内部端点 `/inner/authorization/snapshot`、`/inner/authorization/v2/evaluate`、`/inner/authorization/v2/preview` 使用优先于通用 INNER 的专用安全链，同时检查内部调用来源、转发 Bearer 签名及在线会话身份。请求体和内部账号头不能替代当前认证成员，不从 JWT 角色文本构造身份。

snapshot 返回实时改密状态，基础授权链不再次调用改密门禁，避免递归授权；evaluate/preview 继续执行本地求值的强制改密限制。缺失或无效 Token、会话撤销、请求身份不匹配及依赖故障拒绝。其他内部接口保持既有安全链。

## 目录升级与兼容

正式目录由 `tools/iam/generate_database.py` 和 006 种子生成，包含 5 个应用、44 个资源、181 项启用操作及 41 个菜单节点。既有正式页面 URL、路由名和视图键延续，运行时按新操作码校验。

目录切换使用新版初始化和重建授权的配对环境，普通角色按新目录显式授予所需操作。切换后重新登录并 bootstrap。回退使用旧代码与旧库配对。完整初始化固定重建 `ingot_iam`，直接写入目录、默认平台管理员及治理分配。平台账号首次登录强制改密，租户数据由管理员创建。历史 020、033、034、036 按各自的目标版本留存。部署入口见[数据库脚本索引](../../../../databases/README.md)。
