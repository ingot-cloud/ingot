# 前端联调接口说明（RBAC / 菜单 / 数据权限）

网关对外前缀为 **`/pms`**（StripPrefix=1），例如 `GET /pms/v1/auth/user/permissions`。  
下文表格路径为 **PMS 服务内路径**（直连默认端口 `5200`）。统一响应：

```json
{ "code": "S0200", "message": "...", "data": {} }
```

成功码为 `S0200`。下文 HTTP 状态码指响应行；业务 `code` 见错误码一节。

本次 change 一次性切换，旧 Access Token / Refresh Token 全部失效，前端按本文档改完后再联调，不要兼容旧字段混跑。

---

## 0. 字段对照（前端必改）

不要把「权限节点类型」和已删除的「权限 type」搞混：

| 易混字段 | 是否还在 | JSON 取值 | 用途 |
|---|---|---|---|
| **权限 `type`** | **已删除** | 旧 `"0"` 菜单权限 / `"1"` API 权限 | 不再参与鉴权，请求/响应都不要再带 |
| **权限 `nodeType`** | **保留并作为唯一节点形态** | `"0"` GROUP / `"2"` ACTION | 树展示、创建权限、菜单只能绑 ACTION |
| 角色 `type` | 保留 | `"0"` 角色 / `"1"` 分组 | 与权限无关，不要删 |
| 菜单 `menuType` | 保留 | `"0"` 目录 / `"1"` 菜单 | **不要再传 `"9"` Button** |
| 字典 / 社交等其它 `type` | 保留 | 各自枚举 | 与权限无关 |

### 0.1 删除的字段（请求不要传，响应不要再读）

| 旧字段 | 出现位置 | 前端动作 |
|---|---|---|
| `authorities` | `GET /v1/auth/user/info` | 改用 `GET /v1/auth/user/permissions` 的 `permissions` |
| `type` | 权限树 / 创建权限 / 全量权限树 / 角色权限树 | 删除表单、表格列、过滤条件；不要再用 `"0"`/`"1"` 区分菜单权限和 API |
| `managed` | 权限树节点 | 删除只读/托管标记；节点均可按权限码管理 |
| `readOnly` | 权限树节点 | 同上 |
| `sourceType` / `sourceId` | 权限 | 删除 |
| `permissionId`（单值） | 菜单节点 | 改为 `permissionIds: number[]` |
| `permissionCode` | 菜单节点 | 删除；可见性只认 ID 列表 |
| 菜单 Button 节点 | `menuType: "9"` | 用户菜单与配置树都不再返回按钮；页面按钮用具体权限码 |
| 通配 `:*` | 权限编码 | 只允许 `:**`，例如 `contacts:**` |

### 0.2 改名 / 语义变化

| 旧 | 新 | 说明 |
|---|---|---|
| 权限 `type` = 菜单/API | `nodeType` = GROUP/ACTION | 取值不同：GROUP=`"0"`，ACTION=`"2"`，**没有 `"1"`** |
| 菜单单权限 | `permissionIds` + `permissionMatchMode` | `"0"` ANY（默认）/ `"1"` ALL |
| JWT / token 内业务权限码 | 独立权限接口 | 不要从 token 解析 RBAC |
| 应用列表 `permissionId` | 详情里同时有 `rootPermissionId` | 分页列表仍是实体字段 `permissionId`；详情 VO 映射为 `rootPermissionId` + `rootPermissionCode` |

### 0.3 新增字段 / 新接口

| 字段或接口 | 说明 |
|---|---|
| `UserEffectivePermissionVO.permissions` | 已展开的具体权限码，通配不会出现 |
| `expiresAt` / `version` / `generatedAt` | 这份列表对应的后端快照时刻；**不是**前端定时轮询间隔 |
| `defaultAccessMode` | 应用：`"0"` OPEN / `"1"` CLOSED |
| `resourceId` | 权限节点可选；数据操作应绑定资源 |
| `/apps/{appId}/resources` | 资源目录 CRUD |
| `/role/{id}/data-rules` | 角色数据范围；平台入口与租户入口是两套层 |

