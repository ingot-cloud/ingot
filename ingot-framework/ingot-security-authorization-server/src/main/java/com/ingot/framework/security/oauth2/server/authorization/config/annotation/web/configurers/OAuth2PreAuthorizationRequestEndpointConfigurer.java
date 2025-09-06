package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configurers;

import com.ingot.framework.commons.constants.SecurityConstants;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.authentication.PreAuthClientAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.web.OAuth2PreAuthorizationCodeRequestEndpointFilter;
import com.ingot.framework.security.oauth2.server.authorization.web.OAuth2PreAuthorizationCodeRequestUserDetailsAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.web.OAuth2AuthorizationServerMetadataEndpointFilter;
import org.springframework.security.oauth2.server.authorization.web.OAuth2PreAuthorizationClientAuthenticationFilter;
import org.springframework.security.web.context.RedisSecurityContextRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description  : OAuth2PreAuthorizationEndpointConfigurer.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/27.</p>
 * <p>Time         : 3:34 PM.</p>
 */
public class OAuth2PreAuthorizationRequestEndpointConfigurer extends AbstractOAuth2Configurer {
    private static final String DEFAULT_PRE_AUTHORIZATION_ENDPOINT_URI = SecurityConstants.PRE_AUTHORIZE_URI;

    private RequestMatcher requestMatcher;

    OAuth2PreAuthorizationRequestEndpointConfigurer(ObjectPostProcessor<Object> objectPostProcessor) {
        super(objectPostProcessor);
    }

    @Override
    void init(HttpSecurity httpSecurity) {
        this.requestMatcher = new AntPathRequestMatcher(
                DEFAULT_PRE_AUTHORIZATION_ENDPOINT_URI, HttpMethod.POST.name());

        List<AuthenticationProvider> providers =
                createProviders(httpSecurity);
        providers.forEach(authenticationProvider ->
                httpSecurity.authenticationProvider(postProcess(authenticationProvider)));
    }

    @Override
    void configure(HttpSecurity httpSecurity) {
        AuthenticationManager authenticationManager = httpSecurity.getSharedObject(AuthenticationManager.class);

        RedisSecurityContextRepository redisSecurityContextRepository = OAuth2ConfigurerUtils.getBean(
                httpSecurity, RedisSecurityContextRepository.class);
        OAuth2PreAuthorizationCodeRequestEndpointFilter preAuthorizationEndpointFilter =
                new OAuth2PreAuthorizationCodeRequestEndpointFilter(authenticationManager, this.requestMatcher, redisSecurityContextRepository);
        httpSecurity.addFilterBefore(
                postProcess(preAuthorizationEndpointFilter), OAuth2AuthorizationServerMetadataEndpointFilter.class);

        // user details
        OAuth2PreAuthorizationCodeRequestUserDetailsAuthenticationFilter userDetailsFilter =
                new OAuth2PreAuthorizationCodeRequestUserDetailsAuthenticationFilter(authenticationManager, this.requestMatcher);
        httpSecurity.addFilterBefore(
                postProcess(userDetailsFilter), OAuth2PreAuthorizationCodeRequestEndpointFilter.class);

        // 增加处理client认证
        OAuth2PreAuthorizationClientAuthenticationFilter clientFilter =
                new OAuth2PreAuthorizationClientAuthenticationFilter(authenticationManager, this.requestMatcher);
        httpSecurity.addFilterBefore(
                postProcess(clientFilter), OAuth2PreAuthorizationCodeRequestUserDetailsAuthenticationFilter.class);
    }

    @Override
    RequestMatcher getRequestMatcher() {
        return requestMatcher;
    }

    private List<AuthenticationProvider> createProviders(HttpSecurity httpSecurity) {
        List<AuthenticationProvider> authenticationProviders = new ArrayList<>();

        OAuth2PreAuthorizationCodeRequestAuthenticationProvider provider = new OAuth2PreAuthorizationCodeRequestAuthenticationProvider();
        authenticationProviders.add(provider);

        RegisteredClientRepository clientRepository = OAuth2ConfigurerUtils.getBean(
                httpSecurity, RegisteredClientRepository.class);
        PreAuthClientAuthenticationProvider preAuthClientAuthenticationProvider =
                new PreAuthClientAuthenticationProvider(clientRepository);
        authenticationProviders.add(preAuthClientAuthenticationProvider);

        return authenticationProviders;
    }
}
