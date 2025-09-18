package com.ingot.framework.security.oauth2.server.authorization.web;

import java.io.IOException;

import com.ingot.framework.security.oauth2.server.authorization.web.authentication.OAuth2PreAuthorizationCodeRequestUserDetailsAuthenticationConverter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>Description  : OAuth2PreAuthorizationUserDetailsAuthenticationFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/28.</p>
 * <p>Time         : 2:04 PM.</p>
 */
@Slf4j
public final class OAuth2PreAuthorizationCodeRequestUserDetailsAuthenticationFilter extends OncePerRequestFilter {
    private final OAuth2UserDetailsAuthenticationFilter proxy;

    public OAuth2PreAuthorizationCodeRequestUserDetailsAuthenticationFilter(AuthenticationManager authenticationManager,
                                                                            RequestMatcher requestMatcher) {
        this.proxy = new OAuth2UserDetailsAuthenticationFilter(authenticationManager, requestMatcher);
        this.proxy.setAuthenticationConverter(new OAuth2PreAuthorizationCodeRequestUserDetailsAuthenticationConverter());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        proxy.doFilterInternal(request, response, filterChain);
    }
}