---

## 1. 破坏性变化（必须改前端）

1. **`GET /v1/auth/user/info` 不再返回业务 authorities**。按钮/路由守卫改用 `GET /v1/auth/user/permissions` 的 `permissions`。
2. **菜单不再返回 Button 节点**；页面按钮用具体权限码控制。受保护页用 `permissionIds` + `permissionMatchMode`，不再用单个 `permissionId` / path 生成权限。
3. **权限树不再返回 `type` / `managed` / `readOnly`**。展示与创建只用 `nodeType`（GROUP `"0"` / ACTION `"2"`）。创建权限 **不要传 `type`**。
4. **GROUP 通配统一 `:**`**（例如 `contacts:**`），不再用 `:*` 创建。
5. 撤权在后端最多 **30 秒**生效（业务接口鉴权，不靠前端轮询）。`expiresAt` 只说明当前这份 UI 权限列表对应哪次快照，**不要按它 setInterval**。任意已登录接口可能返回 **503**（`AuthorizationSnapshot.Unavailable`），表示授权服务暂时不可用，**不要当成无权限空数组**。
6. JWT / 会话不再展开业务权限码；前端不要从 token 解析 RBAC。
7. 权限增删改走应用中心化接口 `/v1/platform/config/apps/{appId}/permissions`；`GET /v1/platform/config/permission/tree` 仅只读全量树。

---

## 2. 当前登录用户

鉴权：登录态 Bearer。禁止用 query 指定其它用户。

| Method | 路径 | 说明 |
|---|---|---|
| GET | `/v1/auth/user/info` | 用户资料、角色编码、可访问租户；**无 authorities** |
| GET | `/v1/auth/user/menus` | 可见导航树，不含 Button |
| GET | `/v1/auth/user/permissions` | 有效具体权限码（按钮/路由） |

### `UserInfoDTO`

| 字段 | 类型 | 说明 |
|---|---|---|
| user | object | 用户基本信息 |
| roles | `string[]` | 角色编码 |
| allows | 租户列表 | 可切换租户 |
| mustChangePwd | boolean | 必须改密 |
| credentialStatus | string / null | `pwd_expiring_soon` / `pwd_expired_with_grace` |
| daysLeft | number | 即将过期剩余天数 |
| graceRemaining | number | 宽限期剩余次数 |

**已删除：** `authorities`。登录后立刻打 info，确认响应里没有该字段。

### `UserEffectivePermissionVO`

```json
{
  "permissions": ["contacts:user:query", "demo:order:query"],
  "version": 1710000000000,
  "generatedAt": "2026-09-11T01:22:00Z",
  "expiresAt": "2026-09-11T01:22:29Z"
}
```

- `permissions`：当前启用的 **具体** 权限码，用于 `v-if` / 页内能力；通配不会出现，服务端已展开。
- `generatedAt` / `version` / `expiresAt`：这份列表对应的后端快照。`expiresAt` 约 `generatedAt + 30s`，表示 **后端** 授权判定的新鲜度上限，不是前端心跳。
- **不要定时拉取。** 按钮显隐可以暂时不准；真正拦请求的是业务 API（撤权后最多 30 秒内旧授权失效，再请求会 403/503）。

登录后三个接口并行 bootstrap 一次即可；路由守卫只在尚未加载时等待这次结果，**不要每个 `beforeEach` 都打 info / menus / permissions**。

之后只在这些时机再拉 `permissions`：

1. 切换租户、重新登录
2. 窗口重新可见（`visibilitychange` / focus）且本地已过 `expiresAt`（可选，用来尽快藏掉已撤按钮）
3. 业务接口返回明确无权限的 **403**，或快照不可用的 **503** 时再拉一次以对齐界面；503 提示稍后重试，不要清空成游客

菜单同理：bootstrap + 切租户足够。若权限刷新后 `version` 变了，可顺带再拉一次 menus，不必定时拉。

`roles` 在 info 里是身份（展示、少量产品态），**不要**用角色编码做按钮/路由放行。

