package com.ingot.framework.security.config.annotation.web.configuration;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.ingot.framework.security.oauth2.core.IngotOAuth2ResourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * <p>Description  : EnableIngotWebSecurity.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/14.</p>
 * <p>Time         : 3:34 下午.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableWebSecurity
@EnableConfigurationProperties(IngotOAuth2ResourceProperties.class)
@Import({IngotOAuth2ResourceServerConfiguration.class,
        IngotOAuth2ResourceServerJwtConfiguration.class,
        PasswordEncoderConfiguration.class})
public @interface EnableIngotWebSecurity {
}
