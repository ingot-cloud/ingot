package com.ingot.framework.security.oauth2.server.authorization.code;

import cn.hutool.core.map.MapUtil;

import java.util.Map;

/**
 * <p>Description  : DefaultPreAuthorizationCodeService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/28.</p>
 * <p>Time         : 3:14 PM.</p>
 */
public class DefaultOAuth2PreAuthorizationService implements OAuth2PreAuthorizationService {
    private final Map<String, OAuth2PreAuthorization> cache = MapUtil.newConcurrentHashMap();

    @Override
    public void save(OAuth2PreAuthorization authorization) {
        String code = authorization.getToken().getTokenValue();
        cache.put(code, authorization);
    }

    @Override
    public OAuth2PreAuthorization get(String code) {
        return cache.remove(code);
    }
}
