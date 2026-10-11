# 角色版本信息与批量应用

平台、共享和租户角色支持可读版本信息、历史版本详情与来源发布；平台自定义角色支持受治理边界约束的固定版本批量应用。

负责人 jy 于 2026-10-11 确认人工测试完成。本能力归属 IAM，首轮批量应用限于平台域，不跨租户推广共享角色版本。

- [当前规则、数据模型与运行约束](./SPEC.md)
- [已验收变更](../../../changes/archive/2026/20261010-iam-role-version-application/README.md)
- [接口说明](../../../changes/archive/2026/20261010-iam-role-version-application/API.md)
- [数据库升级说明](../../../../databases/iam/migrations/README.md)
- 关联前端能力：`ingot-admin/specs/current/base/role-version-application/`。

后续调度优化仍由独立 [change](../../../changes/active/20261011-iam-role-application-scheduling/README.md) 管理，本次人工验收结论不替代其多节点验收。
