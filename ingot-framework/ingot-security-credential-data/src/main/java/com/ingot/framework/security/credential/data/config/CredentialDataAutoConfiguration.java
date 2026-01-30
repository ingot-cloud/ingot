package com.ingot.framework.security.credential.data.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * 描组件和 Mapper，其他配置由依赖的模块提供
 *
 * @author jymot
 * @since 2026-01-24
 */
@Configuration
@ComponentScan("com.ingot.framework.security.credential.data")
public class CredentialDataAutoConfiguration {
}
