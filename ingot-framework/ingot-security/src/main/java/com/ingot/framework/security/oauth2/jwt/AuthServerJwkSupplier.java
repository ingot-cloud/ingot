package com.ingot.framework.security.oauth2.jwt;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.*;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.utils.crypto.AESUtil;
import com.ingot.framework.commons.utils.crypto.RSAUtil;
import com.ingot.framework.security.core.InSecurityProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * <p>Description  : 授权服务器专用的 JWK 提供者（持有私钥，支持加密存储）.</p>
 * <p>支持密钥轮换和多密钥管理</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 */
@Slf4j
public class AuthServerJwkSupplier implements JwkSupplier {
    private final StringRedisTemplate stringRedisTemplate;
    
    // 密钥轮换配置
    private final Duration keyLifetime;
    private final Duration keyGracePeriod;
    private final int maxActiveKeys;
    
    // 是否启用加密
    private final boolean encryptionEnabled;
    private final String masterKey;
    
    public AuthServerJwkSupplier(StringRedisTemplate stringRedisTemplate,
                                 InSecurityProperties properties) {
        this.stringRedisTemplate = stringRedisTemplate;
        
        // 读取配置
        InSecurityProperties.Jwk jwkConfig = properties.getJwk();
        this.keyLifetime = jwkConfig.getKeyLifetime();
        this.keyGracePeriod = jwkConfig.getKeyGracePeriod();
        this.maxActiveKeys = jwkConfig.getMaxActiveKeys();
        this.encryptionEnabled = jwkConfig.isEnableEncryption();
        this.masterKey = jwkConfig.getMasterKey();
        
        // 验证配置
        if (encryptionEnabled && StrUtil.isEmpty(masterKey)) {
            log.warn("[AuthServerJwkSupplier] Encryption is enabled but master key is not configured. " +
                    "Please set 'ingot.security.jwk.master-key' or disable encryption. " +
                    "Private keys will NOT be encrypted!");
        }
        
        log.info("[AuthServerJwkSupplier] Initialized with keyLifetime={}, gracePeriod={}, maxKeys={}, encryption={}",
                keyLifetime, keyGracePeriod, maxActiveKeys, encryptionEnabled && StrUtil.isNotEmpty(masterKey));
    }
    
