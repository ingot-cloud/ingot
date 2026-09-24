# Ingot 框架编码细则

按主题阅读对应章节。JavaDoc 真源是仓库 [docs/standards/Javadoc.md](../../../docs/standards/Javadoc.md)。

## 接口时间

接口上的墙钟时间（响应里的创建/更新时间、后台配置时间、前端提交的时间）按**本次请求的前端当地时间**解释和返回。数据库默认存 UTC。定时任务触发、过期/锁定/有效期是否到期、授权截止这类瞬时判断不跟请求时区走。

### 接口

- 格式继续用 `yyyy-MM-dd HH:mm:ss`（`InJavaTimeModule`），字符串本身不带偏移。
- 序列化、反序列化都按本次请求能识别出的客户端时区解释。
- 识别不到时区时用 `Asia/Shanghai`。禁止写 `Asia/Beijing`（该 ID 无效，`TimeZone.getTimeZone` 会落到 GMT）。
- 不要把库存 UTC 墙钟直接当本地时间返回，也不要把前端当地时间当 UTC 入库。进出 API 时在边界转换。

### 库存

- `LocalDateTime` 字段默认表示 UTC。写入用 `LocalDateTime.now(ZoneOffset.UTC)` 或 `LocalDateTime.ofInstant(instant, ZoneOffset.UTC)`。
- JDBC 会话时区与库存口径对齐为 UTC；不要在连接串里用 `serverTimezone=Asia/Shanghai` 把 UTC 墙钟再偏一次。

### 判断与定时

- 比较「现在是否已过某时刻」用 `Instant`，或把两边都换到同一套 UTC 时钟再比。
- 定时任务的触发时刻按任务配置的时区（或系统调度时钟）执行，不要用当前请求的前端时区去算下一次触发。
- 用户时区只影响接口展示和用户提交的墙钟，不影响任务是否该跑、锁是否该解、授权是否已过期。

### 不要做的事

- 不要在业务代码里写 `LocalDateTime.now()`（系统默认时区）再拿去入库或跟 UTC 字段比较。
- 不要为了展示方便把库里的 UTC `LocalDateTime` 原样塞进响应。
- 不要在 Jackson 全局 `timeZone` 上写死一个与请求无关的北京/上海时区后，就认为接口时间已经正确；请求有客户端时区时以前端为准。

## 业务枚举

新增或修改业务枚举时按本规范落盘。形态对齐 `CommonStatusEnum`，查找对齐 `IamAction` 的索引，不要每个枚举再手写 `HashMap` 循环。

```java
@Getter
@RequiredArgsConstructor
public enum MemberStatus {
    ACTIVE("ACTIVE"),
    SUSPENDED("SUSPENDED"),
    REMOVED("REMOVED");

    /** JSON 与数据库使用的稳定字面量。 */
    @JsonValue
    @EnumValue
    private final String value;

    private static final Map<String, MemberStatus> BY_VALUE =
            EnumUtils.index(values(), MemberStatus::getValue);

    @JsonCreator
    public static MemberStatus getEnum(String value) {
        return EnumUtils.require(BY_VALUE, value);
    }
}
```

示例只展示形态；实际源码同时遵循仓库 JavaDoc 规范。参考 `com.ingot.framework.commons.model.iam.MemberStatus`、`IamAction`。

1. 读目标枚举、调用点、JSON 夹具和实体字段，确认稳定字面量是枚举名还是独立编码。
2. 类上使用 `@Getter`、`@RequiredArgsConstructor`。稳定字面量字段同时标 `@JsonValue`、`@EnumValue`。
3. 用 `EnumUtils.index(values(), Type::getValue)` 建索引；`@JsonCreator public static Type getEnum(String)` 调用 `EnumUtils.require`。禁止再复制 `valueMap` 静态块。
4. 名称即值时，构造参数写与常量名相同的字符串。字面量与常量名不同时（ACTION 码、错误码、字段键）才加 `public static final String VALUE_*`，供注解等编译期常量使用。
5. 编译并跑相关契约测试；至少覆盖 JSON 往返、未知字面量拒绝、`CompositeEnumTypeHandler` 可构造。

