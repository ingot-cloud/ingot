package com.ingot.framework.crypto;

import com.ingot.framework.core.jackson.InJackson2ObjectMapperBuilderCustomizer;
import com.ingot.framework.crypto.jackson.CryptoObjectMapperCustomizer;
import com.ingot.framework.crypto.web.DefaultSecretKeyResolver;
import com.ingot.framework.crypto.web.SecretKeyResolver;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
    public InJackson2ObjectMapperBuilderCustomizer cryptoObjectMapperCustomizer() {
        return new CryptoObjectMapperCustomizer();
    }
}
