package com.ingot.framework.security.web.authentication;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.utils.DigestUtils;
import com.ingot.framework.security.common.constants.TokenAuthMethod;
import com.ingot.framework.security.core.userdetails.IngotUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import static com.ingot.framework.core.constants.CacheConstants.AUTHORIZATION_KEY;

/**
 * <p>Description  : DefaultAuthorizationCacheService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/10/22.</p>
 * <p>Time         : 2:32 下午.</p>
 */
@Slf4j
public class DefaultAuthorizationCacheService implements AuthorizationCacheService {
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void save(IngotUser user, Instant expiresAt, String tokenValue) {
        if (ignore(user)) {
            return;
        }
        String key = key(user);
        long expiresIn = ChronoUnit.SECONDS.between(Instant.now(), expiresAt);
        this.redisTemplate.opsForValue().set(key, tokenValue, expiresIn, TimeUnit.SECONDS);
    }

    @Override
    public void remove(IngotUser user, String tokenValue) {
        if (ignore(user)) {
            return;
        }
        String key = key(user);
        Object current = this.redisTemplate.opsForValue().get(key);
        // 当前tokenValue和缓存tokenValue相同才可以remove
        if (current != null && StrUtil.equals(tokenValue, String.valueOf(current))) {
            this.redisTemplate.opsForValue().getOperations().delete(key);
        }
    }

    @Override
    public void remove(IngotUser user) {
        if (ignore(user)) {
            return;
        }
        String key = key(user);
        this.redisTemplate.opsForValue().getOperations().delete(key);
    }

    @Override
    public String get(IngotUser user) {
        String key = key(user);
        Object current = this.redisTemplate.opsForValue().get(key);
        return current == null ? null : String.valueOf(current);
    }

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // 只处理唯一类型
    private boolean ignore(IngotUser user) {
        TokenAuthMethod method = TokenAuthMethod.getEnum(user.getTokenAuthenticationMethod());
        return method != TokenAuthMethod.UNIQUE;
    }

    private String key(IngotUser user) {
        return String.format("%s:%d:%s:%s",
                AUTHORIZATION_KEY,
                user.getTenantId(),
                user.getClientId(),
                DigestUtils.sha256(user.getUsername()));
    }

}
