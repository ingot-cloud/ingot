package com.ingot.framework.vc.config;

import java.util.Map;

import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCRepository;
import com.ingot.framework.vc.common.DefaultVCRepository;
import com.ingot.framework.vc.module.servlet.DefaultVCProviderManager;
import com.ingot.framework.vc.module.servlet.VCProvider;
import com.ingot.framework.vc.module.servlet.VCProviderManager;
import com.ingot.framework.vc.properties.IngotVCProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.RedisTemplate;

/**
 * <p>Description  : VCConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/3/21.</p>
 * <p>Time         : 10:07 PM.</p>
 */
@AutoConfiguration
@EnableConfigurationProperties(IngotVCProperties.class)
public class VCConfig {

    @Bean
    @ConditionalOnMissingBean(VCRepository.class)
    public VCRepository repository(RedisTemplate<String, Object> redisTemplate) {
        return new DefaultVCRepository(redisTemplate);
    }

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public VCProviderManager vcProviderManager(Map<String, VCProvider> providerMap,
                                               Map<String, VCGenerator> generatorMap) {
        return new DefaultVCProviderManager(providerMap, generatorMap);
    }


}
