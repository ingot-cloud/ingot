package com.ingot.cloud.auth.service;

import cn.hutool.core.util.RandomUtil;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.security.oauth2.server.authorization.code.OAuth2PreAuthorization;
import com.ingot.framework.security.oauth2.server.authorization.code.PreAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * <p>Description  : DefaultPreAuthorizationCodeService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/7/28.</p>
 * <p>Time         : 4:53 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultPreAuthorizationService implements PreAuthorizationService {
    /**
     * 2分钟超时
     */
    private static final int EXPIRED_TIME = 2 * 60;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(OAuth2PreAuthorization authorization) {
        String code = authorization.getToken().getTokenValue();
        redisTemplate.opsForValue().set(key(code), authorization,
                EXPIRED_TIME + RandomUtil.randomInt(10), TimeUnit.SECONDS);
    }

    @Override
    public OAuth2PreAuthorization get(String code) {
        String key = key(code);
        Object value = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        return value != null ? (OAuth2PreAuthorization) value : null;
    }

    private String key(String code) {
        return CacheConstants.PRE_AUTHORIZATION + ":" + code;
    }
}
