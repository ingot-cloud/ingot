package com.ingot.framework.security.provider;

import com.ingot.framework.security.provider.filter.IgnoreBearerTokenFilter;
import com.ingot.framework.security.provider.filter.UserAuthenticationFilter;
import com.ingot.framework.security.service.ResourcePermitService;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.SecurityConfigurerAdapter;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.header.HeaderWriterFilter;

/**
 * <p>Description  : IngotSecurityConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/4.</p>
 * <p>Time         : 5:39 PM.</p>
 */
@Slf4j
@AllArgsConstructor
public class IngotSecurityFilterConfig extends SecurityConfigurerAdapter<DefaultSecurityFilterChain, HttpSecurity> {
    private final ResourcePermitService resourcePermitService;
    private final UserAuthenticationFilter userAuthenticationFilter;

    @SneakyThrows
    @Override public void configure(HttpSecurity builder) {
        log.info(">>> IngotSecurityFilterConfig - configure.");
        IgnoreBearerTokenFilter ignoreBearerTokenFilter = new IgnoreBearerTokenFilter(resourcePermitService);
        builder.addFilterAfter(ignoreBearerTokenFilter, HeaderWriterFilter.class);

        boolean addUserAuthenticationFilter = userAuthenticationFilter != null;
        log.info(">>> addUserAuthenticationFilter = {}, {}", addUserAuthenticationFilter, userAuthenticationFilter);
        if (addUserAuthenticationFilter){
            builder.addFilterAfter(userAuthenticationFilter, ExceptionTranslationFilter.class);
        }
    }

}
