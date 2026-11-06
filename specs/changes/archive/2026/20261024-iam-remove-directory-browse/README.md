# 收掉租户普通通讯录

> 状态：completed

## 元数据

- Change ID：`20261024-iam-remove-directory-browse`
- 领域：`iam`
- 负责人：jy
- 创建日期：`2026-10-24`
- 目标发布日期：TBD

## 目标

去掉尚未上线的普通通讯录浏览，以及只为它服务的可见范围策略、字段策略 DIRECTORY 场景和策略预览。组织成员管理继续使用字段策略的 MANAGEMENT 场景。

## 范围

包含：

- 删除 `/v1/directory/*`、`/v1/tenant/policies/directory`、`/v1/tenant/policies/preview`
- 删除资源 `directory`、`directory-policy`、`policy` 及对应动作
- 删除通讯录菜单，工作台目录不再绑定 `directory:read`
- 删除通讯录策略表、DIRECTORY 默认版本和 DIRECTORY 字段规则
- 字段规则场景只保留 MANAGEMENT

不包含：

- 组织成员、部门接口
- 字段策略 MANAGEMENT 场景与 `field-policy` 资源
- 成员资料手机号、邮箱，以及成员工作簿文件名

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 完成记录

- 完成日期：2026-11-06（用户确认产品验收通过并要求归档）。
- 关联实现提交：c43c2c3f。
- 更新的 current：[iam/platform-capabilities](../../../../current/iam/platform-capabilities/README.md)、[iam/tenant-field-policy](../../../../current/iam/tenant-field-policy/README.md)、[iam/database-persistence](../../../../current/iam/database-persistence/README.md)。
- 最终交付：普通通讯录与可见范围策略退出。原 MANAGEMENT 场景方案随后由按资源字段策略接管。
