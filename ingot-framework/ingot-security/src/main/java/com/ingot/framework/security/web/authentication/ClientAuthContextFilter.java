package com.ingot.framework.security.web.authentication;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.core.context.ClientContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>Description  : UsernamePasswordEnhanceAuthenticationFilter.</p>
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

        String clientId = null;
        SavedRequest savedRequest = requestCache.getRequest(request, response);
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

        if (StrUtil.isNotEmpty(clientId)) {
            ClientContextHolder.set(clientId);
            log.info("[ClientAuthContextFilter] - ClientContextHolder Set Client Id = {}", clientId);
        }
        filterChain.doFilter(request, response);
        ClientContextHolder.clear();
    }
}
