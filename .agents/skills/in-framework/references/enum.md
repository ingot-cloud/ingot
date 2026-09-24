# 业务枚举

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
