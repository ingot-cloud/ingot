package com.ingot.framework.vc.common;

import java.util.concurrent.TimeUnit;

import com.ingot.framework.vc.VCRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * <p>Description  : DefaultVCRepository.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/4/27.</p>
 * <p>Time         : 10:03 AM.</p>
 */
@RequiredArgsConstructor
public class DefaultVCRepository implements VCRepository {
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public VC get(String key, VCType type) {
        String redisKey = VCConstants.getRepositoryKey(key, type);
        Object value = redisTemplate.opsForValue().get(redisKey);
        if (value == null) {
            return null;
        }
        return (VC) value;
    }

    @Override
    public void save(String key, VC code) {
        String redisKey = VCConstants.getRepositoryKey(key, code.getType());
        redisTemplate.opsForValue().set(redisKey, code, code.getExpireIn(), TimeUnit.SECONDS);
    }

    @Override
    public void clear(String key, VCType type) {
        String redisKey = VCConstants.getRepositoryKey(key, type);
        redisTemplate.delete(redisKey);
    }


}
