# 应用中心化授权 SPEC

> 记录当前已上线并验收的有效事实。旧模型物理列（`menu_id`/`permission_id`/`org_type`/`enable_permission`）已随破坏性清理删除，应用中心化模型为唯一事实来源。

## 1. 领域模型

```text
应用 (platform_app)
├── 菜单树 (platform_menu)
└── 权限树 (platform_permission)
```

### 1.1 应用 `platform_app`

| 字段 | 说明 |
|---|---|
| `id` | 应用 ID |
| `code` | 稳定且唯一的应用编码，同时作为权限命名空间 |
| `name` | 应用名称 |
| `app_type` | 应用类型，使用 `OrgTypeEnum`（`0` 平台 / `1` 租户） |
| `icon` / `intro` / `sort` / `status` | 图标 / 说明 / 全局排序 / 状态 |

约束：

- `code` 创建后不可通过普通更新接口修改。
- 平台应用仅用于平台后台；租户应用可授权给租户。
- 一个应用可拥有多个根菜单；应用本身不作为菜单节点返回。
- 应用类型统一由 `OrgTypeEnum` 表示（旧 `AppTypeEnum` 已删除）。
- 应用根引用按 `app_id` 维度识别（旧 `menu_id`/`permission_id` 列已删除）。

### 1.2 菜单 `platform_menu`

| 字段 | 说明 |
|---|---|
| `app_id` | 所属应用 |
| `pid` | 父菜单 ID |
| `access_mode` | 访问模式，`AccessModeEnum`：`0` 开放(OPEN) / `1` 需权限(PERMISSION)，菜单可见性的唯一来源 |
| `permission_id` | 菜单托管 `NAVIGATION` 权限 ID |

约束：

- 根菜单 `app_id` 由应用上下文填充，子菜单继承父菜单 `app_id`，不能跨应用移动。
- 菜单所属平台端/租户端由应用 `app_type` 派生。
- `OPEN` 只跳过角色权限检查，不跳过应用状态与租户应用授权检查。
- `access_mode` 为菜单访问控制唯一来源（旧 `enable_permission` 列已删除）。
- 平台端/租户端由应用 `app_type` 派生（旧 `org_type` 列已删除）。

### 1.3 权限 `platform_permission`

| 字段 | 说明 |
|---|---|
| `app_id` | 所属应用 |
| `pid` | 父权限 ID |
| `code` | 权限编码（应用命名空间内） |
| `node_type` | `GROUP` / `NAVIGATION` / `ACTION` |
| `source_type` | `SYSTEM` / `MENU` / `MANUAL` |
| `source_id` | 来源资源（如菜单）ID |
| `managed` | 是否系统托管 |
| `status` | 权限状态 |

约束：

- 应用创建时自动创建 `{code}:**` 根权限（Ant 子树通配），并绑定组织管理员。
- `GROUP` 必须以 `:*` 结尾；`NAVIGATION` / `ACTION` 必须为精确编码，不得以 `:*` 结尾。
- 子权限编码必须位于父权限命名空间中。
- `MENU` 来源权限由菜单模块托管，权限管理中只读。
- 前端按钮与后端 API 使用同一个 `ACTION` 权限。
- 平台/租户隔离由应用 `app_type` 派生（旧 `org_type` 列已删除）。

## 2. 菜单权限生命周期

每个菜单始终关联一条托管 `NAVIGATION` 权限。

| 菜单操作 | 权限行为 |
|---|---|
| 创建菜单 | 同事务创建托管权限；目录类型(`Directory`)权限码追加 `:**`；根菜单未传 `path` 默认 `/{appCode}` |
| 修改名称/父级 | 同步权限名称/父级 |
| 修改路由 | 不自动修改权限编码 |
| 切换 `access_mode` | 不删除权限与角色绑定 |
| 删除菜单 | 叶子菜单且托管权限无子权限时删除托管权限 |

**菜单即应用**：当根级菜单的 path 转换授权码与应用 `code` 一致时，直接复用应用根权限 ID，不新建权限；删除此类菜单仅删除菜单行，保留应用根权限及其角色绑定（由删除应用统一管理）。

## 3. 权限匹配语义

由 `PermissionMatcher` 统一实现：

