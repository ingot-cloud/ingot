package com.ingot.framework.security.config.annotation.web.configuration;

import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

import java.lang.annotation.*;

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
@Import({IngotOAuth2ResourceServerConfiguration.class})
public @interface EnableIngotWebSecurity {
}
