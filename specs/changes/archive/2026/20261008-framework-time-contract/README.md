# 框架统一时间契约与 UTC 处理

> 状态：completed
> 完成与归档：2026-10-11；验收负责人：jy。

- Change ID：20261008-framework-time-contract
- 负责人：jy
- 日期：2026-10-08
- 批准依据：用户完成 Plan 对齐后明确要求 IMPLEMENT THIS PLAN；无需再次确认。
- 关联仓库：ingot / ingot-admin，后端契约为事实来源。
- 代码提交：ingot `596e8bd9d4acd56241c215c81e0faffd642b90b0`；ingot-admin `8ba7eb84128c2d90ae1dfc982422c3a622d93a8d`。

## 工件

- [需求](./REQUIREMENTS.md)
- [设计](./DESIGN.md)
- [任务](./TASKS.md)

## 验收

时间相关实施与验证已完成；2026-10-11，用户确认实际联调已验收，既有问题另行跟踪，随后更新 current 并归档。

开工记录：先生成 approved 工件与完整 TASKS，随后进入 implementing。

实现完成时进入 validating；[验收记录](./ACCEPTANCE.md) 保留实施阶段证据、历史待办及最终关闭决议。

## 完成记录

2026-10-11，用户要求归档本 Spec 并提交，随后明确确认“联调已验收，既有问题另行跟踪”。该答复作为实际联调验收与既有问题处理的最终决议；不将历史全量回归失败改记为通过，也不声称 Agent 执行了额外的部署或联调。

- 当前能力：[framework/time-contract](../../../../current/framework/time-contract/README.md)，有效规则见 [SPEC.md](../../../../current/framework/time-contract/SPEC.md)。
- 关联实现提交：后端 `596e8bd9d4acd56241c215c81e0faffd642b90b0`；前端 `8ba7eb84128c2d90ae1dfc982422c3a622d93a8d`。本次提交仅保存本仓库归档、基线及文档。
- 既有问题独立跟踪：[FOLLOW_UP.md](./FOLLOW_UP.md)，未扩大为时间改造中的业务修复。
- 最终设计说明：按 2026-10-08 用户确认，由业务 JDBC URL 设置 UTC 连接和会话，公共数据库配置移除重复项；无其他未批准的设计偏离。
- 本次只归档 `20261008-framework-time-contract`，关联前端 `20261008-packages-time-contract` 保留本次操作前的状态；既有 IAM 及其他 active 变更不随本次归档关闭。