- 默认字段名 `value`，访问器 `getValue()`。领域已有固定名称时保留：`IamAction.code`、`IamReasonCode.code`，索引名用 `BY_CODE`，其余用 `BY_VALUE`。
- `getEnum(null)` 返回 `null`；未知非空值抛 `IllegalArgumentException`，Jackson 转为映射失败。不要静默把未知词汇收成 `null`。
- 可选查找用 `EnumUtils.get`。索引在类加载时拒绝重复字面量。
- 不要为 Lombok getter 再写同义 `code()` / `key()`；调用点用 `getCode()` / `getValue()`。
- 不要实现 `IEnum` 来绕过 `@EnumValue`。Nacos `default-enum-type-handler` 是 `CompositeEnumTypeHandler`，无 `@EnumValue` 的枚举会递归崩溃。不要改全局默认处理器。
- 不要把策略来源、投递目标、状态机等不同词汇合成一个枚举。
- 不要在调用点裸写稳定字面量；比较、装配、YAML 绑定走枚举常量或 `VALUE_*`。
- 不要把本次范围扩成全仓库历史枚举重构。`CommonStatusEnum` 等旧枚举保持原查找，直到专门迁移。

## 魔法值与配置取值

业务语义不得在调用点裸写字符串或数字。

1. 至少抽常量：同一字面量出现在条件、装配、YAML 绑定或跨类比较时，必须有命名常量，禁止复制 `"local"`、`"remote"` 这类散落字面量。
2. 封闭且有语义的取值用枚举：策略来源、投递目标、状态机、维度等固定集合用枚举，而不是平行的字符串常量。跨模块复用的枚举放 `ingot-commons`（或该语义已有的基础包），禁止每个属性类再复制一份内部 `Mode`。
3. 注解只能用编译期常量：`@ConditionalOnProperty(havingValue = ...)` 等注解属性不能写枚举本身，把 YAML 字面量做成枚举上的 `public static final String`（如 `PolicySourceMode.VALUE_LOCAL`）。
4. 语义不同不要合成一个枚举：例如策略来源 `local/remote` 与事件投递 `local/center` 是两套词汇。各域自有类型（字典客户端 `AUTO/NONE`、发号器 `redis/machine`）不强行统一。
5. 新增配置项时，属性字段用枚举或常量类型，而不是 `String` 承载封闭取值。

## OSS

头像、附件、图标等对象字段入库只保存相对路径，响应再签发时效 URL。不要在库里持久化带 host 的预签名链接，也不要在前端自己拼对象地址。

写入：

1. 客户端可以提交预签名 URL 或 `bucket/objectName`。
2. 入库前规范成 `bucket/objectName`：IAM 写路径用 `IamOssPaths.store(...)`；请求 DTO 字段标 `@OssSaveUrl`（`com.ingot.framework.oss.common.OssSaveUrl`），由 `OssSaveUrlDeserializer` 还原路径。
3. DTO 在 `ingot-commons`、不能依赖 OSS 模块时，在消费模块用 Jackson mixin 挂 `@OssSaveUrl`，参考 `IamOssJacksonConfiguration.SaveAvatar`。
4. 禁止把带 query 的时效链接原样写入数据库。

读取：

1. 库存值是 `bucket/objectName`（可空）。
2. 响应字段标 `@OssUrl`（`com.ingot.framework.oss.common.OssUrl`），由 `OssUrlSerializer` 调用 `OssService.getObjectURL` 签发时效链接。
3. commons DTO 同样用 mixin 挂 `@OssUrl`，参考 `IamOssJacksonConfiguration.ReadAvatar`。
4. 签发失败时序列化器会回退原值；业务代码不要再手写一层 URL 拼接。

前端把接口返回的时效 URL 直接交给 `in-avatar` / `in-detail-identity` / `<img>`。上传成功后把服务返回的 `url` 写入表单，提交时原样回传。对象键用业务目录 + 文件名。不要根据相对路径拼 host，也不要把过期链接缓存成长期地址。

- 新字段同时覆盖写路径（`store` 或 `@OssSaveUrl`）和读路径（`@OssUrl` 或 mixin）。
- 创建接口如果接受头像，初始化插入必须写入规范化后的路径，不要只在 PATCH 里落库。
- 补一条往返测试：请求预签名 URL → 库存 `bucket/objectName`；响应库存路径 → 时效 URL。

