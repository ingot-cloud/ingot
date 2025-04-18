package com.ingot.cloud.auth.config;

import com.ingot.framework.core.utils.CookieUtils;
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
        configurer.deleteCookies(CookieUtils.SESSION_ID_NAME)
                .invalidateHttpSession(true)
                .permitAll();
    }
}
