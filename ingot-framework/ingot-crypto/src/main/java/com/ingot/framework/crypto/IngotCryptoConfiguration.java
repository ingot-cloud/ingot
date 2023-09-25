package com.ingot.framework.crypto;

import com.ingot.framework.crypto.web.DefaultSecretKeyResolver;
import com.ingot.framework.crypto.web.SecretKeyResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * <p>Description  : IngotCryptoConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 10:25 AM.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(IngotCryptoProperties.class)
public class IngotCryptoConfiguration {

    @Bean
    @ConditionalOnMissingBean(SecretKeyResolver.class)
    public SecretKeyResolver defaultSecretKeyResolver(IngotCryptoProperties properties) {
        return new DefaultSecretKeyResolver(properties);
    }
}