### 用户菜单节点（`MenuTreeNodeVO`）

```json
{
  "id": 100,
  "pid": 0,
  "name": "用户",
  "menuType": "1",
  "path": "/contacts/user",
  "accessMode": "1",
  "permissionIds": [1001, 1002],
  "permissionMatchMode": "0",
  "viewPath": "contacts/user/index",
  "routeName": "ContactsUser",
  "icon": "user",
  "sort": 1,
  "hidden": false,
  "appId": 10,
  "appCode": "contacts",
  "children": []
}
```

| 字段 | 说明 |
|---|---|
| id / pid / children | 树 |
| name, path, viewPath, routeName, icon, sort, hidden… | 导航字段，与原来一致 |
| menuType | `"0"` 目录 / `"1"` 菜单；**不返回 Button `"9"`** |
| accessMode | `"0"` OPEN / `"1"` PERMISSION |
| permissionIds | 可见性条件权限 ID 列表；**不再返回**旧单列 `permissionId` / `permissionCode` |
| permissionMatchMode | `"0"` ANY（默认）/ `"1"` ALL |

前端路由守卫：**只信任服务端已过滤的菜单树** 做动态路由/侧栏，不要用 `permissionIds` 在前端再算一遍「能不能进这一页」。页内按钮用 bootstrap 得到的 `permissions` 做 `v-if`。

---

## 3. 应用 / 菜单 / 权限（平台配置）

基址 `/v1/platform/config/apps`。操作码见下表，超管或持有该码可调用。

### 3.1 应用

| Method | 路径 | 操作码 |
|---|---|---|
| GET | `/v1/platform/config/apps/page` | `platform:config:app:query` |
| GET | `/v1/platform/config/apps/{appId}` | 同上 |
| POST | `/v1/platform/config/apps` | `platform:config:app:create` |
| PUT | `/v1/platform/config/apps/{appId}` | `platform:config:app:update` |
| PATCH | `/v1/platform/config/apps/{appId}/status` | 同上 |
| DELETE | `/v1/platform/config/apps/{appId}?force=` | `platform:config:app:delete` |

**分页列表**返回实体 `PlatformApp`，根权限字段名仍是 `permissionId`。  
**详情**返回 `AppDetailVO`，对应字段为 `rootPermissionId`、`rootPermissionCode`（形如 `appCode:**`），另有 `menuCount`、`permissionCount`。

**新增字段 `defaultAccessMode`**（创建 / 更新 / 列表 / 详情均有）

- `"0"` OPEN：无租户覆盖时默认开放  
- `"1"` CLOSED：申请制，无覆盖默认不可用  
- 创建缺省 OPEN。全局 `status` 停用优先于该策略。  
- 租户显式覆盖（`tenant_app_config`）到期 **不会** 回退默认。

创建请求示例：

```json
{
  "code": "demo",
  "name": "示例应用",
  "appType": "0",
  "defaultAccessMode": "0"
}
```

创建成功返回应用 ID。服务端自动生成根权限 `{code}:**`（GROUP），前端不要再单独创建根节点，也不要删除根权限。

只读全量树仍可用：`GET /v1/platform/config/menu/tree`、`GET /v1/platform/config/permission/tree`。  
租户应用授权：`GET /v1/platform/org/tenant/apps`、`PUT /v1/platform/org/tenant/app/status`。

### 3.2 菜单

| Method | 路径 | 操作码 |
|---|---|---|
| GET | `/v1/platform/config/apps/{appId}/menus/tree` | `platform:config:app:menu:query` |
| POST | `/v1/platform/config/apps/{appId}/menus` | `platform:config:app:menu:create` |
| PUT | `/v1/platform/config/apps/{appId}/menus/{menuId}` | `platform:config:app:menu:update` |
| DELETE | `/v1/platform/config/apps/{appId}/menus/{menuId}` | `platform:config:app:menu:delete` |

创建/更新请求：

```json
{
  "pid": 0,
  "name": "用户",
  "menuType": "1",
  "path": "/contacts/user",
  "accessMode": "1",
  "permissionIds": [1001, 1002],
  "permissionMatchMode": "0",
  "viewPath": "contacts/user/index",
  "routeName": "ContactsUser",
  "status": "0"
}
```

