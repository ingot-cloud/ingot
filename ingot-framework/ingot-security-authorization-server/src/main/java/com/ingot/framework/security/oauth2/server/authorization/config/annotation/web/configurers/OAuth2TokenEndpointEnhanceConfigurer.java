package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configurers;

import com.ingot.framework.security.core.userdetails.OAuth2UserDetailsServiceManager;
import com.ingot.framework.security.oauth2.server.authorization.authentication.IngotOAuth2AuthorizationCodeAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2CustomAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.web.OAuth2UserDetailsAuthenticationFilter;
import com.ingot.framework.security.web.ClientContextAwareFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UserDetailsPasswordService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationProvider;
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
@Slf4j
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

        // 使用IngotOAuth2AuthorizationCodeAuthenticationProvider替换默认OAuth2AuthorizationCodeAuthenticationProvider
        AuthenticationManagerBuilder builder = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        builder.objectPostProcessor(new ObjectPostProcessor<>() {
            @Override
            public <O> O postProcess(O object) {
                if (object instanceof ProviderManager providerManager) {
                    providerManager.getProviders()
                            .removeIf((item) -> (item instanceof OAuth2AuthorizationCodeAuthenticationProvider));
                }
                return object;
            }
        });
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
        authenticationProviders.add(userDetailsAuthenticationProvider);

        // OAuth2CustomAuthenticationProvider
        OAuth2CustomAuthenticationProvider customAuthenticationProvider =
                new OAuth2CustomAuthenticationProvider(authorizationService, tokenGenerator);
        authenticationProviders.add(customAuthenticationProvider);

        // 增强 OAuth2AuthorizationCodeAuthenticationProvider
        IngotOAuth2AuthorizationCodeAuthenticationProvider ingotCodeAuthProvider =
                new IngotOAuth2AuthorizationCodeAuthenticationProvider(authorizationService, tokenGenerator);
        authenticationProviders.add(ingotCodeAuthProvider);

        return authenticationProviders;
    }

    private boolean canConfig(HttpSecurity httpSecurity) {
        return OAuth2ConfigurerUtils.getOptionalBean(
                httpSecurity, OAuth2UserDetailsServiceManager.class) != null;
    }
}
