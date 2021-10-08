package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.oauth2.core.IngotOAuth2AuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@EnableConfigurationProperties(IngotOAuth2AuthProperties.class)
@Import({IngotOAuth2AuthorizationServerConfiguration.class})
public @interface EnableIngotAuthorizationServer {
}
