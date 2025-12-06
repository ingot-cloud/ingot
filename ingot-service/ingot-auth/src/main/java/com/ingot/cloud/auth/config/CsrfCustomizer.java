package com.ingot.cloud.auth.config;

import com.ingot.framework.security.oauth2.core.PermitResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * <p>Description  : CsrfCustomizer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/25.</p>
 * <p>Time         : 10:34 PM.</p>
 */
@RequiredArgsConstructor
public class CsrfCustomizer implements Customizer<CsrfConfigurer<HttpSecurity>> {
    private static final PathPatternRequestMatcher LOGIN_MATCHER
            = PathPatternRequestMatcher
            .withDefaults()
            .matcher(HttpMethod.POST, FormLoginCustomizer.LOGIN_PROCESSING_URL);

    private final PermitResolver permitResolver;

    @Override
    public void customize(CsrfConfigurer<HttpSecurity> configurer) {
        configurer.ignoringRequestMatchers(permitResolver.publicRequestMatcher(), LOGIN_MATCHER);
    }
}
