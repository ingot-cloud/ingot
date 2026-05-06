package com.ingot.framework.eventbus.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ingot.framework.eventbus.EventType;

/**
 * 解析事件类上的 {@link EventType} 注解并缓存结果。
 *
 * @author jy
 * @since 2026/4/27
 */
public final class EventTypeResolver {

    private static final Map<Class<?>, String> CACHE = new ConcurrentHashMap<>();

    private EventTypeResolver() {
    }

    /**
     * 解析事件类的类型字符串。
     *
     * @param eventClass 事件类
     * @return {@link EventType#value()}
     */
    public static String resolve(Class<?> eventClass) {
        return CACHE.computeIfAbsent(eventClass, c -> {
            EventType annotation = c.getAnnotation(EventType.class);
            if (annotation == null) {
                throw new IllegalStateException(
                        "Event class " + c.getName() + " missing @EventType annotation");
            }
            String value = annotation.value();
            if (value == null || value.isBlank()) {
                throw new IllegalStateException(
                        "Event class " + c.getName() + " has empty @EventType value");
            }
            return value;
        });
    }

    /**
     * 拼接 channel 名：{@code <topic-prefix>:<event-type>}。
     */
    public static String channel(String topicPrefix, Class<?> eventClass) {
        return topicPrefix + ":" + resolve(eventClass);
    }
}
