# 已移出租户成员重新加入

> 状态：completed（已更新 current，已归档）

## 完成与归档（2026-10-20）

- 完成日期：2026-10-20。自动化测试通过后更新 current 并归档。
- 当前能力：[iam/tenant-members](../../../../current/iam/tenant-members/README.md)
- 另一仓库对应归档：`ingot-admin/specs/changes/archive/2026/20261020-org-tenant-member-rejoin/`
- 与原设计的差异：策略选择器无独立 version；已移出详情 `GET` 仍排除 REMOVED

## 元数据

- Change ID：`20261020-iam-tenant-member-rejoin`
- 领域：`iam`
- 负责人：`jy`
- 创建日期：`2026-10-20`
- 目标发布日期：`TBD`

## 目标

已移出（`REMOVED`）的租户成员可以重新加入本组织：复用原成员行改回 `ACTIVE`，按本次提交覆盖姓名、联系方式和部门，并清掉旧的角色授权、用户组、负责人等关系。入口包括「添加成员」按手机号命中已移出记录，以及已移出列表上的「重新加入」。

## 范围

包含：

- `POST /v1/tenant/members` 在手机号命中已移出成员时自动重新加入
- `POST /v1/tenant/members/{id}/rejoin`
- 重新加入时清理该成员的授权关系并覆盖通讯录资料

不包含：

- 平台成员重新加入
- 恢复移出前的角色、用户组或负责人
- 新增 `IamAction` 或数据迁移
- 修改 `joinedAt` 语义（仍为首次加入的 `created_at`）

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)
- [验收](./VALIDATION.md)

## 完成记录

- 完成日期：2026-10-20
- 关联提交或 PR：未提交
- 更新的 current capability：`specs/current/iam/tenant-members`
- 与原设计的差异：策略选择器表无独立 version，清理成员引用后不递增选择器版本；已移出详情 `GET` 仍排除 REMOVED，详情重新加入入口使用列表行资料
- 取消原因：
