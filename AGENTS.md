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

# 魔法值与配置取值

业务语义不得在调用点裸写字符串或数字。这是编码门禁，不必等用户提醒。规则如下：

1. 至少抽常量：同一字面量出现在条件、装配、YAML 绑定或跨类比较时，必须有命名常量，禁止复制 `"local"`、`"remote"` 这类散落字面量。
2. 封闭且有语义的取值用枚举：策略来源、投递目标、状态机、维度等固定集合用枚举，而不是平行的字符串常量。跨模块复用的枚举放 `ingot-commons`（或该语义已有的基础包），禁止每个属性类再复制一份内部 `Mode`。
3. 注解只能用编译期常量：`@ConditionalOnProperty(havingValue = ...)` 等注解属性不能写枚举本身，把 YAML 字面量做成枚举上的 `public static final String`（如 `PolicySourceMode.VALUE_LOCAL`）。
4. 语义不同不要合成一个枚举：例如策略来源 `local/remote` 与事件投递 `local/center` 是两套词汇。各域自有 mode（字典客户端 `AUTO/NONE`、发号器 `redis/machine`）不强行统一。
5. 新增配置项时，属性字段用枚举或常量类型，而不是 `String` 承载封闭取值。

# 代码注释规范

Java 注释统一遵循 [Javadoc 规范](./docs/standards/Javadoc.md)，可借助 `.agents/skills/java-class-javadoc` skill 生成、补全或审查。与本节冲突时以该文件为准。这是编码门禁，新增或修改对外契约时必须一并写上，不必等用户说「补注释」。

必须注释的范围：

1. 所有类型：`class`、`interface`、`enum`、`record`、`@interface` 及嵌套类型。
2. 对外方法：接口方法、供其他模块或调用方使用的 `public` 方法（含抽象方法）。
3. 对外字段：`@ConfigurationProperties` 绑定字段、会被其他模块 get/set 的 field。Lombok 生成的 getter/setter 不写注释，注释写在 field 上。

不强制注释：`private` 实现、包内 helper、与类型注释重复的成员列表、语义已由枚举名表达清楚的常量（仍建议给非显而易见的枚举常量补一句）。

规则如下：

1. 动笔前先读 `docs/standards/Javadoc.md`。类型注释结构：首段用一个 `<p>...</p>` 一句话说明职责；仅当设计意图、场景、边界或约束确有帮助时才追加第二段；能从可见 API 推导出正确示例时用 `<pre>{@code ... }</pre>` 给出。
2. 方法注释写清做什么、关键参数、返回值与失败语义，不要复述方法名。字段注释写清含义、默认值、合法取值或 YAML 语义。
3. 类型标签顺序：`@author`、`@since`、可选 `@see`、可选 `@apiNote`、可选 `@implNote`。方法按需使用 `@param`、`@return`、`@throws`。禁止占位符与空标签。
4. 作者与版本优先级：① 用户/当前任务显式指定的 `@author`/`@since`；② 更新既有注释时保留其非空值；③ 新建且无显式值时用 `@author jy`、`@since 1.0.0`。不得从 Git 历史、邻近文件或当前日期推断，除非用户明确要求。
5. 真实性：所有 `{@link}`/`@see`/示例必须对应真实符号并与实现或公共契约一致；只修正错误、过时、冗余或不符合规范之处。
6. 落盘时机：新增或修改上述对外契约时，注释作为实现的一部分写入源码。用户明确要求「只审查、先不要改文件」时，只给建议不改源码。补历史代码的专项任务按用户指定范围改。

# Git 提交规范

本仓库提交默认遵循 [Conventional Commits 1.0.0](https://www.conventionalcommits.org/zh-hans/v1.0.0/)。创建或改写提交信息时必须使用 `.agents/skills/conventional-commits` skill。这是提交门禁，不必等用户提醒格式。与本节冲突时以该规范和 skill 为准。

规则如下：

1. 标题：`<type>[optional scope][!]: <description>`。`type` 小写；冒号为英文半角，后面一个空格。
2. 允许的 type：`feat`、`fix`、`docs`、`style`、`refactor`、`perf`、`test`、`build`、`ci`、`chore`、`revert`。新功能用 `feat`，修 bug 用 `fix`，禁止自造类型。
3. `description` 用中文，写清意图（为什么），不要罗列文件或写「功能提交」；标题不加句号。
4. scope 可选：仅当变更明确落在单一模块或域时使用，小写名词，如 `oss`、`cache`、`security`。不要用 PascalCase。
5. 破坏性变更必须在 type/scope 后加 `!`，或在 footer 写 `BREAKING CHANGE: `；需要迁移说明时两者都写。
6. 标题不足以说明动机、风险或迁移时再写 body；`Co-authored-by` 等 git trailer 放 footer。
7. 未明确要求提交时不要创建 commit。

# 缓存接入规范

远端拉取的只读参考数据（策略、配置、字典、租户参数等）一律接入统一分层缓存框架 `ingot-framework/ingot-cache`，不再手写 L1/L2/降级逻辑，可借助 `.agents/skills/layered-cache` skill 完成接入或审查。规则如下：

1. 适用范围：读多写少、来自远端、故障时不能 fail-open 的参考数据。实体 CRUD 的 `@Cacheable` 缓存由 `InRedisCacheManager` 承担，不在本框架职责内。
2. 装配方式：用 `LayeredCacheBuilder` 组合，层次顺序固定为 `L1 → 刷新通知 → L2 → Resilient → loader`，可选层缺省即跳过。
3. 不可违反的语义：Resilient 位于 L1/L2 之下；LKG 独立 key、无 TTL、不随失效清除；不缓存空值；远端不可用与合法空严格区分；地板 fail-closed；广播方自行清本地缓存。
4. 配置键归属消费模块，框架只接收映射后的 `LayeredCacheSettings`，不得为兼容框架而改动模块已上线的配置键。
5. 编译产物（`Pattern`、`PathPattern`、预建索引等）不进 L2，改用 `VersionedDerivedCache`，失效键必须是 `(source, version)` 二元组而非版本号本身。
6. 详细契约与迁移记录以 [specs/current/framework/layered-cache/](./specs/current/framework/layered-cache/) 为准。
