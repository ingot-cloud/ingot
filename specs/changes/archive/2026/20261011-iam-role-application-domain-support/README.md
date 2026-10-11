# 角色版本应用引擎职责分离

> 状态：completed

## 元数据

- Change ID：`20261011-iam-role-application-domain-support`
- 领域：IAM；负责人：jy。
- 创建与批准日期：2026-10-11。
- 批准依据：用户在 Plan 模式选择“仅重构并预留扩展”，并在方案完成后明确要求“执行计划”，同时强调收敛、灵活性、安全性和性能。
- 验收、完成与归档日期：2026-10-11。
- 关联提交：与本次代码同一提交，标题为 `refactor(iam): 隔离角色应用平台规则以复用任务执行流程`；未创建 PR。
- 更新的 current capability：[iam/role-version-application](../../../../current/iam/role-version-application/README.md)。
- 最终实现与原设计差异：无；租户批量应用与 TSS 调度接入均保持非目标。

## 目标与范围

将平台专属身份、治理权限、角色及分配访问收拢到一个内部适配器，保留共用计划与任务执行流程。只实施平台适配器，不开放租户批量应用或独立 App 用户授权；不建设通用任务框架，不把现有 worker 强行接入 TSS。

公开接口、数据库结构、前端、配置、事件、缓存及调度运行契约均保持不变。实施遵循 approved 门禁；验收完成后更新 current，不修改既有归档。

## 工件与关联

- [需求与验收](./REQUIREMENTS.md)
- [设计、兼容及回退](./DESIGN.md)
- [任务与实施状态](./TASKS.md)
- [实施与自动化证据](./EVIDENCE.md)
- [当前角色应用基线](../../../../current/iam/role-version-application/SPEC.md)
- [已归档的版本应用](../20261010-iam-role-version-application/README.md)
- [已归档的调度优化](../20261011-iam-role-application-scheduling/README.md)
- [仍在实施的 IAM 总体变更](../../../active/20260912-iam-identity-access-management/README.md)：本 change 不替代其状态或修改其接口契约。

## 完成记录

2026-10-11：approved 门禁检查通过，开始按已批准范围实施。

2026-10-11：实施及自动化完成。角色应用专项 27 项、IAM 完整回归 442 项、TSS Spring 2 项、Python 15 项及契约/数据库检查通过，详见证据。没有设计偏离；等待人工验收，未更新 current、归档或提交。本轮不创建前端关联 change。

2026-10-11：用户确认更新角色权限范围并应用到当前匹配角色的人工测试成功，随后明确要求归档。结合自动化结果完成验收，补齐 current 的内部扩展边界与 TSS 职责说明，状态改为 completed 并归档。没有代码或契约调整，未创建提交。

2026-10-11：用户后续明确要求提交相关代码，本 change、current 基线、实现、测试及运行说明一并纳入上述提交。
