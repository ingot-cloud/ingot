package com.ingot.framework.security.web;

import java.io.IOException;
import java.util.Optional;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.model.security.TokenAuthTypeEnum;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.security.oauth2.server.authorization.AuthorizationCache;
import com.ingot.framework.security.oauth2.server.authorization.AuthorizationCacheService;
import com.ingot.framework.security.utils.SecurityUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
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
    private AuthorizationCacheService authorizationCacheService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        if (ignoreRequestMatcher.matches(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        InUser user = SecurityAuthContext.getUser();
        if (user == null) {
            OAuth2ErrorUtils.throwInvalidToken();
        }
        TokenAuthTypeEnum authType = TokenAuthTypeEnum.getEnum(user.getTokenAuthType());
        if (authType != TokenAuthTypeEnum.UNIQUE) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取当前token
        AuthorizationCache cache = authorizationCacheService.get(user);
        if (cache == null) {
            OAuth2ErrorUtils.throwInvalidToken();
        }

        Optional<String> token = SecurityUtils.getBearerToken(request);
        if (token.isEmpty()) {
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
