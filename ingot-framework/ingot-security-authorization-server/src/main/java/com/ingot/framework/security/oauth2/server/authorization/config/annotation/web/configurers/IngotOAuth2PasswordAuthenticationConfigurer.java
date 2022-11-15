package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configurers;

import java.util.ArrayList;
import java.util.List;

import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PasswordAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UsernamePasswordAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.web.OAuth2UsernamePasswordAuthenticationFilter;
import com.ingot.framework.security.web.ClientAuthContextFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
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
public class IngotOAuth2PasswordAuthenticationConfigurer
        extends AbstractHttpConfigurer<IngotOAuth2PasswordAuthenticationConfigurer, HttpSecurity> {
    private RequestMatcher requestMatcher;

    @Override
    public void init(HttpSecurity httpSecurity) throws Exception {
        if (!canConfig(httpSecurity)) {
            return;
        }

        AuthorizationServerSettings providerSettings = OAuth2ConfigurerUtils.getAuthorizationServerSettings(httpSecurity);
        this.requestMatcher = new AntPathRequestMatcher(
                providerSettings.getTokenEndpoint(), HttpMethod.POST.name());

        List<AuthenticationProvider> authenticationProviders =
                createPasswordAuthenticationProviders(httpSecurity);
        authenticationProviders.forEach(authenticationProvider ->
                httpSecurity.authenticationProvider(postProcess(authenticationProvider)));
    }

    @Override
    public void configure(HttpSecurity httpSecurity) throws Exception {
        if (!canConfig(httpSecurity)) {
            return;
        }

        AuthenticationManager authenticationManager = httpSecurity.getSharedObject(AuthenticationManager.class);

        // ClientAuthContextFilter 在 OAuth2ClientAuthenticationFilter 后面
        ClientAuthContextFilter clientAuthContextFilter = new ClientAuthContextFilter();
        httpSecurity.addFilterAfter(postProcess(clientAuthContextFilter), OAuth2ClientAuthenticationFilter.class);

        // OAuth2UsernamePasswordAuthenticationFilter 在 ClientAuthContextFilter 后面
        OAuth2UsernamePasswordAuthenticationFilter filter =
                new OAuth2UsernamePasswordAuthenticationFilter(authenticationManager, this.requestMatcher);
        httpSecurity.addFilterAfter(postProcess(filter), ClientAuthContextFilter.class);
    }

    private List<AuthenticationProvider> createPasswordAuthenticationProviders(HttpSecurity httpSecurity) {
        List<AuthenticationProvider> authenticationProviders = new ArrayList<>();

        OAuth2AuthorizationService authorizationService =
                OAuth2ConfigurerUtils.getAuthorizationService(httpSecurity);
        OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator =
                OAuth2ConfigurerUtils.getTokenGenerator(httpSecurity);
        PasswordEncoder passwordEncoder = OAuth2ConfigurerUtils.getOptionalBean(
                httpSecurity, PasswordEncoder.class);
        UserDetailsPasswordService passwordManager = OAuth2ConfigurerUtils.getOptionalBean(
                httpSecurity, UserDetailsPasswordService.class);
        UserDetailsService userDetailsService = OAuth2ConfigurerUtils.getBean(
                httpSecurity, UserDetailsService.class);

        OAuth2UsernamePasswordAuthenticationProvider usernamePasswordProvider =
                new OAuth2UsernamePasswordAuthenticationProvider();
        usernamePasswordProvider.setUserDetailsService(userDetailsService);
        if (passwordEncoder != null) {
            usernamePasswordProvider.setPasswordEncoder(passwordEncoder);
        }
        if (passwordManager != null) {
            usernamePasswordProvider.setUserDetailsPasswordService(passwordManager);
        }
        authenticationProviders.add(usernamePasswordProvider);

        OAuth2PasswordAuthenticationProvider passwordAuthProvider =
                new OAuth2PasswordAuthenticationProvider(authorizationService, tokenGenerator);
        authenticationProviders.add(passwordAuthProvider);

        return authenticationProviders;
    }

    private boolean canConfig(HttpSecurity httpSecurity) {
        return OAuth2ConfigurerUtils.getOptionalBean(
                httpSecurity, UserDetailsService.class) != null;
    }
}
