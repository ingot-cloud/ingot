# Requirements

## 用户场景

组织管理员打开用户组工作区，选中左侧组后，右侧列出该组当前有效成员。部门加人后刷新即可看到新人，无需改组。

## 业务规则

1. 有效成员 = `iam_tenant_group_member` 显式成员 ∪ 部门来源按任职实时展开，去重。口径与 `IamMembershipSql.GROUP_MEMBERSHIP`、`visibleMemberCount` 一致。
2. `include_descendants=true` 时计入下级部门任职；`false` 只计该部门。
3. 任职变化立即生效，不回写组表。
4. 分页在 SQL 完成后再解析当前页来源。不额外排除停用账号或已移出成员；全范围查看时 total 与 `visibleMemberCount` 对齐。
5. 权限：`iam-tenant:group:read` 且组对象可见；成员按 `iam-tenant:member:read` 范围过滤并做字段投影。
6. 来源：`membership.direct`；`membership.departments` 为命中的组部门规则。看不到的部门名省略，前端显示「部门范围」。
7. `GET /v1/tenant/groups/{id}/members` 仍只返回直接成员。

## 边界与非目标

- 不提供全量有效成员 ID 列表。
- 不把部门成员写入 `iam_tenant_group_member`。
- 平台组不增加本接口。

## 验收标准

- [ ] 部门加人后有效成员页出现该人，组表无新行
- [ ] 含下级只在规则打开时计入子部门
- [ ] 直接 + 部门同时命中只出现一次，两种来源都标出
- [ ] 直接成员接口仍只返回显式绑定
