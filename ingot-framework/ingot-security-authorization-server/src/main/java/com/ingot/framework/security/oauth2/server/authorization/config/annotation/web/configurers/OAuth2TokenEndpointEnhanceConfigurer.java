package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configurers;

import com.ingot.framework.security.core.userdetails.OAuth2UserDetailsServiceManager;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2CustomAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.authentication.UserDetailsTokenProcessor;
import com.ingot.framework.security.oauth2.server.authorization.web.OAuth2UserDetailsAuthenticationFilter;
import com.ingot.framework.security.web.ClientContextAwareFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.web.OAuth2ClientAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description  : OAuth2TokenEndpointEnhanceConfigurer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 10:05 AM.</p>
 */
public final class OAuth2TokenEndpointEnhanceConfigurer extends AbstractOAuth2Configurer {
    private RequestMatcher requestMatcher;

    OAuth2TokenEndpointEnhanceConfigurer(ObjectPostProcessor<Object> objectPostProcessor) {
        super(objectPostProcessor);
    }

    @Override
    void init(HttpSecurity httpSecurity) {
        if (!canConfig(httpSecurity)) {
            return;
        }

        AuthorizationServerSettings providerSettings = OAuth2ConfigurerUtils.getAuthorizationServerSettings(httpSecurity);
        this.requestMatcher = new AntPathRequestMatcher(
                providerSettings.getTokenEndpoint(), HttpMethod.POST.name());

        List<AuthenticationProvider> authenticationProviders = createAuthenticationProviders(httpSecurity);
        authenticationProviders.forEach(authenticationProvider ->
                httpSecurity.authenticationProvider(postProcess(authenticationProvider)));
    }

    @Override
    void configure(HttpSecurity httpSecurity) {
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

    @Override
    RequestMatcher getRequestMatcher() {
        return this.requestMatcher;
    }

    private List<AuthenticationProvider> createAuthenticationProviders(HttpSecurity httpSecurity) {
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
        OAuth2UserDetailsServiceManager userDetailsServiceManager = OAuth2ConfigurerUtils.getBean(
                httpSecurity, OAuth2UserDetailsServiceManager.class);
        UserDetailsTokenProcessor userDetailsTokenProcessor = OAuth2ConfigurerUtils.getBean(
                httpSecurity, UserDetailsTokenProcessor.class);

        // OAuth2UserDetailsAuthenticationProvider
        OAuth2UserDetailsAuthenticationProvider userDetailsAuthenticationProvider =
                new OAuth2UserDetailsAuthenticationProvider();
        userDetailsAuthenticationProvider.setUserDetailsServiceManager(userDetailsServiceManager);
        if (passwordEncoder != null) {
            userDetailsAuthenticationProvider.setPasswordEncoder(passwordEncoder);
        }
        if (passwordManager != null) {
            userDetailsAuthenticationProvider.setUserDetailsPasswordService(passwordManager);
        }
        if (userDetailsChecker != null) {
            userDetailsAuthenticationProvider.setAuthenticationChecks(userDetailsChecker);
        }
        userDetailsAuthenticationProvider.setUserDetailsTokenProcessor(userDetailsTokenProcessor);
        authenticationProviders.add(userDetailsAuthenticationProvider);

        // OAuth2CustomAuthenticationProvider
        OAuth2CustomAuthenticationProvider customAuthenticationProvider =
                new OAuth2CustomAuthenticationProvider(authorizationService, tokenGenerator);
        authenticationProviders.add(customAuthenticationProvider);

        return authenticationProviders;
    }

    private boolean canConfig(HttpSecurity httpSecurity) {
        return OAuth2ConfigurerUtils.getOptionalBean(
                httpSecurity, OAuth2UserDetailsServiceManager.class) != null;
    }
}
