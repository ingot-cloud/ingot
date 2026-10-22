# Design

## 方案摘要

在 `TenantRoleCommandAPI` 增加三个只读接口，由 `TenantLiveAssignmentService` 调用 `TenantRoleWorkspaceMapper`。查询使用 `SelectProvider`，在 SQL 内展开组继承后去重分页。

无表结构变更，不接受业务版本。

## 数据模型与接口

`RoleSubjectPage` / `RoleSubjectSummary` 复用 commons。租户响应里 `revisionNumbers` 为空，`versionNames` 为空。

| 方法 | 路径 | 响应 |
|---|---|---|
| GET | `/v1/tenant/roles/{id}/members` | `RoleSubjectPage` |
| GET | `/v1/tenant/roles/{id}/groups` | `RoleSubjectPage` |
| GET | `/v1/tenant/roles/{id}/members/{memberId}/assignments` | `PageResponse<ResourceDetail<TenantAssignmentRecord>>` |

查询参数为 `keyword`、`page`、`pageSize`。三个接口都是 `IamReadOnlyRequest`。进入查询前先 `roleService.get` 确认角色可见，再准入 `TENANT_ASSIGNMENT_READ`。

角色权限选择先选定应用。`GET /v1/tenant/roles/grant-catalog` 一次返回该应用下全部已开通操作，查询参数为 `applicationId`，可选 `keyword` 与 `ids`，不再接受页码。平台 `GET /v1/platform/applications/{id}/grant-catalog` 同样一次返回该应用全部启用资源及其启用操作。左侧应用列表仍分页。

## 数据流

1. 成员页公共表展开 `iam_tenant_group_department`。`include_descendants` 为真时递归子部门，再并上 `iam_tenant_group_member`。
2. 直接分配关联 `iam_tenant_member`。组继承用展开后的成员关联同一张分配表。
3. 分配需 `ACTIVE`、角色启用、当前时间落在有效期内。委派来源还需委派本身有效、分配带失效时间、时长不超过委派上限，且角色在白名单内。
4. 治理者的可见分配沿用 `TENANT_ASSIGNMENT_READ` 对象范围。受限管理员用本人委派 ID 过滤 `delegation_grant_id`，直接分配不进入该视图。
5. 组可见性：无组读取资格返回空列表；范围覆盖全部则为空过滤；否则使用对象 ID。`inheritedSourcesRestricted` 在组过滤不为空时为真。
6. 名称关键字沿用包含匹配转义。来源查询复用同一关系，再按分配 ID 回读详情投影，有效状态仍由 `detail()` 计算。

## 边界

本聚合不重复展开委派接收人部门，也不在 SQL 中复查管理员账号有效性。因此偶发 `SOURCE_INVALID` 的委派分配仍可能出现在成员聚合中。来源对话框的每一行会经 `detail()` 给出有效状态。

## 迁移与回滚

无数据迁移。回滚即下线三个路径，分配写入不受影响。

## 字段权限

`CurrentRoleDefinition.resourceFieldPermissions` 保存当前字段快照。`CurrentRoleCatalogAction` 带回资源字段能力和默认值；通讯录资源这两项为空。

`RoleFieldPermissionService.freeze` 对 `TENANT_CUSTOM` 和 `SHARED` 与 `PLATFORM_CUSTOM` 使用同一套固化，并跳过通讯录资源。创建、预览、保存当前权限都调用它，内部快照写入 JSON 对象，不再写 `null`。

租户求值：

- 通讯录继续走字段策略。
- 成员资源先算字段策略，再与角色快照并集求交。全部来源都是旧空快照时不收紧。拥有者不求交。
- 其他业务资源走当前角色快照。旧空快照按资源默认值，显式快照缺少的字段隐藏。

## 测试策略

- 现有 `TenantLiveMySqlHttpTest` 构造函数补入工作区 Mapper。
- 编译 `ingot-iam-provider`。部门展开与去重留待具备数据库的集成验证。
