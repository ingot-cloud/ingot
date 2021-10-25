package com.ingot.framework.security.oauth2.server.resource.web;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.SecurityConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>Description  : OAuth2InnerResourceFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/23.</p>
 * <p>Time         : 11:33 上午.</p>
 */
@Slf4j
public class OAuth2InnerResourceFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String header = request.getHeader(SecurityConstants.HEADER_FROM);
        if (!StrUtil.equals(SecurityConstants.HEADER_FROM_INSIDE_VALUE, header)) {
            log.warn("[OAuth2InnerResourceFilter] 访问接口 {} 没有权限", request.getRequestURI());
            throw new AccessDeniedException("Access is denied");
        }

        filterChain.doFilter(request, response);
    }
}
