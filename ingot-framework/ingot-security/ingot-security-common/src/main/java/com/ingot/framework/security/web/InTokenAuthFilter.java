package com.ingot.framework.security.web;

import java.io.IOException;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.context.SessionContextHolder;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.jwt.JwtClaimNamesExtension;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>会话态过滤器：把当前请求的会话 ID 放入上下文，供后续业务读取。</p>
 *
 * <p>位于
 * {@link org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter}
 * 之后，此时 JWT 已验签且会话已确认存在（见
 * {@link com.ingot.framework.security.oauth2.server.resource.authentication.JwtInUserConverter}）。
 * 被踢下线的 Token 在 Converter 阶段因 sid 主数据缺失已被拒绝，本过滤器不再重复校验。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class InTokenAuthFilter extends OncePerRequestFilter {
    private final RequestMatcher ignoreRequestMatcher;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (ignoreRequestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            InUser user = SecurityAuthContext.getUser();
            if (user == null) {
                OAuth2ErrorUtils.throwInvalidToken();
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication instanceof InJwtAuthenticationToken jwtAuthentication)) {
                OAuth2ErrorUtils.throwInvalidToken();
                return;
            }

            String sid = JwtClaimNamesExtension.getSid(jwtAuthentication.getToken());
            if (StrUtil.isEmpty(sid)) {
                OAuth2ErrorUtils.throwInvalidToken();
            }
            SessionContextHolder.set(sid);

            filterChain.doFilter(request, response);
        } finally {
            SessionContextHolder.clear();
        }
    }
}
