package com.ingot.framework.security.web;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.security.common.constants.TokenAuthMethod;
import com.ingot.framework.security.common.utils.SecurityUtils;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.server.authorization.AuthorizationCache;
import com.ingot.framework.security.oauth2.server.authorization.AuthorizationCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>Description  : IngotTokenAuthFilter.
 * 用于验证{@link TokenAuthMethod}为{@link TokenAuthMethod#UNIQUE}时的情况，提示签退等逻辑。
 * 该Filter在{@link org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter} 之后</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/22.</p>
 * <p>Time         : 4:54 下午.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class IngotTokenAuthFilter extends OncePerRequestFilter {
    private final RequestMatcher ignoreRequestMatcher;
    private AuthorizationCacheService authorizationCacheService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (ignoreRequestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        IngotUser user = SecurityAuthContext.getUser();
        TokenAuthMethod method = TokenAuthMethod.getEnum(user.getTokenAuthenticationMethod());
        if (method != TokenAuthMethod.UNIQUE) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取当前token
        AuthorizationCache cache = authorizationCacheService.get(user);
        if (cache == null) {
            OAuth2ErrorUtils.throwInvalidToken();
        }

        Optional<String> token = SecurityUtils.getBearerToken(request);
        if (!token.isPresent()) {
            OAuth2ErrorUtils.throwInvalidToken();
        }

        // 当前token和cache不相同，则已被签退
        if (!StrUtil.equals(cache.getTokenValue(), SecurityUtils.getBearerTokenValue(token.get()))) {
            OAuth2ErrorUtils.throwSignOut();
        }

        filterChain.doFilter(request, response);
    }

    @Autowired
    public void setAuthorizationCacheService(AuthorizationCacheService authorizationCacheService) {
        this.authorizationCacheService = authorizationCacheService;
    }
}
