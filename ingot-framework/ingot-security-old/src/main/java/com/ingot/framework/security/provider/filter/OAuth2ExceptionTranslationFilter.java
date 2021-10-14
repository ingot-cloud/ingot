package com.ingot.framework.security.provider.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ingot.framework.security.provider.error.IngotWebResponseExceptionTranslator;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.oauth2.common.exceptions.OAuth2Exception;
import org.springframework.security.oauth2.provider.error.DefaultOAuth2ExceptionRenderer;
import org.springframework.security.oauth2.provider.error.OAuth2ExceptionRenderer;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>Description  : OAuth2ExceptionTranslationFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/19.</p>
 * <p>Time         : 4:10 下午.</p>
 */
public class OAuth2ExceptionTranslationFilter extends OncePerRequestFilter {
    private final IngotWebResponseExceptionTranslator exceptionTranslator = new IngotWebResponseExceptionTranslator();
    private final OAuth2ExceptionRenderer exceptionRenderer = new DefaultOAuth2ExceptionRenderer();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (OAuth2Exception e) {
            try {
                ResponseEntity<?> result = exceptionTranslator.translate(e);
                exceptionRenderer.handleHttpEntityResponse(result, new ServletWebRequest(request, response));
                response.flushBuffer();
            } catch (Exception exception) {
                throw new RuntimeException(exception);
            }
        }
    }
}
