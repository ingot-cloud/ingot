# 应用中心化 REST API 契约

> Change: `20260612-pms-application-authorization`  
> Base path: `/v1/platform/config/apps`  
> OpenAPI：PMS 直连 `http://localhost:5200/v3/api-docs`，或经 Gateway Swagger UI 聚合。

## 前端页面映射

| 页面 | API |
|---|---|
| 应用列表 | `GET /v1/platform/config/apps/page` |
| 创建应用 | `POST /v1/platform/config/apps` |
| 详情 - 基本信息 | `GET /v1/platform/config/apps/{appId}`、`PUT /v1/platform/config/apps/{appId}`、`PATCH /v1/platform/config/apps/{appId}/status` |
| 详情 - 菜单 Tab | `GET/POST/PUT/DELETE .../menus` |
| 详情 - 权限 Tab | `GET/POST/PUT/DELETE .../permissions` |

旧「平台配置 > 菜单管理 / 权限管理」写入页面应隐藏；全量展示可调用只读 `GET /v1/platform/config/menu/tree`、`GET /v1/platform/config/permission/tree`。

## 权限码

| 操作 | 权限码 |
|---|---|
| 应用查询 | `platform:base:app:query` |
| 应用创建 | `platform:base:app:create` |
| 应用更新/启停 | `platform:base:app:update` |
| 应用删除 | `platform:base:app:delete` |
| 应用菜单 | `platform:base:app:menu:query|create|update|delete` |
| 应用权限 | `platform:base:app:permission:query|create|update|delete` |

## 应用

### GET `/page`

分页列表，Query 支持 `appType`、`status`、`name`（与 `PlatformApp` 字段一致）。

**响应示例**

```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 1001,
        "code": "contacts",
        "name": "通讯录",
        "appType": "1",
        "sort": 100,
        "status": "0"
      }
    ],
    "total": 1
  }
}
```

### GET `/{appId}`

**响应示例**

```json
{
  "code": 0,
  "data": {
    "id": 1001,
    "code": "contacts",
    "name": "通讯录",
    "appType": "1",
    "icon": "contacts",
    "intro": "租户通讯录应用",
    "sort": 100,
    "status": "0",
    "rootPermissionId": 2001,
    "rootPermissionCode": "contacts:**",
    "menuCount": 3,
    "permissionCount": 8
  }
}
```

### POST `/`

创建应用，**不要求 menuId**；同事务创建 `code:**` 根权限并绑定组织管理员。

**请求示例**

```json
{
  "code": "contacts",
  "name": "通讯录",
  "appType": "1",
  "icon": "contacts",
  "intro": "租户通讯录",
  "sort": 100
}
```

**响应**：`data` 为新应用 ID（Long）。

**错误**

| 场景 | i18n key |
|---|---|
| 编码重复 | `ApplicationResourceServiceImpl.ExistCode` |
| 根权限编码冲突 | `ApplicationResourceServiceImpl.ExistPermissionCode` |

### PUT `/{appId}`

更新名称、图标、介绍、排序；**code 不可改**。

### PATCH `/{appId}/status`

```json
{ "status": "0" }
```

`0` 启用，`9` 禁用。

### DELETE `/{appId}`

查询参数：`force`（默认 `false`）。

- 普通删除（`force=false`）：存在菜单、子权限（除根权限外）、租户授权或非组织管理员角色绑定时拒绝。删除前自动解除组织管理员对根权限的默认绑定。
- 强制删除（`force=true`）：**仅超级管理员**（`ROLE_ADMIN_CODE`，服务层校验，非超管返回 `ForceDeleteRequireAdmin`）可用，级联清除应用全部菜单、权限及其平台角色绑定。为保护租户数据，**存在租户授权（`tenant_app_config`）时仍拒绝**。前端需做二次确认。

## 应用内菜单

`appId` 来自 URL，请求体中的 `appId` 会被忽略。

### GET `/{appId}/menus/tree`

返回该应用菜单树（`MenuTreeNodeVO`，含 `appId`、`appCode`）。

### POST `/{appId}/menus`

创建菜单并自动创建托管 `NAVIGATION` 权限。根菜单未传 `path` 时默认 `/{appCode}`。`menuType=Directory` 时权限码追加 `:**`。

> 菜单即应用：当根级菜单的 path 转换码与应用编码一致（例如应用 `platform:config` + 菜单 `/platform/config`）时，视为"菜单即应用"，直接复用应用根权限 id，不再新建权限（避免重复编码与嵌套码）。

**请求示例**

```json
{
  "pid": 0,
  "name": "用户管理",
  "path": "/contacts/user",
  "accessMode": "1",
  "sort": 10
}
```

`accessMode`：`0` 开放，`1` 需权限（访问模式唯一来源，旧 `enable_permission` 字段已废弃）。

### PUT `/{appId}/menus/{menuId}`

同步更新托管权限名称与状态；`access_mode` 切换不删除权限关系。

### DELETE `/{appId}/menus/{menuId}`

叶子菜单且托管权限下无子权限时可删。若该菜单复用应用根权限（菜单即应用），删除时仅删除菜单行，保留应用根权限及其角色绑定（由删除应用统一管理）。

## 应用内权限

### GET `/{appId}/permissions/tree`

节点类型 `AppPermissionTreeNodeVO`：

- `managed`：菜单托管导航权限
- `readOnly`：托管/导航权限，不可通过权限接口修改

### POST `/{appId}/permissions`

仅 `GROUP` / `ACTION`；`NAVIGATION` 由菜单生命周期创建。

**请求示例**

```json
{
  "pid": 2001,
  "name": "用户模块",
  "code": "user:*",
  "nodeType": "0"
}
```

编码规则：

- `GROUP`（`nodeType=0`）必须以 `:*` 结尾
- `ACTION`（`nodeType=2`）必须为精确编码，不含 `*`
- 须落在应用命名空间 `{appCode}:` 下；可传片段（如 `user:*`）或完整编码

### PUT `/{appId}/permissions/{permissionId}`

仅非托管、非导航权限可更新（name/remark/status）。

### DELETE `/{appId}/permissions/{permissionId}`

不可删根权限、托管权限、导航权限；无角色绑定的叶子节点可删。

## 全量树查询（前端展示）

菜单/权限的增删改统一走应用中心化接口；以下只读接口返回全量树，供前端展示所有菜单/权限：

| 用途 | 接口 |
|---|---|
| 全量菜单树 | `GET /v1/platform/config/menu/tree` |
| 全量权限树 | `GET /v1/platform/config/permission/tree` |

## Deprecated 旧接口

旧 `PlatformAppAPI` 保留兼容，OpenAPI 标记 `deprecated`：

| 旧路径 | 替代 |
|---|---|
| `GET/POST/PUT/DELETE /v1/platform/base/app` | `/v1/platform/config/apps` |

旧 `/v1/platform/base/menu`、`/v1/platform/base/permission` 的写接口已移除；查询能力迁移到上文 `/v1/platform/config/{menu,permission}/tree`。

## 验收联调顺序

1. 测试库执行 `databases/migrations/007_application_authorization.sql`
2. Postman/Apifox 调用本契约全流程
3. 前端：应用列表 → 创建 → 详情内维护菜单/权限
4. 登录验证菜单与按钮权限
5. `GET /v1/platform/dev/authorization/audit` 与 `snapshot` 无阻断项
