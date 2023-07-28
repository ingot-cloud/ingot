package com.ingot.cloud.auth.service;

import cn.hutool.core.util.RandomUtil;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.oauth2.server.authorization.code.PreAuthorizationCodeService;
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
public class DefaultPreAuthorizationCodeService implements PreAuthorizationCodeService {
    /**
     * 2分钟超时
     */
    private static final int EXPIRED_TIME = 2 * 60;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void saveUserInfo(IngotUser user, String code) {
        redisTemplate.opsForValue().set(key(code), user,
                EXPIRED_TIME + RandomUtil.randomInt(10), TimeUnit.SECONDS);
    }

    @Override
    public IngotUser getUserInfo(String code) {
        String key = key(code);
        Object value = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        return value != null ? (IngotUser) value : null;
    }

    private String key(String code) {
        return CacheConstants.PRE_AUTHORIZATION + ":" + code;
    }
}
