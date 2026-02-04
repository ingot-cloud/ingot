package com.ingot.framework.security.credential.data.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * CredentialDataAutoConfiguration
 *
 * @author jy
 * @since 2026/2/4
 */
@AutoConfiguration
@MapperScan("com.ingot.framework.security.credential.data.mapper")
@ComponentScan("com.ingot.framework.security.credential.data.service")
public class CredentialDataAutoConfiguration {
}
