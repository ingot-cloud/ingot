package com.ingot.cloud.auth.web;

import java.util.Map;

import com.ingot.framework.security.config.annotation.web.configuration.Permit;
import com.ingot.framework.security.oauth2.jwt.JwkSupplier;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>Description  : JWK Set 端点，提供公钥用于验证 JWT.</p>
 * <p>符合 OAuth 2.0 规范，暴露 /.well-known/jwks.json 端点</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/6.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class JwksEndpoint {
    
    private final JwkSupplier jwkSupplier;
    
    /**
     * JWK Set 端点
     * 返回所有活跃的公钥（用于验证 JWT）
     * 
     * @return JWK Set JSON
     */
    @Permit  // 公开访问，无需认证
    @GetMapping(value = "/.well-known/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> jwks() {
        try {
            JWKSet jwkSet = jwkSupplier.get();
            
            // 移除私钥，只返回公钥部分
            JWKSet publicJwkSet = jwkSet.toPublicJWKSet();
            
            log.debug("[JwksEndpoint] Serving JWK Set with {} key(s)", publicJwkSet.getKeys().size());
            
            return publicJwkSet.toJSONObject();
            
        } catch (Exception e) {
            log.error("[JwksEndpoint] Failed to serve JWK Set", e);
            throw new RuntimeException("Failed to serve JWK Set", e);
        }
    }
}

