# 租户按手机号添加成员

> 状态：implementing

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

- 完成日期：
- 关联提交或 PR：
- 更新的 current capability：
- 与原设计的差异：
- 取消原因：
