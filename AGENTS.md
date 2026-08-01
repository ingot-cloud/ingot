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

# 缓存接入规范

远端拉取的只读参考数据（策略、配置、字典、租户参数等）一律接入统一分层缓存框架 `ingot-framework/ingot-cache`，不再手写 L1/L2/降级逻辑，可借助 `.agents/skills/layered-cache` skill 完成接入或审查。规则如下：

1. 适用范围：读多写少、来自远端、故障时不能 fail-open 的参考数据。实体 CRUD 的 `@Cacheable` 缓存由 `InRedisCacheManager` 承担，不在本框架职责内。
2. 装配方式：用 `LayeredCacheBuilder` 组合，层次顺序固定为 `L1 → 刷新通知 → L2 → Resilient → loader`，可选层缺省即跳过。
3. 不可违反的语义：Resilient 位于 L1/L2 之下；LKG 独立 key、无 TTL、不随失效清除；不缓存空值；远端不可用与合法空严格区分；地板 fail-closed；广播方自行清本地缓存。
4. 配置键归属消费模块，框架只接收映射后的 `LayeredCacheSettings`，不得为兼容框架而改动模块已上线的配置键。
5. 编译产物（`Pattern`、`PathPattern`、预建索引等）不进 L2，改用 `VersionedDerivedCache`，失效键必须是 `(source, version)` 二元组而非版本号本身。
6. 详细契约与迁移记录以 [specs/current/framework/layered-cache/](./specs/current/framework/layered-cache/) 为准；该基线建立前参考 [active change DESIGN](./specs/changes/active/20260730-framework-layered-cache/DESIGN.md)。