- 精确码只匹配自身。
- 单段通配 `{ns}:*` 匹配命名空间 `{ns}:` 前缀下的全部权限。
- Ant 子树通配 `{ns}:**` 匹配命名空间 `{ns}` 前缀下的全部权限。
- 通配只匹配不含 `*` 的具体权限码；不支持中间通配与隐式父级包含。

通配授权是动态授权：应用新增权限后，已持有对应通配码的角色自动获得新权限，相关操作需审计。

## 4. 角色与租户授权

### 4.1 租户应用授权 `tenant_app_config`

| 字段 | 说明 |
|---|---|
| `tenant_id` / `app_id` | 租户 / 租户应用 |
| `enabled` | 是否启用 |
| `source` | 套餐 / 人工 / 系统初始化 |
| `valid_from` / `valid_until` | 生效 / 失效时间 |

未关联应用默认不可用；仅租户类型应用可授权。

### 4.2 角色来源

- `platform_role` 布尔字段为角色来源**唯一表示**（`RoleType.getPlatformRole()` 契约）；冗余的 `role_source`/`RoleSourceEnum` 已删除。
- 平台预设角色共享使用，不复制到每个租户。
- 有效权限 = `platform_role_permission` ∪ `tenant_role_permission_private`；租户只能追加/撤销自己追加的权限，不能修改平台默认权限。
- 租户自定义角色（`tenant_role_private`）权限不能超过租户已授权应用范围。
- 租户管理员不存全部叶子权限，动态拥有当前租户全部有效应用的根权限。

## 5. 有效权限与菜单生成

授权解析唯一入口为 `ApplicationAuthorizationResolver`（已无 legacy/shadow 双轨与 `authorization.model` 开关）。

有效权限计算：

```text
用户角色权限并集
→ 合并平台预设权限与租户追加权限
→ 应用显式通配/精确匹配
→ 过滤禁用权限 / 禁用应用 / 未授权或过期租户应用
```

菜单排序：根菜单 `app.sort → menu.sort → menu.id`；同父子菜单 `menu.sort → menu.id`。构建时按应用过滤有效菜单、按 `access_mode` 与有效权限过滤、补齐可见叶子的祖先目录、分应用递归排序后按应用顺序拼接。

## 6. 接口

基址 `/v1/platform/config/apps`：

| 用途 | 接口 |
|---|---|
| 应用分页 / 详情 | `GET /page`、`GET /{appId}` |
| 应用创建 / 更新 / 启停 / 删除 | `POST /`、`PUT /{appId}`、`PATCH /{appId}/status`、`DELETE /{appId}?force=` |
| 应用菜单 | `GET /{appId}/menus/tree`、`POST/PUT/DELETE /{appId}/menus[/{menuId}]` |
| 应用权限 | `GET /{appId}/permissions/tree`、`POST/PUT/DELETE /{appId}/permissions[/{permissionId}]` |

只读全量树（前端展示）：`GET /v1/platform/config/menu/tree`、`GET /v1/platform/config/permission/tree`。

只读审计：`GET /v1/platform/dev/authorization/audit`。

删除应用语义：

- 普通删除（`force=false`）：存在菜单、子权限（除根权限）、租户授权或非组织管理员角色绑定时拒绝；删除前自动解除组织管理员对根权限的默认绑定。
- 强制删除（`force=true`）：**仅超级管理员**（`ROLE_ADMIN_CODE`，服务层校验）可用，级联清除应用全部菜单、权限及平台角色绑定；存在租户授权（`tenant_app_config`）时**仍拒绝**。

兼容：旧 `PlatformAppAPI`（`/v1/platform/base/app`）保留并在 OpenAPI 标记 `deprecated`；旧 `/base` 菜单/权限写接口已移除。

## 7. 缓存与失效

菜单与有效权限缓存键包含租户与用户上下文。应用资源、租户应用授权、角色授权、用户角色变化均触发对应维度失效，写操作在事务提交后失效。

## 8. 权限码

| 操作 | 权限码 |
|---|---|
| 应用查询/创建/更新启停/删除 | `platform:config:app:query|create|update|delete` |
| 应用菜单 | `platform:config:app:menu:query|create|update|delete` |
| 应用权限 | `platform:config:app:permission:query|create|update|delete` |
