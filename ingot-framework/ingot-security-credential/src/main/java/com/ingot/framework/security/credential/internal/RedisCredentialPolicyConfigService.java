package com.ingot.framework.security.credential.internal;

import java.time.Duration;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.framework.security.credential.config.CredentialCacheProperties;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * L2 Redis 共享缓存装饰器。集群所有实例可命中同一份策略配置 JSON，
 * 配合写端 evict + 跨节点失效广播实现一致性。
 * <p>
 * Key 形如：{@code <l2KeyPrefix>all}（默认 {@code in:credential:configs:all}）。
 * 不缓存空列表：避免覆盖期长期命中 {@code []}。
 * </p>
 *
 * @author jy
 * @since 2026/5/16
 */
@Slf4j
public class RedisCredentialPolicyConfigService implements CredentialPolicyConfigService {

    private static final String KEY_SUFFIX = "all";
    private static final TypeReference<List<CredentialPolicyConfigVO>> LIST_TYPE = new TypeReference<>() {
    };

    private final CredentialPolicyConfigService delegate;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final String key;
    private final Duration ttl;

    public RedisCredentialPolicyConfigService(CredentialPolicyConfigService delegate,
                                              StringRedisTemplate redisTemplate,
                                              ObjectMapper objectMapper,
                                              CredentialCacheProperties properties) {
        this.delegate = delegate;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.key = properties.getL2KeyPrefix() + KEY_SUFFIX;
        this.ttl = properties.getL2Ttl();
    }

    @Override
    public List<CredentialPolicyConfigVO> getAll() {
        List<CredentialPolicyConfigVO> hit = read();
        if (hit != null) {
            return hit;
        }
        List<CredentialPolicyConfigVO> fresh = delegate.getAll();
        write(fresh);
        return fresh != null ? fresh : List.of();
    }

    @Override
    public void evictAll() {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("[Credential] L2 evict failed key={}", key, e);
        }
        delegate.evictAll();
    }

    private List<CredentialPolicyConfigVO> read() {
        try {
            String raw = redisTemplate.opsForValue().get(key);
            List<CredentialPolicyConfigVO> list = parse(raw);
            if (list != null && list.isEmpty()) {
                try {
                    redisTemplate.delete(key);
                } catch (Exception e) {
                    log.warn("[Credential] L2 delete stale empty key failed key={}", key, e);
                }
                return null;
            }
            return list;
        } catch (Exception e) {
            log.warn("[Credential] L2 read failed key={}", key, e);
            return null;
        }
    }

    private void write(List<CredentialPolicyConfigVO> value) {
        if (value == null || value.isEmpty()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            log.warn("[Credential] L2 write failed key={}", key, e);
        }
    }

    private List<CredentialPolicyConfigVO> parse(String raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(raw, LIST_TYPE);
        } catch (Exception e) {
            log.warn("[Credential] L2 deserialize failed, raw={}", raw, e);
            return null;
        }
    }
}
