package com.ingot.framework.security.oauth2.server.authorization.web;

import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.DefaultAuthenticationFailureHandler;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.OAuth2PreAuthorizationAuthenticationConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.web.authentication.DelegatingAuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * <p>Description  : .</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 11:09 AM.</p>
 */
@Slf4j
public final class OAuth2PreAuthorizationEndpointFilter extends OncePerRequestFilter {
    private final AuthenticationManager authenticationManager;
    private final RequestMatcher requestMatcher;
    private final AuthenticationConverter authenticationConverter;
    private final AuthenticationSuccessHandler authenticationSuccessHandler = this::sendResponse;
    private final AuthenticationFailureHandler authenticationFailureHandler
            = new DefaultAuthenticationFailureHandler();

    public OAuth2PreAuthorizationEndpointFilter(AuthenticationManager authenticationManager,
                                                RequestMatcher requestMatcher) {
        Assert.notNull(authenticationManager, "authenticationManager cannot be null");
        Assert.notNull(requestMatcher, "requestMatcher cannot be null");
        this.authenticationManager = authenticationManager;
        this.requestMatcher = requestMatcher;
        this.authenticationConverter = new DelegatingAuthenticationConverter(
                Collections.singletonList(
                        new OAuth2PreAuthorizationAuthenticationConverter()));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (!this.requestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Authentication authenticationRequest = this.authenticationConverter.convert(request);
            if (authenticationRequest == null) {
                filterChain.doFilter(request, response);
                return;
            }

            Authentication authenticationResult = this.authenticationManager.authenticate(authenticationRequest);
            this.authenticationSuccessHandler.onAuthenticationSuccess(request, response, authenticationResult);
        } catch (AuthenticationException ex) {
            SecurityContextHolder.clearContext();
            this.authenticationFailureHandler.onAuthenticationFailure(request, response, ex);
        }
    }

    private void sendResponse(HttpServletRequest request,
                              HttpServletResponse response,
                              Authentication authentication) throws IOException {
        OAuth2PreAuthorizationAuthenticationToken token = (OAuth2PreAuthorizationAuthenticationToken) authentication;

        response.getWriter().write(token.getPrincipal().toString());
        response.flushBuffer();
    }
}
