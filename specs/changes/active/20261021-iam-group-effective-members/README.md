# 租户组有效成员分页

> 状态：implementing

## 元数据

- Change ID：`20261021-iam-group-effective-members`
- 领域：`iam`
- 负责人：`jy`
- 创建日期：`2026-10-21`
- 目标发布日期：TBD

## 目标

组织用户组工作区需要按「有效成员」分页展示直接添加与部门来源展开后的去重集合。现有 `GET /v1/tenant/groups/{id}/members` 只返回直接绑定，不能改语义。本次新增有效成员接口，并标出来源，供前端右侧列表使用。

## 范围

包含：

- `GET /v1/tenant/groups/{id}/effective-members`
- 来源标记：`direct` 与命中的部门规则
- 契约、测试

不包含：

- 改直接成员接口
- 物化组成员或新增权限码
- 平台组（无部门来源）

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
