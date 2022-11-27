package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configurers;

import java.util.ArrayList;
import java.util.List;

import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PasswordAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.web.OAuth2UserDetailsAuthenticationFilter;
import com.ingot.framework.security.web.ClientContextAwareFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.web.OAuth2ClientAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * <p>Description  : 增强{@link OAuth2AuthorizationServerConfigurer}.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/10.</p>
 * <p>Time         : 10:05 上午.</p>
 */
@Slf4j
public class OAuth2AuthorizationServerEnhanceConfigurer
        extends AbstractHttpConfigurer<OAuth2AuthorizationServerEnhanceConfigurer, HttpSecurity> {
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
        ClientContextAwareFilter clientAuthContextFilter = new ClientContextAwareFilter();
        httpSecurity.addFilterAfter(postProcess(clientAuthContextFilter), OAuth2ClientAuthenticationFilter.class);

        // OAuth2UserDetailsAuthenticationFilter 在 ClientAuthContextFilter 后面
        OAuth2UserDetailsAuthenticationFilter filter =
                new OAuth2UserDetailsAuthenticationFilter(authenticationManager, this.requestMatcher);
        httpSecurity.addFilterAfter(postProcess(filter), ClientContextAwareFilter.class);
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
        UserDetailsChecker userDetailsChecker = OAuth2ConfigurerUtils.getOptionalBean(
                httpSecurity, UserDetailsChecker.class);
        UserDetailsService userDetailsService = OAuth2ConfigurerUtils.getBean(
                httpSecurity, UserDetailsService.class);

        OAuth2UserDetailsAuthenticationProvider userDetailsAuthenticationProvider =
                new OAuth2UserDetailsAuthenticationProvider();
        userDetailsAuthenticationProvider.setUserDetailsService(userDetailsService);
        if (passwordEncoder != null) {
            userDetailsAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        }
        if (passwordManager != null) {
            userDetailsAuthenticationProvider.setUserDetailsPasswordService(passwordManager);
        }
        if (userDetailsChecker != null) {
            userDetailsAuthenticationProvider.setAuthenticationChecks(userDetailsChecker);
        }
        authenticationProviders.add(userDetailsAuthenticationProvider);

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
