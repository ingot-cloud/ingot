# 应用中心化授权 SPEC

> 记录当前已上线并验收的有效事实。权限编码与菜单路由解耦；菜单不再托管权限；应用对租户默认开放。

## 1. 领域模型

```text
应用 (platform_app)
├── 菜单树 (platform_menu) + 可见性关联 (platform_menu_permission)
├── 权限树 (platform_permission)   # 仅 GROUP / ACTION
└── 资源目录 (platform_resource)   # 见 data-authorization
```

### 1.1 应用 `platform_app`

| 字段 | 说明 |
|---|---|
| `id` | 应用 ID |
| `code` | 稳定且唯一的应用编码，同时作为权限命名空间 |
| `name` | 应用名称 |
| `app_type` | 应用类型，使用 `OrgTypeEnum`（`0` 平台 / `1` 租户） |
| `icon` / `intro` / `sort` / `status` | 图标 / 说明 / 全局排序 / 状态 |
| `default_access_mode` | 无租户覆盖时的默认策略，`AppDefaultAccessModeEnum`：`0` OPEN / `1` CLOSED；缺省 OPEN |
| `permission_id` | 应用根 GROUP 权限 ID（`{code}:**`） |
| `menu_id` | 历史列，当前不作为授权或导航事实 |

约束：

- `code` 创建后不可通过普通更新接口修改。
- 平台应用仅用于平台后台；租户应用可授权给租户。
- 一个应用可拥有多个根菜单；应用本身不作为菜单节点返回。
- 应用类型统一由 `OrgTypeEnum` 表示。
- 应用根权限只认 `permission_id`，不再用菜单托管权限识别。
- 全局 `status` 停用优先于默认策略与租户覆盖。

### 1.2 菜单 `platform_menu`

| 字段 | 说明 |
|---|---|
| `app_id` | 所属应用 |
| `pid` | 父菜单 ID |
| `path` | 浏览器 URL；**不**用于生成或匹配权限码 |
| `view_path` | 前端页面或布局注册键，与 `path` 独立；创建与更新原样落库，不由 path 推导 |
| `route_name` | 导航标识，不参与授权匹配 |
| `access_mode` | 访问模式，`AccessModeEnum`：`0` 开放(OPEN) / `1` 需权限(PERMISSION) |
| `permission_match_mode` | 受保护页匹配，`PermissionMatchModeEnum`：`0` ANY（默认）/ `1` ALL |

可见性关联表 `platform_menu_permission`：`(menu_id, permission_id)` 唯一；关联同应用、启用、非 GROUP 的 ACTION。可见性条件不是页面全部操作列表。

约束：

- 根菜单 `app_id` 由应用上下文填充，子菜单继承父菜单 `app_id`，不能跨应用移动。
- 菜单所属平台端/租户端由应用 `app_type` 派生。
- `OPEN` 只跳过角色权限检查，不跳过应用状态与租户应用授权检查。
- 受保护页面（`accessMode=PERMISSION` 且 `menuType=Menu`）必须关联至少一个 ACTION；Directory / OPEN 不得关联。
- 用户菜单与配置菜单树均不返回 `menuType=Button`（枚举值 `9` 仍保留，存量按钮行已迁为独立 ACTION）。
- 已无 `platform_menu.permission_id` 单关联列，也无 `custom_view_path`。
- 创建 Directory / Menu 且 `linkType=Default` 时 `viewPath` 必填；更新仅在请求显式传入 `viewPath` 时覆盖。
- 后端不解析、不校验注册键格式；修改 `path` 不改权限码、不重算 `view_path`。

### 1.3 权限 `platform_permission`

| 字段 | 说明 |
|---|---|
| `app_id` | 所属应用 |
| `pid` | 父权限 ID |
| `code` | 权限编码（应用命名空间内），创建后普通更新不可改 |
| `node_type` | 仅 `GROUP`（`"0"`）/ `ACTION`（`"2"`）；无 `"1"`，已无 `NAVIGATION` |
| `resource_id` | 可选；数据操作应绑定资源，纯配置权限可空 |
| `status` | 权限状态 |

