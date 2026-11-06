# Design

## 方案摘要

新增租户组有效成员分页。集合复用 `GROUP_MEMBERSHIP`，在 `OrganizationMemberRepository` 用成员范围与可选显示名过滤后分页。当前页再解析直接绑定与部门规则来源。

## 数据模型与接口

无表结构变更。新增 commons 记录：

- `GroupEffectiveMemberRecord`：`member` + `membership`
- `GroupMembership`：`direct`、`departments`
- `GroupMembershipDepartment`：`id`、可见时的 `name`、`includeDescendants`

`GET /v1/tenant/groups/{id}/effective-members?page&pageSize&name`

- 权限 `iam-tenant:group:read`
- 响应 `R<PageResponse<GroupEffectiveMemberRecord>>`
- 显示名搜索走字段策略 `requireOriginalLookup`

直接成员接口不改。

## 数据流与失败处理

1. `group:read` + 组对象可见。
2. `member:read` 范围与 `GROUP_MEMBERSHIP` 求交后分页；无成员读资格则拒绝。
3. 当前页：直接绑定 IN 查询；部门规则用任职 + `DepartmentClosure` 判定；部门名按 `department:read` 范围披露。
4. 成员字段投影与组织成员列表相同。组不存在按对象不存在处理。

## 迁移与回滚

无数据迁移。回滚即下线该路径，直接成员接口不受影响。

## 测试策略

- Repository：任职变化后分页跟上、含下级、去重
- `GroupMembershipSources`：来源标记、不可见部门省略名称
- 直接成员查询仍只走 `iam_tenant_group_member`
- 契约测试纳入新 schema
