package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.core.IngotSecurityProperties;
import com.ingot.framework.security.oauth2.core.IngotOAuth2ResourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

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
@EnableConfigurationProperties({IngotSecurityProperties.class, IngotOAuth2ResourceProperties.class})
@Import({IngotOAuth2ResourceServerConfiguration.class,
        IngotOAuth2ResourceServerJwtConfiguration.class,
        PasswordEncoderConfiguration.class})
public @interface EnableIngotWebSecurity {
}