已删除列：`type`、`managed`、`source_type`、`source_id`。树节点不再返回 `type` / `managed` / `readOnly`。

约束：

- 应用创建时自动创建 `{code}:**` 根 GROUP，并写入 `platform_app.permission_id`，绑定组织管理员。
- `GROUP` 必须以 `:**` 结尾；`ACTION` 必须为精确编码，不得以 `:*` / `:**` 结尾。创建接口不再接受 `:*`。
- 子权限编码必须位于父权限命名空间中；权限树移动不得静默变更命名空间。
- 前端按钮与后端 API 使用同一个 ACTION。操作权限不隐含 read；通配功能不自动给新资源配置数据范围。
- 删除权限时若仍被菜单、角色或数据规则引用则拒绝；删除菜单不级联删除权限。

## 2. 菜单与权限生命周期

菜单与权限分开管理。快捷新建权限是独立的权限创建请求，成功后再选中保存菜单；两个请求不伪装成跨请求事务。菜单保存失败或用户取消时保留已创建权限。

| 操作 | 行为 |
|---|---|
| 创建菜单 | 不创建权限。受保护页校验并写入 `platform_menu_permission` |
| 更新菜单 path / 层级 / 名称 | 不改权限码或角色绑定 |
| 切换 `access_mode` | 不删除权限与角色绑定；受保护页仍须满足关联约束 |
| 删除菜单 | 只删菜单及其可见性关联 |
| 创建权限 | 仅 GROUP/ACTION；可选 `resourceId`；创建后不可改 code / 跨应用 / 改 resourceId |
| 删除权限 | 仍被引用则拒绝 |

**菜单即应用**：当需要应用根 GROUP 时复用 `platform_app.permission_id`，不因删除菜单而删除应用根权限。

## 3. 权限匹配语义

由 `PermissionMatcher` 统一实现：

- 精确码只匹配自身。
- 单段通配 `{ns}:*` 匹配命名空间 `{ns}:` 前缀下的全部不含 `*` 的具体权限码。
- Ant 子树通配 `{ns}:**` 匹配命名空间 `{ns}` 前缀下的全部不含 `*` 的具体权限码。
- 不支持中间通配与隐式父级包含。
- 创建只允许 `:**`。匹配器仍识别遗留 `:*`，避免存量编码误判。
- 委派：`covers` 要求授予未来通配时自身必须持有覆盖该命名空间的通配；仅有当前叶子全集不能推导通配资格。

通配授权是动态授权：应用新增启用的具体权限后，已持有对应通配码的角色自动获得该能力。停用权限与不可用应用不在有效集合中。

## 4. 角色与租户授权

### 4.1 租户应用授权 `tenant_app_config`

| 字段 | 说明 |
|---|---|
| `tenant_id` / `app_id` | 唯一租户/应用关系 |
| `enabled` | 是否启用 |
| `source` | 套餐 / 人工 / 系统初始化 |
| `valid_from` / `valid_until` | 生效 / 失效时间 |

判定（`BizAppServiceImpl`）：

1. 应用全局停用 → 不可用。
2. 无覆盖记录 → 应用启用且 `default_access_mode != CLOSED` 则可用（默认开放）。
3. 有覆盖且 `enabled=false` → 始终禁止。
4. 有覆盖且 `enabled=true` → 当前时间不早于 `valid_from`、不晚于 `valid_until`（`now.isAfter(validUntil)` 才视为到期），且应用仍启用。未生效或到期**不**回退默认开放。

仅租户类型应用进入租户可用列表。默认租户（平台运营）按启用应用全量处理，与租户应用授权分域。

### 4.2 角色来源

- `platform_role` 布尔字段为角色来源**唯一表示**（`RoleType.getPlatformRole()` 契约）。
- 平台预设角色共享使用，不复制到每个租户。
- 有效权限 = `platform_role_permission` ∪ `tenant_role_permission_private`；租户只能追加/撤销自己追加的权限，不能修改平台默认权限。
- 租户自定义角色权限不能超过租户已授权应用范围。
- 租户管理员（`ROLE_ORG_ADMIN`）动态获得当前租户全部有效租户应用的根通配 `{appCode}:**`，不能穿透平台运营域。
- 角色表已无 `scope_type` / `scopes`；数据范围只存在于规则表，见 [data-authorization](../data-authorization/SPEC.md)。
- 部门角色绑定见 data-authorization；`filter_dept` 仍保留在角色上。

