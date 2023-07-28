package com.ingot.framework.security.oauth2.server.authorization.code;

import cn.hutool.core.map.MapUtil;
import com.ingot.framework.security.core.userdetails.IngotUser;

import java.util.Map;

/**
 * <p>Description  : DefaultPreAuthorizationCodeService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/28.</p>
 * <p>Time         : 3:14 PM.</p>
 */
public class DefaultPreAuthorizationCodeService implements PreAuthorizationCodeService {
    private final Map<String, IngotUser> cache = MapUtil.newConcurrentHashMap();

    @Override
    public void saveUserInfo(IngotUser user, String code) {
        cache.put(code, user);
    }

    @Override
    public IngotUser getUserInfo(String code) {
        return cache.remove(code);
    }
}
