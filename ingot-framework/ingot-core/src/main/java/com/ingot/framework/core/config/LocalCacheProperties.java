package com.ingot.framework.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * LocalCacheProperties
 *
 * @author jy
 * @since 2026/2/11
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.cache.local")
public class LocalCacheProperties {
    /**
     * Maximum size of the cache.
     */
    private long maximumSize = 1000;
    /**
     * Expire after write. unit minutes.
     */
    private long expireAfterWrite = -1;
}
