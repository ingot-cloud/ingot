# 租户部门目录

租户管理部门一次返回可见树，保留直属成员数口径。普通部门独立维护零或多位负责人及唯一主负责人；任命不授予权限或数据范围。部门详情聚合可见上级名称与负责人首屏，基本信息和负责人按同一版本原子编辑。

- 负责人：jy；最近验收日期：2026-10-20。
- 关联模块：`ingot-iam-provider`、IAM 种子生成器。
- 关联前端：`ingot-admin/specs/current/org/members-departments/`。
- [当前规则与接口约束](./SPEC.md)
- [已验收变更](../../../changes/archive/2026/20261017-iam-department-member-count/README.md)
- [部门负责人已验收变更](../../../changes/archive/2026/20261019-iam-department-leaders/README.md)
- [负责人接口说明](../../../changes/archive/2026/20261019-iam-department-leaders/API.md)
- [组织成员选择器接口与范围](../organization-member-picker/README.md)
- [接口说明](../../../changes/archive/2026/20261017-iam-department-member-count/API.md)
- [数据库升级说明](../../../../databases/iam/migrations/README.md)