规则：

- 受保护页面：`menuType=Menu("1")` 且 `accessMode=PERMISSION("1")` → `permissionIds` **非空**，且必须是同应用、启用、**ACTION**（`nodeType="2"`，非通配）。
- Directory / OPEN → `permissionIds` 必须为空。
- **不要传 Button**；不要从 path 生成权限；不要再传 `permissionId` / `permissionCode`。
- 快捷新建权限：先 `POST .../permissions`，再用返回 ID 填 `permissionIds`。两个请求不是跨请求事务；取消菜单时已建权限会保留。
- 删除菜单只删菜单和可见性关联，**不删权限**。

配置树节点字段与用户菜单相同（含 `permissionIds` / `permissionMatchMode`）。

### 3.3 权限

| Method | 路径 | 操作码 |
|---|---|---|
| GET | `/v1/platform/config/apps/{appId}/permissions/tree` | `platform:config:app:permission:query` |
| POST | `/v1/platform/config/apps/{appId}/permissions` | `platform:config:app:permission:create` |
| PUT | `/v1/platform/config/apps/{appId}/permissions/{permissionId}` | `platform:config:app:permission:update` |
| DELETE | `/v1/platform/config/apps/{appId}/permissions/{permissionId}` | `platform:config:app:permission:delete` |

全量只读树：`GET /v1/platform/config/permission/tree`（`PermissionTreeNodeVO`，比应用树多 `appId`）。

#### 树节点（应用树 `AppPermissionTreeNodeVO`）

