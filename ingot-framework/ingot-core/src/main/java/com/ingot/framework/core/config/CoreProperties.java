package com.ingot.framework.core.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * <p>Description  : CoreProperties.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/4/3.</p>
 * <p>Time         : 15:39.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "ingot.core")
public class CoreProperties {
    /**
     * 请求日志
     */
    private Boolean requestLog = true;
}
