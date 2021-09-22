package com.ingot.framework.security.oauth2.server.resource.web;

import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;

import javax.servlet.http.HttpServletRequest;

/**
 * <p>Description  : IngotBearerTokenResolver.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/17.</p>
 * <p>Time         : 5:57 下午.</p>
 */
public class IngotBearerTokenResolver implements BearerTokenResolver {
    private final DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();

    @Override
    public String resolve(HttpServletRequest request) {
        // todo 关联 permit，如果是permit的url，那么返回空
        return defaultResolver.resolve(request);
    }
}
