package com.ingot.framework.security.config.annotation.web.configuration;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * <p>Description  : EnableIngotAuthorizationServer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/14.</p>
 * <p>Time         : 10:50 上午.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableIngotWebSecurity
@Import({IngotOAuth2AuthorizationServerConfiguration.class})
public @interface EnableIngotAuthorizationServer {
}
