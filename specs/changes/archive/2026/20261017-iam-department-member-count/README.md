# 租户部门列表补充直属成员数

> 状态：completed（已验收，已更新 current，已归档）

## 完成与归档（2026-10-17）

- 验收依据：用户明确要求“归档当前修改的这个内容对应的spec，并且提交”。
- 完成与归档日期：2026-10-17。此前 implementing、待浏览器联调描述为历史阶段记录，以本次确认结项。
- 关联实现提交：`be1cfad0`；关联前端 `60d262a`。验收、基线及归档由独立 docs 提交记录。
- 当前能力：[iam/tenant-departments](../../../../current/iam/tenant-departments/README.md)，有效规则见 [SPEC.md](../../../../current/iam/tenant-departments/SPEC.md)。
- 另一仓库对应归档：`ingot-admin/specs/changes/archive/2026/20261017-org-members-dept-tabs/`。
- 最终实现与设计差异：实施中补齐组织管理「成员与部门」目录种子、嵌套目录默认 `layout.simple`，以及已有库 `023_tenant_members_departments_menus.sql`；管理部门列表改为一次返回树。成员列表公开 `departmentId` / `status` 筛选。无 DDL。

本归档只关闭部门计数与组织菜单增量。既有 IAM 总体变更保持 implementing，其余任务与验收状态不受本次确认影响。本次未重新执行真实页面或目标库迁移验收。

## 元数据

- Change ID：`20261017-iam-department-member-count`
- 领域：IAM / 租户部门
- 负责人：jy
- 创建日期：2026-10-17
- 目标发布日期：与前端 `20261017-org-members-dept-tabs` 一同验收
- 前端关联：`ingot-admin/specs/changes/archive/2026/20261017-org-members-dept-tabs/`

## 目标

租户部门列表返回本部门直属、状态为 ACTIVE 的成员数，供管理后台部门页展示。详情、通讯录与策略选择器不携带该字段。

## 范围

包含：

- `DepartmentRecord` 增加可选 `memberCount`
- `GET /v1/tenant/departments` 改为一次返回管理部门树，并批量填充计数；主部门与直属一级部门平级
- OpenAPI / schemas 同步
- 列表计数与 SQL 测试
- 组织管理「成员与部门」目录种子与 `023` 迁移

不包含：

- 部门主管表、主管动态范围或从组织属性推导授权
- 部门状态（启用/停用）
- 含子部门的人数
- 通讯录分页契约变更
- 通讯录 `GET /v1/directory/departments` 计数
- 提前改写 `specs/current/`

## 延后决定：部门主管

本次不建主管模型。后续独立后端设计变更的方向：

1. 新增部门主管表，只记录组织事实，用于展示与通讯录。
2. 主管本身不授予权限。
3. 若以后需要「我主管的部门」这类动态范围，仍须先显式分配角色才生效。

这符合既有 IAM 原则：任职只影响 `MEMBER_DEPARTMENTS`；指定管理部门不随组织属性自动变化；不从导航、名称或组织属性推导授权。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [接口](./API.md)
- [任务](./TASKS.md)
- [验收](./ACCEPTANCE.md)

## 相关基线

- [IAM 主变更](../../../active/20260912-iam-identity-access-management/README.md) 保留状态；本变更只扩展 `DepartmentRecord` 的列表可选字段。
- 前端 UI03 布局由 `ingot-admin/specs/changes/archive/2026/20261017-org-members-dept-tabs/` 接管。

## 完成记录

- 完成日期：2026-10-17
- 关联提交或 PR：实现 `be1cfad0`；无 PR
- 更新的 current capability：`specs/current/iam/tenant-departments`
- 与原设计的差异：补齐嵌套目录 `layout.simple`、菜单种子与 `023` 迁移；管理部门列表改为树响应
- 取消原因：
