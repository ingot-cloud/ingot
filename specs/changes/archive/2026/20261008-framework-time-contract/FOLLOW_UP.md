# 既有测试问题独立跟踪

2026-10-11，jy 明确接受“联调已验收，既有问题另行跟踪”。以下问题不属于时间契约实现，不作为其未完成业务任务；原 2026-10-08 全量回归失败事实保留于 [验收记录](./ACCEPTANCE.md)。本文件跟踪事实与归属，不批准新的业务实现方案。

## 身份部门列表可变性

- 状态：待独立处理；负责人 jy。
- 用例：`security-common / InUserIdentityTest.callerCannotMutateBoundDepartmentList`。
- 现象：用例期望调用方不能修改绑定后的部门列表；`InUser` 的 `copyDeptIds` 返回 `new ArrayList<>(deptIds)`，getter 暴露该列表，未抛出期望的 `UnsupportedOperationException`。
- 归档复核：该测试与部门列表实现相对时间代码提交 `596e8bd9` 没有变化，未在本次归档修复或重跑该失败用例。
- 跟进边界：在身份模型的独立工作中明确集合可变性契约、存储反序列化与调用方兼容性，再按 SDD 对齐并实施；不以删除测试或修改时间编码掩盖问题。

## 主题 Token 清单不一致

- 历史用例：`admin-core / defaultTokens.test.ts` 的“公开 Token 名与浅色 CSS 声明一致”。
- 原失败：CSS 已有 `--in-permission-panel-bg`，公开 Token 名清单缺失；保留该次失败记录。
- 后续状态：已由前端独立提交 `9a1262e` 补齐 `tokens.ts`、默认主题值等关联声明，归档复核当前代码已有该 Token。
- 独立验证来源：`ingot-admin/specs/changes/active/20261010-packages-unified-tags/EVIDENCE.md` 记录 admin-core 全量 440 项通过及最终定向 36 项通过，包含主题默认值和视觉契约。
- 此后续修复不改写原时间改造的历史全量结果，也不意味着框架标签变更已人工验收归档；前端独立 change 保留自身状态。
