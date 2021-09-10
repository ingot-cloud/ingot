package com.ingot.framework.security.config.annotation.web.configurers.oauth2.server.authorization;

import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UsernamePasswordAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.web.OAuth2UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings;
import org.springframework.security.oauth2.server.authorization.web.OAuth2ClientAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * <p>Description  : OAuth2UsernamePasswordAuthenticationConfigurer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/10.</p>
 * <p>Time         : 10:05 上午.</p>
 */
public class OAuth2UsernamePasswordAuthenticationConfigurer<B extends HttpSecurityBuilder<B>>
        extends AbstractHttpConfigurer<OAuth2UsernamePasswordAuthenticationConfigurer<B>, B> {
    private RequestMatcher requestMatcher;

    @Override
    public void init(B builder) throws Exception {
        ProviderSettings providerSettings = OAuth2ConfigurerUtils.getProviderSettings(builder);
        this.requestMatcher = new AntPathRequestMatcher(
                providerSettings.getTokenEndpoint(),
                HttpMethod.POST.name());

        builder.authenticationProvider(
                postProcess(new OAuth2UsernamePasswordAuthenticationProvider()));
    }

    @Override
    public void configure(B builder) throws Exception {
        AuthenticationManager authenticationManager = builder.getSharedObject(AuthenticationManager.class);
        OAuth2UsernamePasswordAuthenticationFilter filter =
                new OAuth2UsernamePasswordAuthenticationFilter(authenticationManager, this.requestMatcher);

        builder.addFilterAfter(postProcess(filter), OAuth2ClientAuthenticationFilter.class);
    }
}
