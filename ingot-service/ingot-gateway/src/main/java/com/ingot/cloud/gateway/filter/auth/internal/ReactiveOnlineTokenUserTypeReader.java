package com.ingot.cloud.gateway.filter.auth.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.constants.RedisKeyConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;

/**
 * <p>按 JWT {@code jti} 从 Redis OnlineToken JSON 读取 {@code userType}，供网关身份链路补全瘦身 JWT。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see RedisKeyConstants.OnlineToken
 */
@Slf4j
@Component
public class ReactiveOnlineTokenUserTypeReader {

    private static final String FIELD_USER_TYPE = "userType";

    private final ObjectProvider<ReactiveStringRedisTemplate> redisProvider;
    private final ObjectMapper objectMapper;

    public ReactiveOnlineTokenUserTypeReader(ObjectProvider<ReactiveStringRedisTemplate> redisProvider) {
        this.redisProvider = redisProvider;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 读取 OnlineToken.userType；miss / 解析失败 / Redis 不可用返回 empty。
     *
     * @param jti JWT ID
     * @return userType（如 {@code 0}/{@code 1}），无值时 empty
     */
    public Mono<String> readUserType(String jti) {
        if (!StringUtils.hasText(jti)) {
            return Mono.empty();
        }
        ReactiveStringRedisTemplate redis = redisProvider.getIfAvailable();
        if (redis == null) {
            return Mono.empty();
        }
        String key = RedisKeyConstants.OnlineToken.jtiKey(jti);
        return redis.opsForValue().get(key)
                .flatMap(this::extractUserType)
                .onErrorResume(ex -> {
                    log.warn("[OnlineTokenUserType] read fail-open jti={}: {}", jti, ex.toString());
                    return Mono.empty();
                });
    }

    private Mono<String> extractUserType(String json) {
        if (!StringUtils.hasText(json)) {
            return Mono.empty();
        }
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode object = unwrapTypedJson(root);
            if (object == null || !object.hasNonNull(FIELD_USER_TYPE)) {
                return Mono.empty();
            }
            String userType = object.get(FIELD_USER_TYPE).asText(null);
            return StringUtils.hasText(userType) ? Mono.just(userType.trim()) : Mono.empty();
        } catch (Exception ex) {
            log.debug("[OnlineTokenUserType] parse json failed: {}", ex.toString());
            return Mono.empty();
        }
    }

    /**
     * GenericJackson2JsonRedisSerializer + DefaultTyping.PROPERTY 产出带 {@code @class} 的对象；
     * WRAPPER_ARRAY 形态则为 {@code ["className", { ... }]}。
     */
    private static JsonNode unwrapTypedJson(JsonNode root) {
        if (root == null || root.isNull()) {
            return null;
        }
        if (root.isObject()) {
            return root;
        }
        if (root.isArray() && root.size() >= 2 && root.get(1).isObject()) {
            return root.get(1);
        }
        return null;
    }
}
