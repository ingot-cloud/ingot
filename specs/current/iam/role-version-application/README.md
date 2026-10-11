# 角色版本信息与批量应用

平台、共享和租户角色支持可读版本信息、历史版本详情与来源发布；平台自定义角色支持受治理边界约束的固定版本批量应用。

负责人 jy 于 2026-10-11 确认人工测试完成。本能力归属 IAM，首轮批量应用限于平台域，不跨租户推广共享角色版本。

- [当前规则、数据模型与运行约束](./SPEC.md)
- [已验收变更](../../../changes/archive/2026/20261010-iam-role-version-application/README.md)
- [接口说明](../../../changes/archive/2026/20261010-iam-role-version-application/API.md)
- [数据库升级说明](../../../../databases/iam/migrations/README.md)
- 关联前端能力：`ingot-admin/specs/current/base/role-version-application/`。

负责人 jy 于 2026-10-11 另行确认角色应用调度优化验收完成，提交后唤醒、低频补偿扫描及多节点运行规则已纳入本能力基线。

- [调度优化变更与验收记录](../../../changes/archive/2026/20261011-iam-role-application-scheduling/README.md)
- [调度配置与运行说明](../../../changes/archive/2026/20261011-iam-role-application-scheduling/OPERATIONS.md)

2026-10-11 完成角色应用引擎职责分离验收。共用流程通过内部适配接口调用平台规则，继续使用独立 worker；本轮没有开放租户批量应用，也没有接入 TSS 调度。

- [职责分离变更与验收记录](../../../changes/archive/2026/20261011-iam-role-application-domain-support/README.md)
- [worker 职责及 TSS 边界](../../../../docs/modules/iam/role-application-worker.md)
