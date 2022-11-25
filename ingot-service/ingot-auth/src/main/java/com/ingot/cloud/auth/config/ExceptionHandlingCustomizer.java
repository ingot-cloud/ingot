package com.ingot.cloud.auth.config;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.ExceptionHandlingConfigurer;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * <p>Description  : ExceptionHandlingCustomizer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/25.</p>
 * <p>Time         : 10:33 PM.</p>
 */
public class ExceptionHandlingCustomizer implements Customizer<ExceptionHandlingConfigurer<HttpSecurity>> {

    @Override
    public void customize(ExceptionHandlingConfigurer<HttpSecurity> configurer) {
        configurer.authenticationEntryPoint(
                new LoginUrlAuthenticationEntryPoint(FormLoginCustomizer.LOGIN_PAGE_URL));
    }
}
