package com.ingot.framework.social.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : 社交配置属性.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 14:00.</p>
 */
@Data
@ConfigurationProperties(prefix = "ingot.social")
public class SocialConfigProperties {

    /**
     * Redis配置
     */
    private RedisConfig redis = new RedisConfig();

    @Data
    public static class RedisConfig {
        /**
         * Redis频道主题
         */
        private String topic = "in:social:config:changed";
    }

}

