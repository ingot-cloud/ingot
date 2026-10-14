# 接口与权限矩阵

业务 HTTP URL、方法、请求和响应保持原契约；导航 URL、viewPath、routeName 和应用关联按 DESIGN 切换。bootstrap 沿用已有字段，applicationId 使用安全中心实际 ID。

安全资源的完整键均为 `PLATFORM / platform:security / resourceCode`，只支持 `ALL`，不支持字段权限或对象候选。下列动作保留 `iam-platform:` 前缀；READ_ONLY 读取遵守 SDK 最长30秒热窗口，MUTATING 写入必须 fresh 求值，身份/应用/操作停用、授权到期和依赖故障拒绝。

| resourceCode | 精确操作后缀 | Security HTTP 前缀（网关 /api/security） | 页面读取入口 |
|---|---|---|---|
| credential-policy | read / create / update / delete | /platform/security/credential/policy-config | 凭证策略 |
| lockout-policy | read / update | /platform/security/account/lockout-policies | 账号保护 |
| login-failure-policy | read / update | /platform/security/access/login-failure-policies | 访问防护：登录失败防护 |
| security-policy | read / create / update / delete | /platform/security/policy | 访问防护：网关防护各 Tab |
| session | read / revoke | /platform/security/sessions | 会话管理：在线会话 |
| session-policy | read / update | /platform/security/session/concurrency-policies | 会话管理：并发策略（新增、更新、删除均为 update） |

共六资源十六操作。访问防护、会话管理的菜单按两类 read 的 ANY 放行；仅展示具有对应 read 的 Tab，默认首个可访问 Tab，停用或未激活的 Tab 不请求。

| 能力 | HTTP 路径（网关 /api/iam） | 精确操作 |
|---|---|---|
| 字典管理 | /v1/platform/dictionaries | iam-platform:dictionary:read / create / update / delete；启停及排序为 update |
| 平台授权审计 | /v1/platform/authorization/audits | iam-platform:audit:read |
| 组织授权审计 | /v1/tenant/authorization/audits | iam-tenant:audit:read |
| 通讯录可见范围 | /v1/tenant/policies/directory | iam-tenant:directory-policy:read / update |
| 字段权限 | /v1/tenant/policies/fields | iam-tenant:field-policy:read / update |
| 成员访问策略预览 | /v1/tenant/policies/preview | iam-tenant:policy:preview |

审计适配器固定域且丢弃伪造 domain/tenantId 筛选；公共组件通过域、read 操作和查询回调接入，不引用插件。成员策略两个读取操作独立控制 Tab，保存及预览分别判断自身精确操作。

字典选择租户、应用需 iam-platform:tenant:read、iam-platform:application:read；会话客户端选择沿用 platform:develop:client:query。页面权限不隐式追加目录读取权限。
