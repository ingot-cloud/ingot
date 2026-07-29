package com.ingot.cloud.security.api.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 绑定 {@link SecurityEventProperties}。
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SecurityEventProperties.class)
public class SecurityEventPropertiesAutoConfiguration {
}
