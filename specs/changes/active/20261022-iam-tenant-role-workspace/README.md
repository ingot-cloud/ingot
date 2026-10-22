# 租户角色工作区

> 状态：implementing

## 元数据

- Change ID：`20261022-iam-tenant-role-workspace`
- 领域：`iam`
- 负责人：`jy`
- 创建日期：`2026-10-22`
- 目标发布日期：TBD

## 目标

组织角色页需要按当前有效成员和用户组分页，并查看一名成员的直接分配与组继承来源。租户角色没有业务版本，不能复用带 `revisionId` 的平台工作区接口。

## 范围

包含：

- `GET /v1/tenant/roles/{id}/members`
- `GET /v1/tenant/roles/{id}/groups`
- `GET /v1/tenant/roles/{id}/members/{memberId}/assignments`
- 组继承含显式成员和部门展开
- 租户角色与平台共享角色的当前字段快照、目录字段能力和求值取舍

不包含：

- 角色业务版本、升级
- 改分配写入语义
- 平台自有角色的版本发布
- 通讯录字段策略
- 提前改 `specs/current/`

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：
- 关联提交或 PR：
- 更新的 current capability：
- 与原设计的差异：
- 取消原因：
