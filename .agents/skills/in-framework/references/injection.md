# 构造注入

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
