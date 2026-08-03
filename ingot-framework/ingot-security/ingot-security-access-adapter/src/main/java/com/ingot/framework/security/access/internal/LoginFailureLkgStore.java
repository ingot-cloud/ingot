package com.ingot.framework.security.access.internal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.access.model.LoginFailurePolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

/**
 * 登录失败策略 LKG 存储（Redis 唯一源）。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public class LoginFailureLkgStore {

    private static final TypeReference<List<LoginFailurePolicy>> LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String key;

    public LoginFailureLkgStore(StringRedisTemplate redisTemplate,
                                ObjectMapper objectMapper,
                                String key) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.key = key;
    }

    public void save(List<LoginFailurePolicy> data) {
        if (redisTemplate == null || objectMapper == null) {
            return;
        }
        List<LoginFailurePolicy> snapshot = data != null ? data : List.of();
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(snapshot));
        } catch (Exception e) {
            log.warn("[LoginFailure] LKG save failed key={}", key, e);
        }
    }

    public List<LoginFailurePolicy> load() {
        if (redisTemplate == null || objectMapper == null) {
            return null;
        }
        try {
            String raw = redisTemplate.opsForValue().get(key);
            if (raw != null && !raw.isEmpty()) {
                return objectMapper.readValue(raw, LIST_TYPE);
            }
        } catch (Exception e) {
            log.warn("[LoginFailure] LKG load failed key={}", key, e);
        }
        return null;
    }
}
