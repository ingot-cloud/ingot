# Javadoc规范

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