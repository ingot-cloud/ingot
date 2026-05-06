package com.ingot.framework.eventbus.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 事件总线配置项。
 *
 * @author jy
 * @since 2026/4/27
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.event-bus")
public class EventBusProperties {

    /**
     * 总线类型。{@link Type#REDIS} 默认开启 Redis Pub/Sub 实现；{@link Type#NONE} 禁用自动注册。
     */
    private Type type = Type.REDIS;

    /**
     * 节点标识，用于回环过滤；为空时自动生成 {@code ${spring.application.name}:${UUID}}。
     */
    private String origin;

    /**
     * Redis 实现配置。
     */
    private Redis redis = new Redis();

    public enum Type {
        REDIS, NONE
    }

    @Getter
    @Setter
    public static class Redis {

        /**
         * channel 名前缀，最终 channel = {@code <topic-prefix>:<event-type>}。
         */
        private String topicPrefix = "in:bus";
    }
}
