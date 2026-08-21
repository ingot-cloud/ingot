package com.ingot.cloud.auth.config;

import com.ingot.framework.security.oauth2.core.InOAuth2AuthProperties;
import com.ingot.framework.security.web.authentication.InLoginUrlAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;

/**
 * <p>Description  : ExceptionHandlingCustomizer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/25.</p>
 * <p>Time         : 10:33 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class ExceptionHandlingCustomizer implements Customizer<ExceptionHandlingConfigurer<HttpSecurity>> {
    private final InOAuth2AuthProperties properties;

    @Override
    public void customize(ExceptionHandlingConfigurer<HttpSecurity> configurer) {
        // 默认 authorize code 模式，暂时不做特殊处理
//        properties.getLoginFormUrl()
        configurer.authenticationEntryPoint(
                new InLoginUrlAuthenticationEntryPoint(FormLoginCustomizer.LOGIN_PAGE_URL));
    }
}
