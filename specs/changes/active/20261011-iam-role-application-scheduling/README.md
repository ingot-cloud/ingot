# 角色版本应用调度优化

> 状态：validating
> 批准：2026-10-11，用户在 Plan 对齐后明确要求执行；负责人 jy。

采用各节点低频兜底、提交后唤醒、数据库租约互斥方案，关联 [角色版本应用](../../archive/2026/20261010-iam-role-version-application/README.md)。仅后端调度优化，不修改前端、公开应用接口或数据库结构；实施和自动化完成后等待实际多节点验收，不提前更新 current。

- [需求与验收](REQUIREMENTS.md)
- [设计与运行配置](DESIGN.md)
- [任务](TASKS.md)
- [证据](EVIDENCE.md)
- [配置与多节点验收](OPERATIONS.md)

2026-10-11：approved门禁检查完成，进入实施。

2026-10-11：实现及自动化完成。完整回归IAM430＋commons64项通过，调度专项15项通过，隔离MySQL含真实争抢/事务提交边界测试。待jy实际多节点验收，不更新current或归档。
