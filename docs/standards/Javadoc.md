# Javadoc规范

类型、对外方法与对外字段的注释都必须写。细则如下；与 `AGENTS.md` 冲突时以本文件为准。

### 必须注释

- 类型：`class`、`interface`、`enum`、`record`、`@interface` 及嵌套类型。
- 对外方法：接口方法、供其他模块使用的 `public` 方法。
- 对外字段：`@ConfigurationProperties` 绑定字段、会被其他模块 get/set 的 field。Lombok 生成的 getter/setter 不写注释，注释写在 field 上。

### 不强制注释

- `private` 方法、字段与纯内部 helper。
- 与类型首段重复的成员清单。
- 名称已自解释的枚举常量（非显而易见的仍应补一句）。

### 类注释

```
/**
 * <p>类功能简述（一句话说明这个类是干什么的）</p>
 *
 * <p>详细说明（可选）：
 * 描述设计意图、核心职责、使用场景、注意事项等。
 * 如果是核心组件，这里建议写清楚边界和约束。</p>
 *
 * <h3>使用示例：</h3>
 * <pre>{@code
 * XxxService service = new XxxService();
 * service.doSomething();
 * }</pre>
 *
 * @author jy
 * @since 1.0.0
 *
 * @see XxxInterface
 * @see AnotherClass
 *
 * @apiNote （可选）对“使用者”的说明，比如使用限制、性能提示等
 * @implNote （可选）对“实现者”的说明，比如实现原理、设计权衡
 */
```

- 首段必须是一个 `<p>...</p>` 的完整句子，说明职责，不要只重复类型名。
- 第二段、示例、`@see` / `@apiNote` / `@implNote` 仅在确有帮助时出现；禁止「详细说明」、空标签或编造的链接。
- 标签顺序：`@author`、`@since`、可选 `@see`、可选 `@apiNote`、可选 `@implNote`。
- `@author` / `@since`：优先用任务指定值；更新时保留既有非空值；新建默认 `@author jy`、`@since 1.0.0`。不要从 Git 历史或当前日期推断。

### 方法注释

```
/**
 * 一句话说明该方法做什么。
 *
 * @param name 参数含义与约束（合法范围、是否可空）
 * @return 返回值含义；无返回值时省略
 * @throws IllegalArgumentException 何时抛出
 */
```

- 接口方法与对外 `public` 方法必须有。内部 `private` 方法不强制。
- 写行为、约束和失败语义，不要复述方法名（禁止「获取 xxx」「设置 xxx」而无额外信息）。
- `@param` / `@return` / `@throws` 只在对应元素真实存在且注释能提供类型签名读不出的信息时使用。
- 不要给 Lombok 生成的 getter/setter 写方法注释。

### 字段注释

```
/**
 * 字段含义；默认值与合法取值（若有）。
 * <p>配置项补充 YAML 语义或与其它字段的关系。</p>
 */
```

- `@ConfigurationProperties` 字段、跨模块可见的 field 必须有。
- 配置字段写清：含义、默认值、合法取值（可 `{@link EnumType#CONST}`）、关闭或切模式后的行为。
- 同一信息不要在类型注释里再抄一遍字段列表。
