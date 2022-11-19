package com.ingot.cloud.auth.config;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;

/**
 * <p>Description  : FormLoginCustomizer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/19.</p>
 * <p>Time         : 1:56 PM.</p>
 */
public class FormLoginCustomizer implements Customizer<FormLoginConfigurer<HttpSecurity>> {

    @Override
    public void customize(FormLoginConfigurer<HttpSecurity> configurer) {
        configurer.loginPage("/oauth2/login")
                .loginProcessingUrl("/oauth2/form");
    }
}
