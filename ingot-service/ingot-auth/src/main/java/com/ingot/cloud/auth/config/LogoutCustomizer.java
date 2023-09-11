package com.ingot.cloud.auth.config;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;

/**
 * <p>Description  : LogoutCustomizer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/26.</p>
 * <p>Time         : 12:14 PM.</p>
 */
public class LogoutCustomizer implements Customizer<LogoutConfigurer<HttpSecurity>> {

    @Override
    public void customize(LogoutConfigurer<HttpSecurity> configurer) {
        configurer.deleteCookies("JSESSIONID")
                .invalidateHttpSession(true)
                .permitAll();
    }
}
