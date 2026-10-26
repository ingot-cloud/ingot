# 收掉租户普通通讯录

> 状态：implementing

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

- 完成日期：
- 关联提交或 PR：
- 更新的 current capability：
- 与原设计的差异：
- 取消原因：
