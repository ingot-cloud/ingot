package com.ingot.framework.security.web;

import java.io.IOException;
import java.util.Optional;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.server.authorization.OnlineToken;
import com.ingot.framework.security.oauth2.server.authorization.OnlineTokenService;
import com.ingot.framework.security.oauth2.server.resource.authentication.InJwtAuthenticationToken;
import com.ingot.framework.security.oauth2.server.resource.authentication.JwtContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>Description  : 用于验证{@link TokenAuthTypeEnum}为{@link TokenAuthTypeEnum#UNIQUE}时的情况，提示签退等逻辑。
 * 该Filter在{@link org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter} 之后</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/22.</p>
 * <p>Time         : 4:54 下午.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class InTokenAuthFilter extends OncePerRequestFilter {
    private final RequestMatcher ignoreRequestMatcher;
    private final OnlineTokenService onlineTokenService;

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

            // 获取当前请求的 JTI（从 SecurityContext 获取 JWT）
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (!(authentication instanceof InJwtAuthenticationToken)) {
                OAuth2ErrorUtils.throwInvalidToken();
            }
            JwtContextHolder.set(((InJwtAuthenticationToken) authentication).getToken().getId());

            // 判断登录类型
            TokenAuthTypeEnum authType = TokenAuthTypeEnum.getEnum(user.getTokenAuthType());
            if (authType == null) {
                // redis中已经没有存储当前token相关信息，提示用户token失效
                OAuth2ErrorUtils.throwInvalidToken();
            }
            if (authType != TokenAuthTypeEnum.UNIQUE) {
                // 非唯一登录，直接放行
                filterChain.doFilter(request, response);
                return;
            }

            // 唯一登录验证：检查当前 token 是否为最新的
            Optional<OnlineToken> onlineTokenOpt = onlineTokenService.getByUser(user.getId(), user.getTenantId(), user.getClientId());
            if (onlineTokenOpt.isEmpty()) {
                // 没有在线 token，说明已被强制下线
                OAuth2ErrorUtils.throwSignOut();
            }

            // 比较 JTI
            OnlineToken onlineToken = onlineTokenOpt.get();
            if (!StrUtil.equals(JwtContextHolder.get(), onlineToken.getJti())) {
                // JTI 不匹配，说明有新的登录，当前 token 已被踢掉
                log.warn("[InTokenAuthFilter] Token kicked out: userId={}, currentJti={}, latestJti={}",
                        user.getId(), JwtContextHolder.get(), onlineToken.getJti());
                OAuth2ErrorUtils.throwSignOut();
            }

            filterChain.doFilter(request, response);
        } finally {
            JwtContextHolder.clear();
        }
    }
}
