package com.ingot.framework.security.oauth2.server.resource.web;

import jakarta.servlet.http.HttpServletRequest;

import com.ingot.framework.security.oauth2.core.PermitResolver;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * <p>Description  : Bearer Token 解析器.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/17.</p>
 * <p>Time         : 5:57 下午.</p>
 */
public class InBearerTokenResolver implements BearerTokenResolver {
    private final DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
    private final RequestMatcher requestMatcher;

    public InBearerTokenResolver(PermitResolver permitResolver) {
        this.requestMatcher = permitResolver.permitAllRequestMatcher();
    }

    @Override
    public String resolve(HttpServletRequest request) {
        if (requestMatcher.matches(request)) {
            return null;
        }
        return defaultResolver.resolve(request);
    }
}
