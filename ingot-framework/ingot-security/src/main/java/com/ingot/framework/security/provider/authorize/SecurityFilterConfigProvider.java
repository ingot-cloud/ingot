package com.ingot.framework.security.provider.authorize;

import com.ingot.framework.security.config.AuthorizeConfigProvider;
import com.ingot.framework.security.provider.filter.IgnoreBearerTokenFilter;
import com.ingot.framework.security.provider.filter.UserAuthenticationFilter;
import com.ingot.framework.security.service.ResourcePermitService;
import com.ingot.framework.security.service.TokenService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.header.HeaderWriterFilter;

/**
 * <p>Description  : SecurityFilterConfigProvider.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/6/5.</p>
 * <p>Time         : 10:53 AM.</p>
 */
@Slf4j
@AllArgsConstructor
public class SecurityFilterConfigProvider implements AuthorizeConfigProvider {
    private final ResourcePermitService resourcePermitService;
    private final TokenService tokenService;

    @Override public boolean config(HttpSecurity http) throws Exception {
        log.info(">>> SecurityFilterConfigProvider - configure.");
        IgnoreBearerTokenFilter ignoreBearerTokenFilter = new IgnoreBearerTokenFilter(resourcePermitService);
        http.addFilterAfter(ignoreBearerTokenFilter, HeaderWriterFilter.class);

        UserAuthenticationFilter userAuthenticationFilter = new UserAuthenticationFilter(
                resourcePermitService, tokenService);
        http.addFilterAfter(userAuthenticationFilter, ExceptionTranslationFilter.class);
        return false;
    }
}
