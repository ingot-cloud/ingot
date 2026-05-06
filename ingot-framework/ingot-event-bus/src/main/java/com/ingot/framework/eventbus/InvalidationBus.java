package com.ingot.framework.eventbus;

import java.util.function.Consumer;

/**
 * 失效广播总线 SPI。默认实现基于 Redis Pub/Sub，可替换为 Kafka 等其它消息中间件。
 * <p>
 * channel 名由 {@link EventType} 注解推导：{@code <topic-prefix>:<event-type>}，
 * 业务方仅按事件类发布与订阅，不直接拼接 channel 字符串。
 * </p>
 *
 * @author jy
 * @since 2026/4/27
 */
public interface InvalidationBus {

    /**
     * 发布事件。bus 内部根据 {@link EventType} 注解推导 channel，并自动注入 {@code origin}/{@code timestamp}。
     *
     * @param event 事件实例（事件类必须标注 {@link EventType}）
     * @param <E>   事件类型
     */
    <E extends InvalidationEvent> void publish(E event);

    /**
     * 订阅指定事件类型的失效广播。
     *
     * @param eventType 事件类（必须标注 {@link EventType}）
     * @param handler   事件处理器；本节点自身发布的事件将被自动过滤
     * @param <E>       事件类型
     * @return 取消订阅句柄
     */
    <E extends InvalidationEvent> Subscription subscribe(Class<E> eventType, Consumer<E> handler);
}
