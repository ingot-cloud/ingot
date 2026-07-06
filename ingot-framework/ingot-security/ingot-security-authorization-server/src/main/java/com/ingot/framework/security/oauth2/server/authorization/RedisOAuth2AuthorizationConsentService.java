package com.ingot.framework.security.oauth2.server.authorization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.oauth2.server.authorization.jackson2.RedisObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsent;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;

/**
 * Redis实现的OAuth2AuthorizationConsentService
 * 使用 OAuth2AuthorizationConsentSnapshot 避免序列化问题
 *
 * <p>Author: jy</p>
 * <p>Date: 2025/12/17</p>
 */
@Slf4j
public class RedisOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final RegisteredClientRepository registeredClientRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Redis Key前缀
    private static final String CONSENT_PREFIX = "oauth2:consent:";

    public RedisOAuth2AuthorizationConsentService(
            RedisTemplate<String, Object> redisTemplate,
            RegisteredClientRepository registeredClientRepository) {
        Assert.notNull(redisTemplate, "redisTemplate cannot be null");
        Assert.notNull(registeredClientRepository, "registeredClientRepository cannot be null");

        this.redisTemplate = redisTemplate;
        this.registeredClientRepository = registeredClientRepository;

        // 初始化 ObjectMapper
        // 启用「类型信息」反序列化，用于 Redis 存储数据
        RedisObjectMapper.config(objectMapper);
    }

    @Override
    public void save(OAuth2AuthorizationConsent authorizationConsent) {
        Assert.notNull(authorizationConsent, "authorizationConsent cannot be null");

        String key = buildKey(authorizationConsent.getRegisteredClientId(), authorizationConsent.getPrincipalName());

        // 转换为 Snapshot 并存储（避免直接序列化 OAuth2AuthorizationConsent）
        OAuth2AuthorizationConsentSnapshot snapshot = OAuth2AuthorizationConsentSnapshotMapper.toSnapshot(authorizationConsent);
        try {
            String snapshotJson = objectMapper.writeValueAsString(snapshot);
            // 授权同意无需过期时间（除非用户主动撤销）
            redisTemplate.opsForValue().set(key, snapshotJson);
            log.debug("[RedisOAuth2AuthorizationConsentService] Saved consent: clientId={}, principalName={}, authorities={}",
                    authorizationConsent.getRegisteredClientId(),
                    authorizationConsent.getPrincipalName(),
                    snapshot.getAuthorities().size());
        } catch (JsonProcessingException e) {
            log.error("[RedisOAuth2AuthorizationConsentService] Failed to write value: clientId={}, principalName={}",
                    authorizationConsent.getRegisteredClientId(),
                    authorizationConsent.getPrincipalName());
        }
    }

    @Override
    public void remove(OAuth2AuthorizationConsent authorizationConsent) {
        Assert.notNull(authorizationConsent, "authorizationConsent cannot be null");

        String key = buildKey(authorizationConsent.getRegisteredClientId(), authorizationConsent.getPrincipalName());
        redisTemplate.delete(key);

        log.debug("[RedisOAuth2AuthorizationConsentService] Removed consent: clientId={}, principalName={}",
                authorizationConsent.getRegisteredClientId(), authorizationConsent.getPrincipalName());
    }

    @Override
    @Nullable
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        Assert.hasText(registeredClientId, "registeredClientId cannot be empty");
        Assert.hasText(principalName, "principalName cannot be empty");

        String key = buildKey(registeredClientId, principalName);
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            log.debug("[RedisOAuth2AuthorizationConsentService] Consent not found: clientId={}, principalName={}",
                    registeredClientId, principalName);
            return null;
        }

        try {
            String snapshotJson = value.toString();
            OAuth2AuthorizationConsentSnapshot snapshot = objectMapper.readValue(snapshotJson, OAuth2AuthorizationConsentSnapshot.class);

            // 验证 RegisteredClient 是否存在
            RegisteredClient registeredClient = this.registeredClientRepository.findById(registeredClientId);
            if (registeredClient == null) {
                throw new DataRetrievalFailureException(
                        "The RegisteredClient with id '" + registeredClientId + "' was not found in the RegisteredClientRepository.");
            }

            return OAuth2AuthorizationConsentSnapshotMapper.fromSnapshot(snapshot, registeredClientRepository);
        } catch (JsonProcessingException e) {
            log.error("[RedisOAuth2AuthorizationConsentService] Failed to read value: key={}", key, e);
        }

        log.error("[RedisOAuth2AuthorizationConsentService] Unexpected value type: clientId={}, principalName={}, type={}",
                registeredClientId, principalName, value.getClass().getName());
        return null;
    }

    /**
     * 构建Redis Key
     * 格式：oauth2:consent:{registeredClientId}:{principalName}
     */
    private String buildKey(String registeredClientId, String principalName) {
        return CONSENT_PREFIX + registeredClientId + ":" + principalName;
    }
}
