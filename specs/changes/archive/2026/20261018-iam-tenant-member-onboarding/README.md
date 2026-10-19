# 租户按手机号添加成员

> 状态：completed（已验收，已更新 current，已归档）

## 完成与归档（2026-10-19）

- 验收依据：用户明确要求归档 `20261018-iam-tenant-member-onboarding` 并提交。
- 完成与归档日期：2026-10-19。此前 implementing 为历史阶段记录，以本次确认结项。
- 关联实现提交：`5865e4cf`。验收、基线及归档由独立 docs 提交记录。
- 当前能力：[iam/tenant-members](../../../../current/iam/tenant-members/README.md)，有效规则见 [SPEC.md](../../../../current/iam/tenant-members/SPEC.md)。
- 另一仓库对应归档：`ingot-admin/specs/changes/archive/2026/20261018-org-tenant-member-create-dialog/`。
- 最终实现与设计差异：同一实现同时让成员列表、详情和导出返回 `loginPhone`，与组织联系手机号分开。契约断言写在模型测试中，未整份重写 OpenAPI 快照。

本归档只关闭租户按手机号添加成员。平台创建成员仍按账号 ID。既有 IAM 总体变更不受本次确认影响。

## 元数据

- Change ID：`20261018-iam-tenant-member-onboarding`
- 领域：`iam`
- 负责人：`jy`
- 创建日期：`2026-10-18`
- 目标发布日期：`TBD`

## 目标

租户「添加成员」按手机号匹配全局账号：没有账号时新建并返回一次性初始密码，已有账号则加入本组织。不再要求调用方提交账号 ID。

## 范围

包含：

- 租户 `POST /v1/tenant/members` 的入参、出参与入职事务
- 手机号冲突、账号停用、重复加入的明确错误

不包含：

- 平台 `POST /v1/platform/members` 与 `MemberCreateInput`
- 飞书人事字段（人员类型、入职日期、上级、职务）
- 已离职成员重新激活

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：2026-10-19
- 关联提交或 PR：实现 `5865e4cf`；无 PR
- 更新的 current capability：`specs/current/iam/tenant-members`
- 与原设计的差异：成员投影增加公开字段 `loginPhone`；未整份重写 OpenAPI 快照
- 取消原因：
