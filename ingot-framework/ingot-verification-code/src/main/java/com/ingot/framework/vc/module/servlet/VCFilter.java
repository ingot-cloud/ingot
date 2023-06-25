package com.ingot.framework.vc.module.servlet;

import com.ingot.framework.vc.common.VCException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>Description  : VCFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/29.</p>
 * <p>Time         : 5:24 PM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class VCFilter extends OncePerRequestFilter {
    private final VCProviderManager providerManager;
    private final VCVerifyResolver verifyResolver;
    private final VCFailureHandler failureHandler;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        try {
            verifyResolver.matches(request, (type) -> {
                providerManager.checkOnly(type, new ServletWebRequest(request, response));
            });
        } catch (VCException e) {
            failureHandler.onFailure(request, response, e);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
