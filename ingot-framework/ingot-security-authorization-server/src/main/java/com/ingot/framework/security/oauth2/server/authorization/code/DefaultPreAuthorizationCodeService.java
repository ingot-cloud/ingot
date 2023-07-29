package com.ingot.framework.security.oauth2.server.authorization.code;

import cn.hutool.core.map.MapUtil;

import java.util.Map;

/**
 * <p>Description  : DefaultPreAuthorizationCodeService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/28.</p>
 * <p>Time         : 3:14 PM.</p>
 */
public class DefaultPreAuthorizationCodeService implements PreAuthorizationCodeService {
    private final Map<String, PreAuthorization> cache = MapUtil.newConcurrentHashMap();

    @Override
    public void save(PreAuthorization authorization, String code) {
        cache.put(code, authorization);
    }

    @Override
    public PreAuthorization get(String code) {
        return cache.remove(code);
    }
}
