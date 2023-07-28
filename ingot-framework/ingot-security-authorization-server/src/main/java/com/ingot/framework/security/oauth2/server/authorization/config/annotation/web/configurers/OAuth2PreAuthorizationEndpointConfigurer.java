package com.ingot.framework.security.oauth2.server.authorization.config.annotation.web.configurers;

import com.ingot.framework.security.core.tenantdetails.TenantDetailsService;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationAuthenticationProvider;
import com.ingot.framework.security.oauth2.server.authorization.web.OAuth2PreAuthorizationEndpointFilter;
import com.ingot.framework.security.oauth2.server.authorization.web.OAuth2PreAuthorizationUserDetailsAuthenticationFilter;
import com.ingot.framework.security.oauth2.server.authorization.web.OAuth2UserDetailsAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.authorization.web.OAuth2PreAuthorizationClientAuthenticationFilter;
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
public class OAuth2PreAuthorizationEndpointConfigurer extends AbstractOAuth2Configurer {
    private static final String DEFAULT_PRE_AUTHORIZATION_ENDPOINT_URI = "/oauth2/pre_authorize";

    private RequestMatcher requestMatcher;

    OAuth2PreAuthorizationEndpointConfigurer(ObjectPostProcessor<Object> objectPostProcessor) {
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

        // 增加处理client认证
        OAuth2PreAuthorizationClientAuthenticationFilter clientFilter =
                new OAuth2PreAuthorizationClientAuthenticationFilter(authenticationManager, this.requestMatcher);
        httpSecurity.addFilterAfter(
                postProcess(clientFilter), OAuth2UserDetailsAuthenticationFilter.class);

        // user details
        OAuth2PreAuthorizationUserDetailsAuthenticationFilter userDetailsFilter =
                new OAuth2PreAuthorizationUserDetailsAuthenticationFilter(authenticationManager, this.requestMatcher);
        httpSecurity.addFilterAfter(
                postProcess(userDetailsFilter), OAuth2PreAuthorizationClientAuthenticationFilter.class);

        OAuth2PreAuthorizationEndpointFilter preAuthorizationEndpointFilter =
                new OAuth2PreAuthorizationEndpointFilter(authenticationManager, this.requestMatcher);
        httpSecurity.addFilterAfter(
                postProcess(preAuthorizationEndpointFilter), OAuth2PreAuthorizationUserDetailsAuthenticationFilter.class);

    }

    @Override
    RequestMatcher getRequestMatcher() {
        return requestMatcher;
    }

    private List<AuthenticationProvider> createProviders(HttpSecurity httpSecurity) {
        List<AuthenticationProvider> authenticationProviders = new ArrayList<>();
        OAuth2PreAuthorizationAuthenticationProvider provider = new OAuth2PreAuthorizationAuthenticationProvider();
        TenantDetailsService tenantDetailsService = OAuth2ConfigurerUtils.getBean(
                httpSecurity, TenantDetailsService.class);
        provider.setTenantDetailsService(tenantDetailsService);

        authenticationProviders.add(provider);
        return authenticationProviders;
    }
}
