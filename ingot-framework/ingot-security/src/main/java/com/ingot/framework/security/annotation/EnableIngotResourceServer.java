package com.ingot.framework.security.annotation;

import com.ingot.framework.security.config.AuthorizeConfig;
import com.ingot.framework.security.provider.IngotSecurityBeanDefinitionRegistrar;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;

import java.lang.annotation.*;

/**
 * <p>Description  : EnableIngotResourceServer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/19.</p>
 * <p>Time         : 下午1:47.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@EnableResourceServer
@EnableGlobalMethodSecurity(prePostEnabled = true)
@Import({IngotSecurityBeanDefinitionRegistrar.class, AuthorizeConfig.class})
public @interface EnableIngotResourceServer {
}
