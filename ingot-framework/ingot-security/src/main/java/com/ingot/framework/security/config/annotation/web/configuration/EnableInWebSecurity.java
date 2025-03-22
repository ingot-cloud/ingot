package com.ingot.framework.security.config.annotation.web.configuration;

import com.ingot.framework.security.core.InSecurityProperties;
import com.ingot.framework.security.oauth2.core.InOAuth2ResourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * <p>Description  : 开启安全注解.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/14.</p>
 * <p>Time         : 3:34 下午.</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@EnableConfigurationProperties({InSecurityProperties.class, InOAuth2ResourceProperties.class})
@Import({InOAuth2ResourceServerConfiguration.class,
        CustomOAuth2ResourceServerJwtConfiguration.class,
        PasswordEncoderConfiguration.class})
public @interface EnableInWebSecurity {
}
