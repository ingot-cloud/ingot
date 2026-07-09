package com.ingot.framework.security.crypto;

import com.ingot.framework.security.crypto.hybrid.HybridCryptoService;
import com.ingot.framework.security.crypto.hybrid.HybridKeyManager;
import com.ingot.framework.security.crypto.web.DefaultSecretKeyResolver;
import com.ingot.framework.security.crypto.web.HybridPublicKeyController;
import com.ingot.framework.security.crypto.web.SecretKeyResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * <p>Description  : 加密配置.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 10:25 AM.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(InCryptoProperties.class)
public class InCryptoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecretKeyResolver.class)
    public SecretKeyResolver defaultSecretKeyResolver(InCryptoProperties properties) {
        return new DefaultSecretKeyResolver(properties);
    }

    @Bean
    @ConditionalOnMissingBean(HybridKeyManager.class)
    public HybridKeyManager hybridKeyManager(InCryptoProperties properties) {
        return new HybridKeyManager(properties);
    }

    @Bean
    @ConditionalOnMissingBean(HybridCryptoService.class)
    public HybridCryptoService hybridCryptoService() {
        return new HybridCryptoService();
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(prefix = "ingot.crypto.hybrid", name = "public-key-endpoint-enabled",
            havingValue = "true", matchIfMissing = true)
    @ConditionalOnMissingBean(HybridPublicKeyController.class)
    public HybridPublicKeyController hybridPublicKeyController(HybridKeyManager hybridKeyManager) {
        return new HybridPublicKeyController(hybridKeyManager);
    }
}