## 构造注入

本仓库新增或修改 Spring Bean 依赖时，默认使用 `private final` 字段和类上的 `@RequiredArgsConstructor`。仅处理当前任务范围；不要因此清理无关历史代码。

```java
@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository members;
    private final AuthorizationService authorization;
}
```

示例只展示装配形态；实际源码同时遵循仓库 JavaDoc 规范。

1. 阅读目标类、构造器、父类、Bean 定义和调用点，区分普通依赖、可选依赖、参数限定和测试便利构造器。
2. 普通必需依赖改成 final 字段，用 `@RequiredArgsConstructor` 替换只做赋值的构造器。唯一构造器不加 `@Autowired`。
3. 本次范围内的自有字段/Setter 注入（`@Autowired`、`@Resource`、`@Inject`）改为构造注入；不要遗漏 `@Resource` 按名称选择 Bean 的语义。
4. 迁移测试构造方式到测试夹具，不为测试便利新增多个生产注入入口。继承链需要同步修改子类的父构造调用。
5. 编译并按任务验证 Spring 装配；至少检查限定 Bean、多候选、可选依赖与延迟获取时机。

- 不假定 Lombok 会复制 `@Qualifier` 或参数级 `@Lazy`。需要这些语义且没有已验证的复制配置时保留带参数注解的显式构造器；用短注释说明原因，不在整个仓库新增全局 Lombok 配置。
- 必须调用父类带参构造器、或确有必要初始化逻辑时允许显式构造器，不机械添加会生成冲突构造器的注解。
- 多构造器场景中构造器上的 `@Autowired` 是选择入口，不是字段注入；先消除歧义再删除注解，必要时保留明确入口。
- 原本可选/延迟的依赖可以通过 final `ObjectProvider<T>` 注入，保持原有选取和获取时机；不能把必需 Bean 改为可选或把懒加载改成构造时获取。
- 禁止用 `@NoArgsConstructor(force = true)`、可变 Setter 或 ApplicationContext 服务定位替代构造注入，禁止用新增 `@Lazy` 掩盖循环依赖。
- `@Bean` 方法参数注入、测试框架注入及第三方框架内部注入不是违规，不为统一外观改写。
- 修改只服务于当前授权范围；合理显式构造器例外无需额外向用户索要确认。

## JavaDoc

动笔前先读 `docs/standards/Javadoc.md`，与本节冲突时以该文件为准。

1. 读完整目标类型及足够上下文，确认职责、协作者、生命周期、约束和调用方。
2. 必须注释：类型（`class` / `interface` / `enum` / `record` / `@interface` 及嵌套类型）；对外方法（接口方法、供其他模块使用的 `public` 方法）；对外字段（`@ConfigurationProperties`、会被其他模块 get/set 的 field）。不注释 private helper、Lombok getter/setter（注释写在 field 上）。
3. 按代码事实写简洁中文 JavaDoc，写职责和边界，不要复述名字或罗列成员。
4. 校验每个链接、示例、标签和断言后再落盘。新增或修改对外契约时注释一并写入；用户只要审查时只给建议不改源码。

类型：首段一个 `<p>...</p>` 一句话说明职责；仅当设计意图、场景、边界或约束确有帮助时再写第二段；能从可见 API 推导出正确示例时用 `<pre>{@code ... }</pre>`。标签顺序：`@author`、`@since`、可选 `@see`、可选 `@apiNote`、可选 `@implNote`。JavaDoc 紧挨类型注解之前。

方法写做什么、约束和失败语义，不要复述方法名。`@param` / `@return` / `@throws` 只在签名看不出来时写。字段写含义、默认值和合法取值；配置字段补 YAML 或模式语义。不要占位符或空标签。保留仍然正确的既有信息。

作者与版本：① 用户或当前任务显式指定的 `@author` / `@since`；② 更新既有注释时保留非空值；③ 新建且无显式值时用 `@author jy`、`@since 1.0.0`。不得从 Git 历史、邻近文件或当前日期推断。
