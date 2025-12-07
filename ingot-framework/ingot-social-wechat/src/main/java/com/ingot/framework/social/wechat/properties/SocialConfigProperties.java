package com.ingot.framework.social.wechat.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : 社交配置属性.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 10:10.</p>
 */
@Data
@ConfigurationProperties(prefix = "ingot.social")
public class SocialConfigProperties {

    /**
     * 消息队列类型: redis, kafka
     */
    private MessageQueueType messageQueue = MessageQueueType.REDIS;

    /**
     * Redis配置
     */
    private RedisConfig redis = new RedisConfig();

    /**
     * Kafka配置
     */
    private KafkaConfig kafka = new KafkaConfig();

    @Data
    public static class RedisConfig {
        /**
         * Redis频道主题
         */
        private String topic = "in:social:config:changed";
    }

    @Data
    public static class KafkaConfig {
        /**
         * Kafka主题
         */
        private String topic = "in-social-config-changed";

        /**
         * Kafka消费组
         */
        private String groupId = "in-social-config-group";
    }

    /**
     * 消息队列类型枚举
     */
    public enum MessageQueueType {
        /**
         * Redis Pub/Sub
         */
        REDIS,

        /**
         * Kafka
         */
        KAFKA
    }
}

