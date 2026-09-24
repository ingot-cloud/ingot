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

写后端业务代码时阅读 [in-framework](./.agents/skills/in-framework/SKILL.md)。枚举、时间、OSS、魔法值、JavaDoc、构造注入的细则按该 skill 索引读 [references/](./.agents/skills/in-framework/references/)，不必等用户提醒。

# 业务枚举契约

新增或修改业务枚举时，形态对齐 `CommonStatusEnum`：`@Getter`、`@RequiredArgsConstructor`，稳定字面量同时标 `@JsonValue`、`@EnumValue`，`@JsonCreator getEnum` 走 `EnumUtils` 索引。禁止每个枚举手写 HashMap。这是编码门禁。

# 对象存储字段

头像、附件等对象字段入库只保存 `bucket/objectName`，响应用 `@OssUrl` 签发时效链接。这是编码门禁。

# 接口时间

接口墙钟时间（响应创建/更新时间、后台配置时间、前端提交时间）按本次请求的前端当地时间解释和返回；识别不到时区时用 `Asia/Shanghai`。数据库默认存 UTC。定时任务触发、过期与授权截止等瞬时判断走 `Instant` 或同一套 UTC 时钟，不跟请求时区走。这是编码门禁。

# 魔法值与配置取值

业务语义不得在调用点裸写字符串或数字。这是编码门禁。

1. 至少抽常量：同一字面量出现在条件、装配、YAML 绑定或跨类比较时，必须有命名常量。
2. 封闭且有语义的取值用枚举，跨模块复用放 `ingot-commons`（或该语义已有的基础包）。
3. 注解只能用编译期常量；YAML 字面量做成枚举上的 `public static final String`。
4. 语义不同不要合成一个枚举。
5. 新增配置项用枚举或常量类型，不用 `String` 承载封闭取值。

# 代码注释规范

Java 注释统一遵循 [Javadoc 规范](./docs/standards/Javadoc.md)。这是编码门禁，新增或修改对外契约时必须一并写上。

必须注释：所有类型；对外方法；`@ConfigurationProperties` 与会被其他模块 get/set 的字段（注释写在 field 上）。不强制注释 private 实现、包内 helper。新建且无显式值时用 `@author jy`、`@since 1.0.0`；更新既有注释时保留其非空作者与版本。

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

# Spring Bean 依赖注入规范

新增或修改 Bean 依赖时，默认使用 `private final` 字段及 `@RequiredArgsConstructor` 构造注入；不新增自有字段或 Setter 注入。唯一构造器不加 `@Autowired`。这是编码门禁。

限定 Bean、延迟依赖、父类构造及必要初始化须保留语义；Lombok 无法可靠表达时允许明确说明原因的显式构造器。不把必需依赖改成可选，不使用强制无参构造器或服务定位器规避注入。`@Bean` 参数、测试框架与第三方内部注入不纳入此门禁。仅清理当前任务范围，不自动扩展为全仓库重构。
