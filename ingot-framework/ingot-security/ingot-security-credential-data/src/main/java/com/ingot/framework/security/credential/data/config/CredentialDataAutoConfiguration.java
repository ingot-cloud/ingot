package com.ingot.framework.security.credential.data.config;

import com.ingot.framework.security.credential.data.mapper.MapperModule;
import com.ingot.framework.security.credential.data.service.ServiceModule;
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
@MapperScan(basePackageClasses = MapperModule.class)
@ComponentScan(basePackageClasses = ServiceModule.class)
public class CredentialDataAutoConfiguration {
}
