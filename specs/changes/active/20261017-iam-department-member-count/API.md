# API

本变更不新增路径。扩展 `DepartmentRecord`。

## 服务与约定

- 服务：IAM
- Base Path：`/v1`
- 鉴权与权限码不变：`iam-tenant:department:read`
- 响应包装：`R<T>`

## `DepartmentRecord`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | string | 是 | 部门 ID |
| parentId | string | 否 | 父部门 ID，根节点为空 |
| name | string | 是 | 可见部门名称 |
| sortOrder | integer | 是 | 展示顺序 |
| navigationOnly | boolean | 是 | 是否仅作为必要祖先导航骨架 |
| memberCount | integer | 否 | 本部门直属 ACTIVE 成员数。仅租户管理部门列表填充；详情、通讯录、策略骨架省略 |

## 受影响接口

### `GET /v1/tenant/departments`

权限：`iam-tenant:department:read`

查询：`purpose=MANAGED_DEPARTMENT`（强制）。不分页，不接受 `page` / `pageSize`。

响应：`data` 为 `DepartmentTreeNode[]`。唯一根部门与其直属一级部门平级；更深层仍挂在各自父节点的 `children`。每个 `record.memberCount` 必有（含 0）。

### `DepartmentTreeNode`

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| record | DepartmentRecord | 是 | 部门记录 |
| fieldAccess | object | 是 | 字段访问 |
| capabilities | object | 是 | 对象能力 |
| version | string | 是 | 版本 |
| children | DepartmentTreeNode[] | 是 | 子部门，叶节点为空数组 |

### `GET /v1/tenant/departments/{id}`

`record` 不含 `memberCount`。

### `GET /v1/directory/departments`

行为不变，`record` 不含 `memberCount`。
