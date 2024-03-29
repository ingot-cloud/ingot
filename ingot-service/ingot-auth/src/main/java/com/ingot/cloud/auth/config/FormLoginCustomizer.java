package com.ingot.cloud.auth.config;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractAuthenticationFilterConfigurer;
import org.springframework.security.config.annotation.web.configurers.FormLoginConfigurer;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * <p>Description  : FormLoginCustomizer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/19.</p>
 * <p>Time         : 1:56 PM.</p>
 */
public class FormLoginCustomizer implements Customizer<FormLoginConfigurer<HttpSecurity>> {
    public static final String LOGIN_PAGE_URL = "/oauth2/login";
    public static final String LOGIN_PROCESSING_URL = "/oauth2/form";

    /**
     * 自定义login页面，修改{@link UsernamePasswordAuthenticationFilter}
     * 的RequestMatcher匹配URL({@link #LOGIN_PROCESSING_URL})
     * {@link AbstractAuthenticationFilterConfigurer}的默认failureHandler为{@link SimpleUrlAuthenticationFailureHandler}
     *
     * @param configurer the input argument
     */
    @Override
    public void customize(FormLoginConfigurer<HttpSecurity> configurer) {
        configurer.loginPage(LOGIN_PAGE_URL)
                .loginProcessingUrl(LOGIN_PROCESSING_URL);
    }
}
