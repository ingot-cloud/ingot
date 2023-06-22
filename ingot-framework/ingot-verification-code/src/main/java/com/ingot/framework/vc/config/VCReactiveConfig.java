package com.ingot.framework.vc.config;

import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCSendChecker;
import com.ingot.framework.vc.module.reactive.*;
import com.ingot.framework.vc.properties.IngotVCProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

/**
 * <p>Description  : VCReactiveConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/20.</p>
 * <p>Time         : 3:27 PM.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class VCReactiveConfig {

    @Bean
    public VCVerifyResolver vcVerifyResolver(ReactiveWebApplicationContext applicationContext,
                                             IngotVCProperties properties) {
        return new VCVerifyResolver(applicationContext, properties);
    }

    @Bean
    @ConditionalOnMissingBean(VCProcessorManager.class)
    public VCProcessorManager vcProcessorManager(Map<String, VCProcessor> processorMap,
                                                 Map<String, VCGenerator> generatorMap,
                                                 Map<String, VCSendChecker> checkerMap) {
        return new DefaultVCProcessorManager(processorMap, generatorMap, checkerMap);
    }

    @Bean
    public VCWebFilter vcWebFilter(VCProcessorManager processorManager,
                                   VCVerifyResolver verifyResolver) {
        return new VCWebFilter(processorManager, verifyResolver);
    }
}
