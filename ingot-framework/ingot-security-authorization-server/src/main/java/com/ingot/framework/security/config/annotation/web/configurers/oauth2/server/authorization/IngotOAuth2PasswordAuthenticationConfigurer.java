package com.ingot.framework.security.config.annotation.web.configurers.oauth2.server.authorization;

import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PasswordAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UsernamePasswordAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.web.OAuth2UsernamePasswordAuthenticationFilter;
import com.ingot.framework.security.web.authentication.ClientAuthContextFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.HttpSecurityBuilder;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenCustomizer;
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
@Slf4j
public class IngotOAuth2PasswordAuthenticationConfigurer<B extends HttpSecurityBuilder<B>>
        extends AbstractHttpConfigurer<IngotOAuth2PasswordAuthenticationConfigurer<B>, B> {
    private RequestMatcher requestMatcher;

    @Override
    public void init(B builder) throws Exception {
        if (!canConfig(builder)) {
            return;
        }

        ProviderSettings providerSettings = OAuth2ConfigurerUtils.getProviderSettings(builder);
        this.requestMatcher = new AntPathRequestMatcher(
                providerSettings.getTokenEndpoint(),
                HttpMethod.POST.name());

        OAuth2AuthorizationService authorizationService =
                OAuth2ConfigurerUtils.getAuthorizationService(builder);
        JwtEncoder jwtEncoder = OAuth2ConfigurerUtils.getJwtEncoder(builder);
        OAuth2TokenCustomizer<JwtEncodingContext> jwtCustomizer =
                OAuth2ConfigurerUtils.getJwtCustomizer(builder);

        UserDetailsService userDetailsService = OAuth2ConfigurerUtils.getBean(
                builder, UserDetailsService.class);
        PasswordEncoder passwordEncoder = OAuth2ConfigurerUtils.getBeanOrNull(
                builder, PasswordEncoder.class);
        UserDetailsPasswordService passwordManager = OAuth2ConfigurerUtils.getBeanOrNull(
                builder, UserDetailsPasswordService.class);

        OAuth2UsernamePasswordAuthenticationProvider usernamePasswordProvider =
                new OAuth2UsernamePasswordAuthenticationProvider();
        usernamePasswordProvider.setUserDetailsService(userDetailsService);
        if (passwordEncoder != null) {
            usernamePasswordProvider.setPasswordEncoder(passwordEncoder);
        }
        if (passwordManager != null) {
            usernamePasswordProvider.setUserDetailsPasswordService(passwordManager);
        }
        builder.authenticationProvider(
                postProcess(usernamePasswordProvider));

        OAuth2PasswordAuthenticationProvider passwordAuthProvider =
                new OAuth2PasswordAuthenticationProvider(authorizationService, jwtEncoder);
        passwordAuthProvider.setProviderSettings(providerSettings);
        if (jwtCustomizer != null) {
            passwordAuthProvider.setJwtCustomizer(jwtCustomizer);
        }

        builder.authenticationProvider(
                postProcess(passwordAuthProvider));
    }

    @Override
    public void configure(B builder) throws Exception {
        if (!canConfig(builder)) {
            return;
        }

        AuthenticationManager authenticationManager = builder.getSharedObject(AuthenticationManager.class);

        // ClientAuthContextFilter 在 OAuth2ClientAuthenticationFilter 后面
        ClientAuthContextFilter clientAuthContextFilter = new ClientAuthContextFilter();
        builder.addFilterAfter(postProcess(clientAuthContextFilter), OAuth2ClientAuthenticationFilter.class);

        // OAuth2UsernamePasswordAuthenticationFilter 在 ClientAuthContextFilter 后面
        OAuth2UsernamePasswordAuthenticationFilter filter =
                new OAuth2UsernamePasswordAuthenticationFilter(authenticationManager, this.requestMatcher);
        builder.addFilterAfter(postProcess(filter), ClientAuthContextFilter.class);
    }

    private boolean canConfig(B builder) {
        return OAuth2ConfigurerUtils.getBeanOrNull(
                builder, UserDetailsService.class) != null;
    }
}
