package com.ingot.framework.security.oauth2.server.authorization.web;

import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UsernamePasswordAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.IngotAuthenticationFailureHandler;
import com.ingot.framework.security.oauth2.server.authorization.web.authentication.OAuth2UsernamePasswordAuthenticationConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2Error;
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

/**
 * <p>Description  : OAuth2UsernamePasswordAuthenticationFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/10.</p>
 * <p>Time         : 9:28 上午.</p>
 */
@Slf4j
public class OAuth2UsernamePasswordAuthenticationFilter extends OncePerRequestFilter {
    private final AuthenticationManager authenticationManager;
    private final RequestMatcher requestMatcher;
    private AuthenticationConverter authenticationConverter;
    private AuthenticationSuccessHandler authenticationSuccessHandler = this::setSecurityContext;
    private AuthenticationFailureHandler authenticationFailureHandler = new IngotAuthenticationFailureHandler();

    public OAuth2UsernamePasswordAuthenticationFilter(AuthenticationManager authenticationManager,
                                                      RequestMatcher requestMatcher) {
        Assert.notNull(authenticationManager, "authenticationManager cannot be null");
        Assert.notNull(requestMatcher, "requestMatcher cannot be null");
        this.authenticationManager = authenticationManager;
        this.requestMatcher = requestMatcher;
        this.authenticationConverter = new OAuth2UsernamePasswordAuthenticationConverter();
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
            if (authenticationRequest != null) {
                Authentication authenticationResult = this.authenticationManager.authenticate(authenticationRequest);
                this.authenticationSuccessHandler.onAuthenticationSuccess(request, response, authenticationResult);
            }
            filterChain.doFilter(request, response);

        } catch (AuthenticationException ex) {
            this.authenticationFailureHandler.onAuthenticationFailure(request, response, ex);
        }
    }

    /**
     * Sets the {@link AuthenticationConverter} used when attempting to extract password from {@link HttpServletRequest}
     * to an instance of {@link OAuth2UsernamePasswordAuthenticationToken} used for authenticating the client.
     *
     * @param authenticationConverter the {@link AuthenticationConverter} used when attempting to extract client credentials from {@link HttpServletRequest}
     */
    public void setAuthenticationConverter(AuthenticationConverter authenticationConverter) {
        Assert.notNull(authenticationConverter, "authenticationConverter cannot be null");
        this.authenticationConverter = authenticationConverter;
    }

    /**
     * Sets the {@link AuthenticationSuccessHandler} used for handling a successful password authentication
     * and associating the {@link OAuth2UsernamePasswordAuthenticationToken} to the {@link SecurityContext}.
     *
     * @param authenticationSuccessHandler the {@link AuthenticationSuccessHandler} used for handling a successful client authentication
     */
    public void setAuthenticationSuccessHandler(AuthenticationSuccessHandler authenticationSuccessHandler) {
        Assert.notNull(authenticationSuccessHandler, "authenticationSuccessHandler cannot be null");
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    /**
     * Sets the {@link AuthenticationFailureHandler} used for handling a failed password authentication
     * and returning the {@link OAuth2Error Error Response}.
     *
     * @param authenticationFailureHandler the {@link AuthenticationFailureHandler} used for handling a failed client authentication
     */
    public void setAuthenticationFailureHandler(AuthenticationFailureHandler authenticationFailureHandler) {
        Assert.notNull(authenticationFailureHandler, "authenticationFailureHandler cannot be null");
        this.authenticationFailureHandler = authenticationFailureHandler;
    }

    private void setSecurityContext(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Authentication authentication) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
    }
}
