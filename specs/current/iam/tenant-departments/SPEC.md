# 租户部门目录

## 管理部门树

`GET /v1/tenant/departments` 仅接受 `purpose=MANAGED_DEPARTMENT`，权限为 `iam-tenant:department:read`。一次返回当前身份可见的全部部门树，不分页，不接受 `page` / `pageSize`。响应为 `DepartmentTreeNode[]`：唯一根部门与其直属一级部门平级，更深层仍挂在各自父节点的 `children`。

`GET /v1/tenant/departments/{id}`、通讯录 `GET /v1/directory/departments` 以及策略/选择器构造保持原语义，不改成分页树。

## 直属成员数

`DepartmentRecord.memberCount` 为可选整数，只在租户管理部门树填充。口径为本部门直属任职且成员状态为 `ACTIVE`，不含下级部门人数。无直属 ACTIVE 成员时填 `0`，不省略。详情、通讯录、策略骨架和 `navigationOnly` 祖先骨架省略该字段。

计数按当前可见部门 ID 一次 `GROUP BY`，禁止按部门 N+1。无可见部门时跳过计数 SQL。计数失败随列表失败，不 fail-open 为 `0`。未出现在计数结果中的部门视为 `0`。

删除空部门的既有校验仍统计全部任职（含暂停），与列表口径分离，避免暂停成员所在部门被误删。计数不改变对象能力、范围求值或授权。

## 成员列表筛选

`GET /v1/tenant/members` 可按直属 `departmentId` 与 `status=ACTIVE|SUSPENDED|REMOVED` 过滤。不传 `status` 时排除已移出成员。精确 `phone` / `email` 筛选保持不变。

## 组织菜单种子

组织管理下「成员与部门」为二级目录，下面再挂「成员」「部门」两个页面。新库由 `tools/iam/generate_database.py` 写入；已有库执行 `023_tenant_members_departments_menus.sql`。

| 名称 | route_name | path | view_path | 绑定 ACTION |
|---|---|---|---|---|
| 成员与部门 | `org.iam.members.root` | `/org/iam/members/root` | `layout.simple` | `iam-tenant:member:read`、`iam-tenant:department:read` |
| 成员 | `org.iam.members` | `/org/iam/members` | `org.iam.members` | `iam-tenant:member:read` |
| 部门 | `org.iam.departments` | `/org/iam/departments` | `org.iam.departments` | `iam-tenant:department:read` |

带子集的二级及以下目录默认 `view_path=layout.simple`。顶层目录仍用 `layout.main`。组织管理排序为成员与部门、用户组、组织设置。

## 兼容与回滚

无 DDL。旧客户端忽略未知 `memberCount`。回滚后端即停止输出该字段，并恢复原菜单父子关系。
