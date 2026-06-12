# Agent Workflow

本仓库采用 Spec-Driven Development。任何影响业务行为、数据模型、公共接口或部署兼容性的变更，都必须遵循以下规则：

1. 代码变更前，阅读 [SDD 工作流](./specs/README.md)，并检索相关 `specs/current/` 与 `specs/changes/active/`。
2. 在 Plan 模式完成需求、设计、兼容性、测试和验收标准对齐后，才能创建 active change。
3. Active change 状态必须为 `approved`，且 `TASKS.md` 决策完整，才能开始实施。
4. 实现偏离已批准设计时，先更新 Spec 并重新确认，再继续修改代码。
5. 实施期间同步维护任务和阶段状态，不提前修改 `specs/current/`。
6. 变更验收完成后，必须更新 current 系统基线，再将 change 移入 archive。
7. 未完成、取消或被替代的变更不得删除，必须记录原因后归档。

详细目录、状态机、工件职责和归档规则以 [specs/README.md](./specs/README.md) 为准。

