package com.ingot.framework.security.oauth2.server.authorization;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.utils.DigestUtil;
import com.ingot.framework.security.core.userdetails.InUser;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2PreAuthorizationCodeRequestAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.authentication.OAuth2UserDetailsAuthenticationToken;
import com.ingot.framework.security.oauth2.server.authorization.jackson2.RedisObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.lang.Nullable;
import org.springframework.security.oauth2.core.OAuth2DeviceCode;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.core.OAuth2UserCode;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationCode;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.util.Assert;

/**
 * Redis实现的OAuth2AuthorizationService <br/>
 * 优化点： <br/>
 * 1. 使用 AuthorizationSnapshot 避免直接序列化 OAuth2Authorization <br/>
 * 2. 完整索引策略：支持Authorization Code、State、AccessToken、RefreshToken <br/>
 * 3. 按需TTL：授权码5分钟，AccessToken根据配置 <br/>
 * 4. 集成OnlineTokenService存储在线Token信息 <br/>
 * <br/>
 * Redis Key结构： <br/>
 * - oauth2:auth:{authorizationId} → AuthorizationSnapshot（主数据，普通POJO） <br/>
 * - oauth2:token:{tokenHash} → authorizationId（索引，支持所有Token类型） <br/>
 *
 * <p>Author: jy</p>
 * <p>Date: 2025/12/17</p>
 */