    @Override
    public JWKSet get() {
        try {
            // 获取当前活跃的密钥
            String currentKeyId = getCurrentKeyId();
            
            // 检查是否需要轮换
            if (shouldRotateKey(currentKeyId)) {
                rotateKey();
            }
            
            // 获取所有活跃密钥（用于签名的当前密钥 + 用于验证的历史密钥）
            List<RSAKey> keys = loadActiveKeys();
            
            // 如果只有一个密钥，使用单参数构造函数
            if (keys.size() == 1) {
                return new JWKSet(keys.get(0));
            }
            // 多个密钥时，使用 List 构造函数
            return new JWKSet(new ArrayList<>(keys));
        } catch (Exception e) {
            log.error("[AuthServerJwkSupplier] Failed to get JWKSet", e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 获取当前用于签名的密钥
     */
    public RSAKey getCurrentSigningKey() {
        try {
            String keyId = getCurrentKeyId();
            if (StrUtil.isEmpty(keyId)) {
                return generateAndCacheNewKey();
            }
            return loadKey(keyId, true);
        } catch (Exception e) {
            log.error("[AuthServerJwkSupplier] Failed to get current signing key", e);
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 加载所有活跃密钥（包括历史密钥用于验证）
     */
    private List<RSAKey> loadActiveKeys() throws Exception {
        List<RSAKey> keys = new ArrayList<>();
        
        // 获取所有密钥 ID
        Set<String> keyIds = stringRedisTemplate.opsForSet()
                .members(CacheConstants.Security.AUTHORIZATION_KEY_IDS);
        
        if (keyIds == null || keyIds.isEmpty()) {
            // 首次启动，生成新密钥
            keys.add(generateAndCacheNewKey());
            return keys;
        }
        
        // 加载所有活跃密钥
        for (String keyId : keyIds) {
            try {
                RSAKey key = loadKey(keyId, true);  // 授权服务器加载私钥
                if (key != null) {
                    keys.add(key);
                }
            } catch (Exception e) {
                log.warn("[AuthServerJwkSupplier] Failed to load key: {}", keyId, e);
            }
        }
        
        // 如果所有密钥都加载失败，生成新密钥
        if (keys.isEmpty()) {
            keys.add(generateAndCacheNewKey());
        }
        
        return keys;
    }
    
    /**
     * 加载指定密钥
     */
    private RSAKey loadKey(String keyId, boolean includePrivateKey) throws Exception {
        String keyPrefix = CacheConstants.Security.AUTHORIZATION_KEY_PREFIX + keyId;
        
        String pubStr = stringRedisTemplate.opsForValue().get(keyPrefix + ":pub");
        if (StrUtil.isEmpty(pubStr)) {
            return null;
        }
        
        RSAPublicKey publicKey = (RSAPublicKey) RSAUtil.getPublicKey(RSAUtil.toBytes(pubStr));
        RSAKey.Builder builder = new RSAKey.Builder(publicKey).keyID(keyId);
        
        if (includePrivateKey) {
            String priStr = stringRedisTemplate.opsForValue().get(keyPrefix + ":pri");
            
            if (StrUtil.isNotEmpty(priStr)) {
                // 检查是否加密
                String encryptedFlag = stringRedisTemplate.opsForValue().get(keyPrefix + ":encrypted");
                boolean isEncrypted = "true".equals(encryptedFlag);
                
                // 如果加密了，需要解密
                if (isEncrypted && encryptionEnabled && StrUtil.isNotEmpty(masterKey)) {
                    try {
                        priStr = AESUtil.decryptGCM(priStr, masterKey);
                    } catch (Exception e) {
                        log.error("[AuthServerJwkSupplier] Failed to decrypt private key: {}", keyId, e);
                        throw new RuntimeException("Failed to decrypt private key", e);
                    }
                }
                
                RSAPrivateKey privateKey = (RSAPrivateKey) RSAUtil.getPrivateKey(RSAUtil.toBytes(priStr));
                builder.privateKey(privateKey);
            }
        }
        
        return builder.build();
    }
    
    /**
     * 生成并缓存新密钥
     */
    private RSAKey generateAndCacheNewKey() throws Exception {
        KeyPair keyPair = RSAUtil.generateKey();
        String keyId = UUID.randomUUID().toString();
        
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        
        // 缓存密钥
        String keyPrefix = CacheConstants.Security.AUTHORIZATION_KEY_PREFIX + keyId;
        
        // 公钥直接存储（Base64）
        stringRedisTemplate.opsForValue().set(keyPrefix + ":pub",
                RSAUtil.toHexString(publicKey.getEncoded()));
        
        // 私钥处理（是否加密）
        String privateKeyStr = RSAUtil.toHexString(privateKey.getEncoded());
        boolean encrypted = false;
        
        if (encryptionEnabled && StrUtil.isNotEmpty(masterKey)) {
            try {
                privateKeyStr = AESUtil.encryptGCM(privateKeyStr, masterKey);
                encrypted = true;
                log.debug("[AuthServerJwkSupplier] Private key encrypted for keyId: {}", keyId);
            } catch (Exception e) {
                log.error("[AuthServerJwkSupplier] Failed to encrypt private key, storing unencrypted", e);
            }
        }
        
        stringRedisTemplate.opsForValue().set(keyPrefix + ":pri", privateKeyStr);
        stringRedisTemplate.opsForValue().set(keyPrefix + ":encrypted", String.valueOf(encrypted));
        stringRedisTemplate.opsForValue().set(keyPrefix + ":created",
                String.valueOf(System.currentTimeMillis()));
        
        // 添加到活跃密钥集合
        stringRedisTemplate.opsForSet().add(CacheConstants.Security.AUTHORIZATION_KEY_IDS, keyId);
        
        // 设置为当前密钥
        stringRedisTemplate.opsForValue().set(CacheConstants.Security.AUTHORIZATION_CURRENT_KEY_ID, keyId);
        
        log.info("[AuthServerJwkSupplier] Generated new key: {}, encrypted: {}", keyId, encrypted);
        
        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(keyId)
                .build();
    }
    
    /**
     * 获取当前密钥 ID
     */
    private String getCurrentKeyId() {
        return stringRedisTemplate.opsForValue()
                .get(CacheConstants.Security.AUTHORIZATION_CURRENT_KEY_ID);
    }
    
    /**
     * 检查是否需要轮换密钥
     */
    private boolean shouldRotateKey(String currentKeyId) {
        if (StrUtil.isEmpty(currentKeyId)) {
            return true;
        }
        
        String createdStr = stringRedisTemplate.opsForValue()
                .get(CacheConstants.Security.AUTHORIZATION_KEY_PREFIX + currentKeyId + ":created");
        
        if (StrUtil.isEmpty(createdStr)) {
            return true;
        }
        
        long created = Long.parseLong(createdStr);
        long age = System.currentTimeMillis() - created;
        
        return age > keyLifetime.toMillis();
    }
    
    /**
     * 轮换密钥
     */
    private synchronized void rotateKey() {
        try {
            log.info("[AuthServerJwkSupplier] Starting key rotation");
            
            // 生成新密钥
            generateAndCacheNewKey();
            
            // 清理过期的旧密钥
            cleanupOldKeys();
            
            log.info("[AuthServerJwkSupplier] Key rotation completed");
        } catch (Exception e) {
            log.error("[AuthServerJwkSupplier] Failed to rotate key", e);
        }
    }
    
    /**
     * 清理过期的旧密钥
     */
    private void cleanupOldKeys() {
        Set<String> keyIds = stringRedisTemplate.opsForSet()
                .members(CacheConstants.Security.AUTHORIZATION_KEY_IDS);
        
        if (keyIds == null || keyIds.size() <= maxActiveKeys) {
            return;
        }
        
        // 按创建时间排序，保留最新的几个密钥
        List<KeyInfo> keyInfos = new ArrayList<>();
        for (String keyId : keyIds) {
            String createdStr = stringRedisTemplate.opsForValue()
                    .get(CacheConstants.Security.AUTHORIZATION_KEY_PREFIX + keyId + ":created");
            
            if (StrUtil.isNotEmpty(createdStr)) {
                keyInfos.add(new KeyInfo(keyId, Long.parseLong(createdStr)));
            }
        }
        
        // 按时间倒序排序
        keyInfos.sort((a, b) -> Long.compare(b.created, a.created));
        
        // 删除多余的旧密钥
        for (int i = maxActiveKeys; i < keyInfos.size(); i++) {
            KeyInfo keyInfo = keyInfos.get(i);
            long age = System.currentTimeMillis() - keyInfo.created;
            
            // 只删除超过宽限期的密钥
            if (age > (keyLifetime.toMillis() + keyGracePeriod.toMillis())) {
                deleteKey(keyInfo.keyId);
                log.info("[AuthServerJwkSupplier] Deleted old key: {}, age: {} days",
                        keyInfo.keyId, age / (1000 * 60 * 60 * 24));
            }
        }
    }
    
    /**
     * 删除指定密钥
     */
    private void deleteKey(String keyId) {
        String keyPrefix = CacheConstants.Security.AUTHORIZATION_KEY_PREFIX + keyId;
        stringRedisTemplate.delete(Arrays.asList(
                keyPrefix + ":pub",
                keyPrefix + ":pri",
                keyPrefix + ":encrypted",
                keyPrefix + ":created"
        ));
        stringRedisTemplate.opsForSet().remove(CacheConstants.Security.AUTHORIZATION_KEY_IDS, keyId);
    }
    
    /**
     * 强制轮换密钥（用于管理接口）
     */
    public void forceRotateKey() {
        rotateKey();
    }
    
    private static class KeyInfo {
        String keyId;
        long created;
        
        KeyInfo(String keyId, long created) {
            this.keyId = keyId;
            this.created = created;
        }
    }
}

