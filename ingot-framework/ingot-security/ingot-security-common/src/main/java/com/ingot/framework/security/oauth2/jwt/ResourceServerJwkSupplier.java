package com.ingot.framework.security.oauth2.jwt;

import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.utils.crypto.RSAUtil;
import com.ingot.framework.security.core.InSecurityProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>Description  : 资源服务器专用的 JWK 提供者（只持有公钥，用于验证 JWT）.</p>
 * <p>定期从 Redis 刷新公钥，支持密钥轮换</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 */
@Slf4j
public class ResourceServerJwkSupplier implements JwkSupplier {
    private final StringRedisTemplate stringRedisTemplate;
    private final Duration cacheRefreshInterval;
    
    private volatile JWKSet cachedJwkSet;
    private volatile long lastRefreshTime = 0;
    
    public ResourceServerJwkSupplier(StringRedisTemplate stringRedisTemplate,
                                     InSecurityProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.cacheRefreshInterval = properties.getJwk().getCacheRefreshInterval();
        
        log.info("[ResourceServerJwkSupplier] Initialized with cacheRefreshInterval={}", cacheRefreshInterval);
    }
    
    @Override
    public JWKSet get() {
        // 检查缓存是否需要刷新
        long now = System.currentTimeMillis();
        if (cachedJwkSet == null || (now - lastRefreshTime) > cacheRefreshInterval.toMillis()) {
            synchronized (this) {
                // 双重检查
                if (cachedJwkSet == null || (now - lastRefreshTime) > cacheRefreshInterval.toMillis()) {
                    cachedJwkSet = loadPublicKeys();
                    lastRefreshTime = now;
                    log.debug("[ResourceServerJwkSupplier] Refreshed JWK cache");
                }
            }
        }
        
        return cachedJwkSet;
    }
    
    /**
     * 从 Redis 加载所有公钥（不包括私钥）
     */
    private JWKSet loadPublicKeys() {
        try {
            List<RSAKey> keys = new ArrayList<>();
            
            // 获取所有密钥 ID
            Set<String> keyIds = stringRedisTemplate.opsForSet()
                    .members(CacheConstants.Security.AUTHORIZATION_KEY_IDS);
            
            if (keyIds == null || keyIds.isEmpty()) {
                log.warn("[ResourceServerJwkSupplier] No keys found in Redis, please check if authorization server has started");
                return new JWKSet(Collections.emptyList());
            }
            
            // 只加载公钥
            for (String keyId : keyIds) {
                try {
                    String pubStr = stringRedisTemplate.opsForValue()
                            .get(CacheConstants.Security.AUTHORIZATION_KEY_PREFIX + keyId + ":pub");
                    
                    if (StrUtil.isNotEmpty(pubStr)) {
                        RSAPublicKey publicKey = (RSAPublicKey) RSAUtil.getPublicKey(RSAUtil.toBytes(pubStr));
                        
                        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                                .keyID(keyId)
                                .build();  // ✅ 注意：不包含私钥
                        
                        keys.add(rsaKey);
                    }
                } catch (Exception e) {
                    log.warn("[ResourceServerJwkSupplier] Failed to load public key: {}", keyId, e);
                }
            }
            
            if (keys.isEmpty()) {
                log.warn("[ResourceServerJwkSupplier] No valid public keys loaded from Redis");
                return new JWKSet(Collections.emptyList());
            } else {
                log.info("[ResourceServerJwkSupplier] Loaded {} public key(s): {}",
                        keys.size(), keys.stream().map(RSAKey::getKeyID).toList());
            }
            
            // 如果只有一个密钥，使用单参数构造函数
            if (keys.size() == 1) {
                return new JWKSet(keys.get(0));
            }
            // 多个密钥时，使用 List 构造函数
            return new JWKSet(new ArrayList<>(keys));
            
        } catch (Exception e) {
            log.error("[ResourceServerJwkSupplier] Failed to load public keys from Redis", e);
            // 返回空的 JWKSet 而不是抛异常，避免服务启动失败
            return new JWKSet(Collections.emptyList());
        }
    }
    
    /**
     * 强制刷新缓存（用于手动触发）
     */
    public void forceRefresh() {
        synchronized (this) {
            cachedJwkSet = loadPublicKeys();
            lastRefreshTime = System.currentTimeMillis();
            log.info("[ResourceServerJwkSupplier] Force refreshed JWK cache");
        }
    }
}

