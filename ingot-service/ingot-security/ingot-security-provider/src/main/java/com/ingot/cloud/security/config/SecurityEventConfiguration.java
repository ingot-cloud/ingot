package com.ingot.cloud.security.config;

import com.ingot.cloud.security.api.config.SecurityEventProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 统一安全事件相关配置。
 *
 * @author jy
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(SecurityEventProperties.class)
public class SecurityEventConfiguration {
}