@Slf4j
public class RedisOAuth2AuthorizationService implements OAuth2AuthorizationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final OnlineTokenService onlineTokenService;
    private final RegisteredClientRepository registeredClientRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Redis Key前缀
    private static final String AUTHORIZATION_PREFIX = "oauth2:auth:";
    private static final String TOKEN_INDEX_PREFIX = "oauth2:token:";

    public RedisOAuth2AuthorizationService(
            RedisTemplate<String, Object> redisTemplate,
            OnlineTokenService onlineTokenService,
            RegisteredClientRepository registeredClientRepository) {
        Assert.notNull(redisTemplate, "redisTemplate cannot be null");
        Assert.notNull(onlineTokenService, "onlineTokenService cannot be null");
        Assert.notNull(registeredClientRepository, "registeredClientRepository cannot be null");

        this.redisTemplate = redisTemplate;
        this.onlineTokenService = onlineTokenService;
        this.registeredClientRepository = registeredClientRepository;

        // 初始化 ObjectMapper
        // 启用「类型信息」反序列化，用于 Redis 存储数据
        RedisObjectMapper.config(objectMapper);
    }

    @Override
    public void save(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");

        String authorizationId = authorization.getId();

        // 1. 保存主数据
        String key = AUTHORIZATION_PREFIX + authorizationId;
        long ttl = calculateTTL(authorization);

        // 转换为 Snapshot 并存储（避免直接序列化 OAuth2Authorization）
        AuthorizationSnapshot snapshot = AuthorizationSnapshotMapper.toSnapshot(authorization);
        try {
            String snapshotJson = objectMapper.writeValueAsString(snapshot);
            redisTemplate.opsForValue().set(key, snapshotJson, ttl, TimeUnit.SECONDS);
            log.debug("[RedisOAuth2AuthorizationService] Saved OAuth2Authorization: id={}, ttl={}s", authorizationId, ttl);
        } catch (JsonProcessingException e) {
            log.error("[RedisOAuth2AuthorizationService] Failed to save OAuth2Authorization: id={}, ttl={}s", authorizationId, ttl, e);
        }

        // 2. 创建最小索引
        saveMinimalIndexes(authorization, ttl);

        log.debug("[RedisOAuth2AuthorizationService] Saved OAuth2Authorization: id={}, ttl={}s", authorizationId, ttl);
    }

    @Override
    public void remove(OAuth2Authorization authorization) {
        Assert.notNull(authorization, "authorization cannot be null");

        String authorizationId = authorization.getId();

        // 1. 删除主数据
        String key = AUTHORIZATION_PREFIX + authorizationId;
        redisTemplate.delete(key);

        // 2. 删除索引
        removeIndexes(authorization);

        // 3. 删除在线Token信息
        String jti = extractJti(authorization);
        if (StrUtil.isNotEmpty(jti)) {
            onlineTokenService.removeByJti(jti);
        }

        log.debug("[RedisOAuth2AuthorizationService] Removed OAuth2Authorization: id={}", authorizationId);
    }

    @Override
    @Nullable
    public OAuth2Authorization findById(String id) {
        Assert.hasText(id, "id cannot be empty");

        String key = AUTHORIZATION_PREFIX + id;
        Object value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            log.debug("[RedisOAuth2AuthorizationService] OAuth2Authorization not found: id={}", id);
            return null;
        }

        try {
            String snapshotJson = value.toString();
            AuthorizationSnapshot snapshot = objectMapper.readValue(snapshotJson, AuthorizationSnapshot.class);
            return AuthorizationSnapshotMapper.fromSnapshot(snapshot, registeredClientRepository);
        } catch (JsonProcessingException e) {
            log.error("[RedisOAuth2AuthorizationService] Failed to read value: id={}", id, e);
        }

        log.error("[RedisOAuth2AuthorizationService] Unexpected value type: id={}, type={}",
                id, value.getClass().getName());
        return null;
    }

    @Override
    @Nullable
    public OAuth2Authorization findByToken(String token, @Nullable OAuth2TokenType tokenType) {
        Assert.hasText(token, "token cannot be empty");

        // 通过 Token 索引查找 authorizationId
        String tokenHash = DigestUtil.sha256(token);
        String indexKey = TOKEN_INDEX_PREFIX + tokenHash;
        Object authorizationIdObj = redisTemplate.opsForValue().get(indexKey);

        if (authorizationIdObj == null) {
            log.debug("[RedisOAuth2AuthorizationService] AuthorizationId not found by token: type={}",
                    tokenType != null ? tokenType.getValue() : "unknown");
            return null;
        }

        String authorizationId = authorizationIdObj.toString();
        OAuth2Authorization authorization = findById(authorizationId);

        if (authorization != null) {
            log.debug("[RedisOAuth2AuthorizationService] Found OAuth2Authorization by token: id={}, type={}",
                    authorizationId, tokenType != null ? tokenType.getValue() : "unknown");
        }

        return authorization;
    }

    /**
     * 最小索引策略：为所有Token类型创建索引
     * Spring Authorization Server会通过不同类型的token查找Authorization
     */
    private void saveMinimalIndexes(OAuth2Authorization authorization, long ttl) {
        String authorizationId = authorization.getId();

        // 1. Authorization Code索引（授权码模式必需）
        OAuth2Authorization.Token<?> token = authorization.getToken(OAuth2AuthorizationCode.class);
        if (token != null) {
            OAuth2Token code = token.getToken();
            String tokenHash = DigestUtil.sha256(code.getTokenValue());
            String tokenKey = TOKEN_INDEX_PREFIX + tokenHash;
            // 授权码 TTL 较短，通常5分钟
            long codeTtl = Math.min(ttl, 300);
            redisTemplate.opsForValue().set(tokenKey, authorizationId, codeTtl, TimeUnit.SECONDS);
            log.debug("[RedisOAuth2AuthorizationService] Saved AuthorizationCode index");
        }

        // 2. State索引（授权码模式必需，用于CSRF保护）
        String state = authorization.getAttribute("state");
        if (StrUtil.isNotEmpty(state)) {
            String stateHash = DigestUtil.sha256(state);
            String stateKey = TOKEN_INDEX_PREFIX + stateHash;
            // State 的 TTL 与授权码相同
            long stateTtl = Math.min(ttl, 300);
            redisTemplate.opsForValue().set(stateKey, authorizationId, stateTtl, TimeUnit.SECONDS);
            log.debug("[RedisOAuth2AuthorizationService] Saved State index");
        }

        // 3. AccessToken索引（必需：资源服务器验证）
        if (authorization.getAccessToken() != null) {
            String tokenValue = authorization.getAccessToken().getToken().getTokenValue();
            String tokenHash = DigestUtil.sha256(tokenValue);
            String tokenKey = TOKEN_INDEX_PREFIX + tokenHash;
            redisTemplate.opsForValue().set(tokenKey, authorizationId, ttl, TimeUnit.SECONDS);
            log.debug("[RedisOAuth2AuthorizationService] Saved AccessToken index");
        }

        // 4. RefreshToken索引（刷新令牌流程必需）
        if (authorization.getRefreshToken() != null) {
            String tokenValue = authorization.getRefreshToken().getToken().getTokenValue();
            String tokenHash = DigestUtil.sha256(tokenValue);
            String tokenKey = TOKEN_INDEX_PREFIX + tokenHash;
            // RefreshToken 的 TTL 通常更长
            redisTemplate.opsForValue().set(tokenKey, authorizationId, ttl, TimeUnit.SECONDS);
            log.debug("[RedisOAuth2AuthorizationService] Saved RefreshToken index");
        }

        // 5. OidcToken索引（必需：资源服务器验证）
        OAuth2Authorization.Token<?> oidcToken = authorization.getToken(OidcIdToken.class);
        if (oidcToken != null) {
            String tokenValue = oidcToken.getToken().getTokenValue();
            String tokenHash = DigestUtil.sha256(tokenValue);
            String tokenKey = TOKEN_INDEX_PREFIX + tokenHash;
            redisTemplate.opsForValue().set(tokenKey, authorizationId, ttl, TimeUnit.SECONDS);
            log.debug("[RedisOAuth2AuthorizationService] Saved OidcIdToken index");
        }

        // 6. UserCodeToken 索引（用户码模式必需）
        OAuth2Authorization.Token<?> userCodeToken = authorization.getToken(OAuth2UserCode.class);
        if (userCodeToken != null) {
            OAuth2Token code = userCodeToken.getToken();
            String tokenHash = DigestUtil.sha256(code.getTokenValue());
            String tokenKey = TOKEN_INDEX_PREFIX + tokenHash;
            redisTemplate.opsForValue().set(tokenKey, authorizationId, ttl, TimeUnit.SECONDS);
            log.debug("[RedisOAuth2AuthorizationService] Saved UserCodeToken index");
        }
    }

    /**
     * 删除所有索引
     */
    private void removeIndexes(OAuth2Authorization authorization) {
        // 1. 删除Authorization Code索引
        OAuth2Authorization.Token<?> authCodeToken = authorization.getToken(OAuth2AuthorizationCode.class);
        if (authCodeToken != null) {
            OAuth2Token code = authCodeToken.getToken();
            String tokenHash = DigestUtil.sha256(code.getTokenValue());
            String tokenKey = TOKEN_INDEX_PREFIX + tokenHash;
            redisTemplate.delete(tokenKey);
            log.debug("[RedisOAuth2AuthorizationService] Removed AuthorizationCode index");
        }

        // 2. 删除State索引
        String state = authorization.getAttribute("state");
        if (StrUtil.isNotEmpty(state)) {
            String stateHash = DigestUtil.sha256(state);
            String stateKey = TOKEN_INDEX_PREFIX + stateHash;
            redisTemplate.delete(stateKey);
            log.debug("[RedisOAuth2AuthorizationService] Removed State index");
        }

        // 3. 删除AccessToken索引
        if (authorization.getAccessToken() != null) {
            String tokenValue = authorization.getAccessToken().getToken().getTokenValue();
            String tokenHash = DigestUtil.sha256(tokenValue);
            String tokenKey = TOKEN_INDEX_PREFIX + tokenHash;
            redisTemplate.delete(tokenKey);
            log.debug("[RedisOAuth2AuthorizationService] Removed AccessToken index");
        }

        // 4. 删除RefreshToken索引
        if (authorization.getRefreshToken() != null) {
            String tokenValue = authorization.getRefreshToken().getToken().getTokenValue();
            String tokenHash = DigestUtil.sha256(tokenValue);
            String tokenKey = TOKEN_INDEX_PREFIX + tokenHash;
            redisTemplate.delete(tokenKey);
            log.debug("[RedisOAuth2AuthorizationService] Removed RefreshToken index");
        }

        // 5. 删除OidcIdToken索引
        OAuth2Authorization.Token<?> oidcToken = authorization.getToken(OidcIdToken.class);
        if (oidcToken != null) {
            String tokenValue = oidcToken.getToken().getTokenValue();
            String tokenHash = DigestUtil.sha256(tokenValue);
            String tokenKey = TOKEN_INDEX_PREFIX + tokenHash;
            redisTemplate.delete(tokenKey);
            log.debug("[RedisOAuth2AuthorizationService] Removed OidcIdToken index");
        }

        // 6. 删除UserCode索引
        OAuth2Authorization.Token<?> userCodeToken = authorization.getToken(OAuth2UserCode.class);
        if (userCodeToken != null) {
            OAuth2Token code = userCodeToken.getToken();
            String tokenHash = DigestUtil.sha256(code.getTokenValue());
            String tokenKey = TOKEN_INDEX_PREFIX + tokenHash;
            redisTemplate.delete(tokenKey);
            log.debug("[RedisOAuth2AuthorizationService] Removed UserCode index");
        }

        // 7. 删除DeviceCode索引（如果存在）
        OAuth2Authorization.Token<?> deviceCodeToken = authorization.getToken(OAuth2DeviceCode.class);
        if (deviceCodeToken != null) {
            OAuth2Token code = deviceCodeToken.getToken();
            String tokenHash = DigestUtil.sha256(code.getTokenValue());
            String tokenKey = TOKEN_INDEX_PREFIX + tokenHash;
            redisTemplate.delete(tokenKey);
            log.debug("[RedisOAuth2AuthorizationService] Removed DeviceCode index");
        }
    }

    /**
     * 从OAuth2Authorization中提取JTI
     */
    private String extractJti(OAuth2Authorization authorization) {
        if (authorization.getAccessToken() != null) {
            // 从 AccessToken 的 metadata 中获取 claims
            Map<String, Object> metadata = authorization.getAccessToken().getMetadata();
            if (metadata != null && metadata.containsKey("claims")) {
                Object claimsObj = metadata.get("claims");
                if (claimsObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> claimsMap = (Map<String, Object>) claimsObj;
                    if (claimsMap.containsKey("jti")) {
                        return String.valueOf(claimsMap.get("jti"));
                    }
                }
            }
        }

        // 如果找不到，尝试使用authorizationId（临时方案）
        log.warn("[RedisOAuth2AuthorizationService] JTI not found in AccessToken metadata, using authorizationId as fallback");
        return authorization.getId();
    }

    /**
     * 获取 InUser
     */
    private Optional<InUser> getUser(OAuth2Authorization authorization) {
        if (authorization == null) {
            return Optional.empty();
        }
        Object principal = authorization.getAttribute(Principal.class.getName());
        if (principal instanceof OAuth2PreAuthorizationCodeRequestAuthenticationToken preAuthToken) {
            principal = preAuthToken.getPrincipal();
        }
        if (principal instanceof OAuth2UserDetailsAuthenticationToken userDetailsToken) {
            principal = userDetailsToken.getPrincipal();
        }
        if (principal instanceof InUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    /**
     * 计算TTL（秒）
     * 策略：使用所有Token中最长的过期时间
     */
    private long calculateTTL(OAuth2Authorization authorization) {
        Instant maxExpiresAt = null;

        // 1. Authorization Code
        OAuth2Authorization.Token<?> authCodeToken = authorization.getToken(OAuth2AuthorizationCode.class);
        if (authCodeToken != null && authCodeToken.getToken().getExpiresAt() != null) {
            maxExpiresAt = getMaxInstant(maxExpiresAt, authCodeToken.getToken().getExpiresAt());
        }

        // 2. Access Token
        if (authorization.getAccessToken() != null) {
            Instant expiresAt = authorization.getAccessToken().getToken().getExpiresAt();
            if (expiresAt != null) {
                maxExpiresAt = getMaxInstant(maxExpiresAt, expiresAt);
            }
        }

        // 3. Refresh Token（通常是最长的）
        if (authorization.getRefreshToken() != null) {
            Instant expiresAt = authorization.getRefreshToken().getToken().getExpiresAt();
            if (expiresAt != null) {
                maxExpiresAt = getMaxInstant(maxExpiresAt, expiresAt);
            }
        }

        // 4. OIDC ID Token
        OAuth2Authorization.Token<?> oidcToken = authorization.getToken(OidcIdToken.class);
        if (oidcToken != null && oidcToken.getToken().getExpiresAt() != null) {
            maxExpiresAt = getMaxInstant(maxExpiresAt, oidcToken.getToken().getExpiresAt());
        }

        // 5. User Code
        OAuth2Authorization.Token<?> userCodeToken = authorization.getToken(OAuth2UserCode.class);
        if (userCodeToken != null && userCodeToken.getToken().getExpiresAt() != null) {
            maxExpiresAt = getMaxInstant(maxExpiresAt, userCodeToken.getToken().getExpiresAt());
        }

        // 6. Device Code
        OAuth2Authorization.Token<?> deviceCodeToken = authorization.getToken(OAuth2DeviceCode.class);
        if (deviceCodeToken != null && deviceCodeToken.getToken().getExpiresAt() != null) {
            maxExpiresAt = getMaxInstant(maxExpiresAt, deviceCodeToken.getToken().getExpiresAt());
        }

        // 计算TTL
        if (maxExpiresAt != null) {
            long ttl = ChronoUnit.SECONDS.between(Instant.now(), maxExpiresAt);
            return Math.max(ttl, 60); // 至少60秒
        }

        // 默认1小时
        return 3600;
    }

    /**
     * 获取两个时间中较晚的一个
     */
    private Instant getMaxInstant(Instant current, Instant candidate) {
        if (current == null) {
            return candidate;
        }
        return candidate.isAfter(current) ? candidate : current;
    }
}