## 5. 有效权限与菜单生成

功能授权解析核心为 `EffectiveAuthorizationService`。菜单入口 `ApplicationAuthorizationResolver` 只消费该结果构建树。登录 `IdentityUtil.getScopes` 只写入**角色编码**（带租户），不写入业务权限码。

有效权限计算：

```text
用户实际角色绑定（保留部门）
→ 合并平台预设权限与租户追加权限
→ 租户管理员追加有效租户应用根通配
→ 精确/通配匹配启用具体权限
→ 过滤禁用权限 / 禁用应用 / 按 4.1 不可用的租户应用
```

不使用调用方传入角色码代替成员关系。临时登录 scope（如强制改密）不作为业务 RBAC 放行依据。

菜单可见性：

- 过滤 Button、禁用菜单、不可访问应用。
- OPEN 页在应用可用时可见。
- 受保护页按关联 ACTION 的 ANY/ALL 判定。
- 目录只在所属应用有效且存在可见子页面时返回；禁用祖先隐藏其分支，不补回禁用目录。
- 排序：根菜单 `app.sort → menu.sort → menu.id`；同父子菜单 `menu.sort → menu.id`。

## 6. 接口

基址 `/v1/platform/config/apps`：

| 用途 | 接口 |
|---|---|
| 应用分页 / 详情 | `GET /page`、`GET /{appId}`（含 `defaultAccessMode`；详情另有 `rootPermissionId` / `rootPermissionCode`） |
| 应用创建 / 更新 / 启停 / 删除 | `POST /`、`PUT /{appId}`、`PATCH /{appId}/status`、`DELETE /{appId}?force=` |
| 应用菜单 | `GET /{appId}/menus/tree`、`POST/PUT/DELETE /{appId}/menus[/{menuId}]`（节点 `permissionIds` / `permissionMatchMode`，不含 Button） |
| 应用权限 | `GET /{appId}/permissions/tree`、`POST/PUT/DELETE /{appId}/permissions[/{permissionId}]`（`nodeType`、`resourceId`） |
| 应用资源 | `GET/POST /{appId}/resources`、`PUT/DELETE /{appId}/resources/{resourceId}`，见 data-authorization |

只读全量树：`GET /v1/platform/config/menu/tree`、`GET /v1/platform/config/permission/tree`。

当前用户（登录态，禁止 query 指定其它用户）：

| 接口 | 说明 |
|---|---|
| `GET /v1/auth/user/info` | 资料、角色编码、可访问租户；**无**业务 `authorities` |
| `GET /v1/auth/user/menus` | 可见导航树，同源有效授权 |
| `GET /v1/auth/user/permissions` | `UserEffectivePermissionVO`：已展开具体权限码、`version`、`generatedAt`、`expiresAt` |

租户应用授权保持 `GET /v1/platform/org/tenant/apps`、`PUT /v1/platform/org/tenant/app/status`。

删除应用语义：

- 普通删除（`force=false`）：存在菜单、子权限（除根权限）、租户授权或非组织管理员角色绑定时拒绝；删除前自动解除组织管理员对根权限的默认绑定。
- 强制删除（`force=true`）：**仅超级管理员**可用，级联清除应用全部菜单、权限、资源及平台角色绑定；存在租户授权时**仍拒绝**。

兼容：旧 `PlatformAppAPI`（`/v1/platform/base/app`）保留并在 OpenAPI 标记 `deprecated`；旧 `/base` 菜单/权限写接口已移除。

## 7. 权限码

| 操作 | 权限码 |
|---|---|
| 应用查询/创建/更新启停/删除 | `platform:config:app:query\|create\|update\|delete` |
| 应用菜单 | `platform:config:app:menu:query\|create\|update\|delete` |
| 应用权限 | `platform:config:app:permission:query\|create\|update\|delete` |
| 应用资源 | `platform:config:app:resource:query\|create\|update\|delete` |
