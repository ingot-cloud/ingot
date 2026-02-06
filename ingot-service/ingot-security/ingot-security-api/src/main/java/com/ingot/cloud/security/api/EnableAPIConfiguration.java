package com.ingot.cloud.security.api;

import feign.Feign;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * 启用 Credential Security API 配置
 *
 * @author jymot
 * @since 2026-01-22
 */
@AutoConfiguration
@EnableFeignClients
@ConditionalOnClass(Feign.class)
public class EnableAPIConfiguration {
}