```json
{
  "id": 1001,
  "pid": 1,
  "name": "查询用户",
  "code": "contacts:user:query",
  "nodeType": "2",
  "orgType": "1",
  "status": "0",
  "remark": "",
  "resourceId": 2001,
  "children": []
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| id / pid / children | 树 | 与原来一致 |
| name / code | string | 编码创建后不可改 |
| nodeType | `"0"` / `"2"` | **唯一节点形态**；不要再读 `type` |
| orgType | `"0"` 平台 / `"1"` 组织 | 从父节点继承，创建时不必传 |
| status | `"0"` / `"9"` | 启用 / 锁定 |
| resourceId | number / null | 数据操作应有值；纯配置可空 |
| remark | string | 可空 |

**节点上已删除：** `type`、`managed`、`readOnly`、`sourceType`、`sourceId`。  
全量树 / 角色权限树同样删除这些字段，并带上 `nodeType`、`resourceId`。

角色权限树额外字段（租户入口 `BizPermissionTreeNodeVO`）：

| 字段 | 说明 |
|---|---|
| platformRoleBind | 是否由平台角色绑定 |
| defaultFlag | 是否预设 |

#### 创建

```json
{
  "pid": 1,
  "name": "查询用户",
  "code": "query",
  "nodeType": "2",
  "resourceId": 2001,
  "status": "0"
}
```

- **不要传 `type`。** `nodeType` 必填，仅 `"0"` GROUP / `"2"` ACTION。
- GROUP 的 `code` 必须以 `:**` 结尾（可写片段，服务端会拼到父编码下）。
- ACTION 必须是不含通配的精确编码。
- `resourceId` 可选；要配数据范围的操作应先建资源再填 ID。
- 更新只接受 `name` / `remark` / `status`，**不可改** `code`、`nodeType`、`resourceId`、应用。
- 仍被菜单、角色或数据规则引用时删除返回 400。
- 应用根权限不可删。

### 3.4 资源目录（新增）

| Method | 路径 | 操作码 |
|---|---|---|
| GET | `/v1/platform/config/apps/{appId}/resources` | `platform:config:app:resource:query` |
| POST | `/v1/platform/config/apps/{appId}/resources` | `platform:config:app:resource:create` |
| PUT | `/v1/platform/config/apps/{appId}/resources/{resourceId}` | `platform:config:app:resource:update` |
| DELETE | `/v1/platform/config/apps/{appId}/resources/{resourceId}` | `platform:config:app:resource:delete` |

创建：`{ "code": "demo-order", "name": "订单", "status": "0" }`  
更新：`{ "name", "status" }`，**code 创建后不可改**。  
仍被权限或数据规则引用时删除 400。

GET 列表字段：`id`、`appId`、`code`、`name`、`status`、`createdAt`、`updatedAt`。

建议配置顺序：资源目录 → 权限绑定 `resourceId` → 菜单关联 `permissionIds` → 角色绑权限 → 角色 data-rules。

---

## 4. 角色权限与数据规则

### 4.1 角色绑定功能权限（既有接口，树字段已变）

| Method | 路径 | 操作码 |
|---|---|---|
| GET | `/v1/platform/config/role/{id}/permissions` | `platform:config:role:permissions:query` |
| PUT | `/v1/platform/config/role/{id}/permissions` | `platform:config:role:permissions:assign` |
| GET | `/v1/org/role/{id}/permissions` | `org:contacts:role:permissions:query` |
| PUT | `/v1/org/role/{id}/permissions` | `org:contacts:role:permissions:set` |

绑定 body 仍为 `{ "setIds": [权限ID, ...] }`（路径上的角色 ID 即可，不必再在 body 重复 `id`）。  
GET 返回权限树：平台入口为 `PermissionTreeNodeVO`，租户入口为 `BizPermissionTreeNodeVO`。勾选/展示请用 `id` + `nodeType` + `code`，**不要依赖 `type`/`managed`**。GROUP 节点用于展开通配，实际绑定仍按现有「选中节点 ID」语义提交。

### 4.2 角色数据规则（新增）

整体替换当前操作者可管理的来源层：平台入口写平台默认层；租户入口只写租户追加层，**不能删改平台默认层**。

| Method | 路径 | 操作码 |
|---|---|---|
| GET | `/v1/platform/config/role/{id}/data-rules` | `platform:config:role:data-rule:query` |
| PUT | `/v1/platform/config/role/{id}/data-rules` | `platform:config:role:data-rule:set` |
| GET | `/v1/org/role/{id}/data-rules` | `org:contacts:role:data-rule:query` |
| PUT | `/v1/org/role/{id}/data-rules` | `org:contacts:role:data-rule:set` |

```json
{
  "items": [
    {
      "permissionId": 1001,
      "resourceId": 2001,
      "scopeType": 3,
      "scopes": []
    }
  ]
}
```

`scopeType`（数字，不是字符串）：

| 值 | 含义 |
|---|---|
| 0 | ALL 全部（只消去该资源行过滤，不扩大其它资源） |
| 1 | CUSTOM，`scopes` 为部门 ID 列表 |
| 2 | DEPT_AND_CHILD 绑定部门及下级 |
| 3 | DEPT 本部门 |
| 9 | SELF 仅本人（资源归属用户字段） |

平台规则 `scopeType=1` 禁止写入租户部门 ID。空 `items` 表示清空该来源层。

GET 返回该来源层完整列表（不是树）：

- 平台层字段：`id`、`roleId`、`permissionId`、`resourceId`、`scopeType`、`scopes`
- 租户层额外：`tenantId`、`platformRole`（是否平台预设角色）

租户 GET **不会**混入平台默认层；前端展示「平台默认 + 租户追加」时，需要分别打两个入口，或产品上只展示当前层。

---

## 5. 错误码与 HTTP

| HTTP | 业务 `code` | 场景 |
|---|---|---|
| 401 | 既有安全过滤器 | 未登录 / token 无效 |
| 403 | `S0403` | `AuthorizationDeniedException`：明确无功能权限或越权写 |
| 403 | `ds_forbidden` | 数据范围无规则 / 未登记资源，写校验拒绝 |
| 503 | `AuthorizationSnapshot.Unavailable` | 授权快照不可用或过期刷新失败，**不要当空权限** |
| 400 | i18n `message` | 受保护页未关联权限、权限仍被引用、部门重复绑定、平台 CUSTOM 含租户部门、缺 `nodeType` 等 |

管理接口 400 的 `message` 为中文提示（i18n）。创建权限缺 `nodeType` 时提示「节点类型不能为空」。

---

## 6. 联调示例（ingot-test，端口 `5210`）

先执行 `databases/ingot_test_demo_data_scope.sql` 建表。再在 PMS 为示例应用登记：

| 资源 code | 权限码 |
|---|---|
| `demo-order` | `demo:order:query` / `create` / `update` / `delete` |
| `demo-announcement` | `demo:announcement:query` / `create` / `update` / `delete` |

给角色绑权限，并配置对应资源的 data-rules（例如 A 部门主管 `DEPT_AND_CHILD`，B 部门成员 `SELF` 或 `DEPT`）。

| Method | 路径 | 说明 |
|---|---|---|
| GET | `/v1/demo/orders` | 列表，行级过滤 |
| POST | `/v1/demo/orders` | body: `{ "title", "deptId", "ownerUserId" }` |
| PUT | `/v1/demo/orders/{id}` | 先满足原范围再校验新归属 |
| DELETE | `/v1/demo/orders/{id}` | 不可见记录表现为 403 |
| GET/POST/PUT/DELETE | `/v1/demo/announcements` | 同上；公告 ALL **不会**扩大订单范围 |

验收：A 部门主管能看到本部门及下级订单；B 部门成员只能看到自己部门/本人订单；公告 ALL 用户仍不能看到越权订单。

旧学生接口 `GET /mybatis/scope` 现需权限 `demo:student:query`、资源 `t_student`。

---

## 7. 枚举 JSON 取值速查

| 枚举 | JSON |
|---|---|
| 应用 defaultAccessMode | `"0"` OPEN / `"1"` CLOSED |
| 菜单 menuType | `"0"` Directory / `"1"` Menu（不要传 `"9"` Button） |
| 菜单 accessMode | `"0"` OPEN / `"1"` PERMISSION |
| 菜单 permissionMatchMode | `"0"` ANY / `"1"` ALL |
| 通用 status | `"0"` 启用 / `"9"` 锁定 |
| **权限 nodeType** | **`"0"` GROUP / `"2"` ACTION**（没有 `"1"`，也没有旧 type） |
| 数据范围 scopeType | `0` ALL / `1` CUSTOM / `2` DEPT_AND_CHILD / `3` DEPT / `9` SELF |

已删除、不要再映射的权限枚举：

- `type`：`"0"` 菜单权限 / `"1"` API 权限
- `nodeType` 旧值 `"1"` NAVIGATION
- `managed` / `readOnly` 布尔托管标记

---

## 8. 建议联调顺序

1. 执行 `022_pms_rbac_data_authorization.sql`、`022_pms_rbac_data_authorization_data.sql`（先 dry-run）、`023_pms_menu_permission_finalize.sql`、`024_pms_permission_drop_legacy_columns.sql`、**`025_pms_permission_drop_type.sql`**、**`026_pms_role_drop_legacy_scope.sql`**。角色 CRUD **不再**返回或接受 `scopeType`/`scopes`，数据范围只走 data-rules。
2. 登录后并行打 `info` / `menus` / `permissions` 各一次（bootstrap）：info **没有** `authorities`；menus 无 Button、无旧 `permissionId`；permissions 用具体码做按钮。之后不要按路由或 30 秒定时再打。
3. 打权限树：确认节点只有 `nodeType`，**没有** `type` / `managed` / `readOnly`。创建权限只传 `nodeType`，不传 `type`。
4. 配置页：资源目录 → 权限绑定 `resourceId` → 菜单关联 `permissionIds` → 角色权限 → 角色 data-rules。
5. 撤权后业务 API 最多 30 秒内拒绝旧授权；前端按钮可能晚一点消失，点了会 403，属预期。任意已登录接口都可能返回 **503** `AuthorizationSnapshot.Unavailable`，提示稍后重试，不要清空权限当成游客。
6. 行级过滤示例直连 ingot-test `http://localhost:5210`（网关默认未配 test 路由）。
