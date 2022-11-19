package com.ingot.framework.security.web;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.core.context.ClientContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>Description  : ClientAuthContextFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/28.</p>
 * <p>Time         : 9:16 上午.</p>
 */
@Slf4j
public class ClientAuthContextFilter extends OncePerRequestFilter {
    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String url = request.getRequestURI();
        log.info("[ClientAuthContextFilter] do filter url = {}", url);

        try {
            String clientId = null;
            HttpServletRequest savedRequest = requestCache.getMatchingRequest(request,
                    response);
            if (savedRequest == null) {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null) {
                    clientId = authentication.getName();
                }
            } else {
                String[] ids = savedRequest.getParameterValues("client_id");
                if (ArrayUtil.isNotEmpty(ids)) {
                    clientId = ids[0];
                }
            }

            log.info("[ClientAuthContextFilter] savedRequest {}, clientId = {}",
                    (savedRequest == null) ? "不存在" : "存在", clientId);

            if (StrUtil.isNotEmpty(clientId)) {
                ClientContextHolder.set(clientId);
            }
            filterChain.doFilter(request, response);
        } finally {
            ClientContextHolder.clear();
        }
    }
}
