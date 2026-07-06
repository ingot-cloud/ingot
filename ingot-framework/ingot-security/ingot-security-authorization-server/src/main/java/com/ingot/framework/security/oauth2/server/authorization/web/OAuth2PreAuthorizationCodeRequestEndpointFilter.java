package com.ingot.framework.security.oauth2.server.authorization.web;

import java.io.IOException;
import java.util.Collections;

import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.http.converter.OAuth2PreAuthHttpMessageConverter;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.DefaultAuthenticationFailureHandler;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationConverter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.DelegatingAuthenticationConverter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.util.Assert;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>Description  : .</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/26.</p>
 * <p>Time         : 11:09 AM.</p>
 */
@Slf4j
public final class OAuth2PreAuthorizationCodeRequestEndpointFilter extends OncePerRequestFilter {
    private final AuthenticationManager authenticationManager;
    private final RequestMatcher requestMatcher;
    private final AuthenticationConverter authenticationConverter;
    private final AuthenticationSuccessHandler authenticationSuccessHandler = this::sendResponseWithNewSession;
    private final AuthenticationSuccessHandler authenticatedSuccessHandler = this::sendResponse;
    private final AuthenticationFailureHandler authenticationFailureHandler
            = new DefaultAuthenticationFailureHandler();

    private final SecurityContextRepository securityContextRepository;
    private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
            .getContextHolderStrategy();

    private final HttpMessageConverter<OAuth2PreAuthorizationCodeRequestAuthenticationToken> responseConverter =
            new OAuth2PreAuthHttpMessageConverter();

    public OAuth2PreAuthorizationCodeRequestEndpointFilter(AuthenticationManager authenticationManager,
                                                           RequestMatcher requestMatcher,
                                                           SecurityContextRepository securityContextRepository) {
        Assert.notNull(authenticationManager, "authenticationManager cannot be null");
        Assert.notNull(requestMatcher, "requestMatcher cannot be null");
        this.authenticationManager = authenticationManager;
        this.requestMatcher = requestMatcher;
        this.authenticationConverter = new DelegatingAuthenticationConverter(
                Collections.singletonList(
                        new OAuth2PreAuthorizationCodeRequestAuthenticationConverter()));
        this.securityContextRepository = securityContextRepository;
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

            if (authenticationRequest.isAuthenticated()) {
                this.authenticatedSuccessHandler.onAuthenticationSuccess(request, response, authenticationRequest);
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
        OAuth2PreAuthorizationCodeRequestAuthenticationToken token =
                (OAuth2PreAuthorizationCodeRequestAuthenticationToken) authentication;
        log.debug("[OAuth2PreAuthorizationCodeRequestEndpointFilter] - sendResponse - session = {}",
                request.getSession(false));

        SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        this.securityContextHolderStrategy.setContext(context);
        this.securityContextRepository.saveContext(context, request, response);

        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        this.responseConverter.write(token, null, httpResponse);
    }

    private void sendResponseWithNewSession(HttpServletRequest request,
                                            HttpServletResponse response,
                                            Authentication authentication) throws IOException {
        // create session
        String sessionId = request.getSession(true).getId();
        log.debug("[OAuth2PreAuthorizationCodeRequestEndpointFilter] - sendResponseWithNewSession - create new session = {}", sessionId);

        sendResponse(request, response, authentication);
    }
}
