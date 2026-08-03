package com.ingot.framework.security.access.redis;

import com.ingot.framework.security.access.service.LoginFailureCounter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.Collections;

/**
 * Redis 滑动窗口登录失败计数器。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public class RedisLoginFailureCounter implements LoginFailureCounter {

    private static final RedisScript<Long> INCR_SCRIPT = RedisScript.of(
            "local v = redis.call('INCR', KEYS[1])\n" +
                    "if v == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end\n" +
                    "return v", Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisLoginFailureCounter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long increment(String counterKey, Duration window) {
        if (redisTemplate == null || counterKey == null) {
            return 0L;
        }
        long ttlMs = Math.max(1000L, window.toMillis());
        try {
            Long value = redisTemplate.execute(
                    INCR_SCRIPT,
                    Collections.singletonList(counterKey),
                    String.valueOf(ttlMs));
            return value != null ? value : 0L;
        } catch (Exception e) {
            log.warn("[LoginFailure] counter incr failed key={}", counterKey, e);
            return 0L;
        }
    }

    @Override
    public void reset(String counterKey) {
        if (redisTemplate == null || counterKey == null) {
            return;
        }
        try {
            redisTemplate.delete(counterKey);
        } catch (Exception e) {
            log.warn("[LoginFailure] counter reset failed key={}", counterKey, e);
        }
    }
}
