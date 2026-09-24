# 魔法值与配置取值

业务语义不得在调用点裸写字符串或数字。

1. 至少抽常量：同一字面量出现在条件、装配、YAML 绑定或跨类比较时，必须有命名常量，禁止复制 `"local"`、`"remote"` 这类散落字面量。
2. 封闭且有语义的取值用枚举：策略来源、投递目标、状态机、维度等固定集合用枚举，而不是平行的字符串常量。跨模块复用的枚举放 `ingot-commons`（或该语义已有的基础包），禁止每个属性类再复制一份内部 `Mode`。
3. 注解只能用编译期常量：`@ConditionalOnProperty(havingValue = ...)` 等注解属性不能写枚举本身，把 YAML 字面量做成枚举上的 `public static final String`（如 `PolicySourceMode.VALUE_LOCAL`）。
4. 语义不同不要合成一个枚举：例如策略来源 `local/remote` 与事件投递 `local/center` 是两套词汇。各域自有类型（字典客户端 `AUTO/NONE`、发号器 `redis/machine`）不强行统一。
5. 新增配置项时，属性字段用枚举或常量类型，而不是 `String` 承载封闭取值。
