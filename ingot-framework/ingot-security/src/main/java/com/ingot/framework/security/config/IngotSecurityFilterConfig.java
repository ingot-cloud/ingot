package com.ingot.framework.security.config;

import com.ingot.framework.security.annotation.IgnoreUserAuthentication;
import com.ingot.framework.security.core.filter.IgnoreBearerTokenFilter;
import com.ingot.framework.security.core.filter.UserAuthenticationFilter;
import com.ingot.framework.security.service.AuthenticationService;
import com.ingot.framework.security.service.UserAccessTokenRedisService;
import com.ingot.framework.security.utils.ResourcePermitUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.header.HeaderWriterFilter;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : IngotSecurityConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/4.</p>
 * <p>Time         : 5:39 PM.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngotSecurityFilterConfig extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {
    private final ResourcePermitUtils resourcePermitUtils;
    @Lazy
    @Autowired(required = false)
    private UserAuthenticationFilter userAuthenticationFilter;

    @SneakyThrows
    @Override public void configure(HttpSecurity builder) {
        log.info(">>> IngotSecurityFilterConfig - configure.");
        IgnoreBearerTokenFilter ignoreBearerTokenFilter = new IgnoreBearerTokenFilter(resourcePermitUtils);
        builder.addFilterAfter(ignoreBearerTokenFilter, HeaderWriterFilter.class);

        boolean addUserAuthenticationFilter = userAuthenticationFilter != null;
        log.info(">>> addUserAuthenticationFilter = {}, {}", addUserAuthenticationFilter, userAuthenticationFilter);
        if (addUserAuthenticationFilter){
            builder.addFilterAfter(userAuthenticationFilter, ExceptionTranslationFilter.class);
        }
    }

    @Bean
    @ConditionalOnBean(UserAccessTokenRedisService.class)
    @ConditionalOnMissingBean(annotation = IgnoreUserAuthentication.class)
    public UserAuthenticationFilter userAuthenticationFilter(@Lazy UserAccessTokenRedisService userAccessTokenRedisService,
                                                             AuthenticationService authenticationService){
        return new UserAuthenticationFilter(userAccessTokenRedisService, resourcePermitUtils, authenticationService);
    }
}
