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

# 代码注释规范

Java 类型级注释（`class`、`interface`、`enum`、`record`、`@interface` 及嵌套类型）统一遵循 [Javadoc 规范](./docs/standards/Javadoc.md)，可借助 `.agents/skills/java-class-javadoc` skill 生成、补全或审查。规则如下：

1. 以标准为准：动笔前先读 `docs/standards/Javadoc.md`，与本节冲突时以该文件为准；仅处理类型级注释，不改动方法、构造器、字段、枚举常量与包注释（除非任务明确要求）。
2. 结构：首段用一个 `<p>...</p>` 一句话说明该类型职责；仅当设计意图、核心职责、使用场景、边界或约束确有帮助时才追加第二段；能从可见 API 推导出正确示例时用 `<pre>{@code ... }</pre>` 给出。
3. 标签顺序：`@author`、`@since`、可选 `@see`、可选 `@apiNote`（面向调用者的限制/性能/生命周期）、可选 `@implNote`（实现原理/不变量/取舍/扩展）；代码不支持的内容一律省略，禁止占位符与空标签。
4. 作者与版本优先级：① 使用用户/当前任务显式指定的 `@author`/`@since`；② 更新既有注释时保留其非空 `@author`/`@since`；③ 新建且无显式值时用 `@author jy`、`@since 1.0.0`。不得从 Git 历史、邻近文件或当前日期推断，除非用户明确要求。
5. 真实性：所有 `{@link}`/`@see`/示例必须对应真实符号并与实现或公共契约一致；保留仍然有效的既有信息，仅修正错误、过时、冗余或不符合规范之处。
6. 落盘时机：仅在用户明确要求"应用/修正注释"时改写源码；否则只给出建议注释或审查结论。

