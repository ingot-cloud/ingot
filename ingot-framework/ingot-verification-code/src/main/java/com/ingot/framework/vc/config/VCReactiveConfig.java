package com.ingot.framework.vc.config;

import java.util.Map;

import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.VCPreChecker;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCType;
import com.ingot.framework.vc.module.reactive.*;
import com.ingot.framework.vc.properties.InVCProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * <p>Description  : VCReactiveConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/20.</p>
 * <p>Time         : 3:27 PM.</p>
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class VCReactiveConfig {

    @Bean
    public VCVerifyResolver vcVerifyResolver(ReactiveWebApplicationContext applicationContext,
                                             InVCProperties properties) {
        return new VCVerifyResolver(applicationContext, properties);
    }

    @Bean
    @ConditionalOnMissingBean(VCProcessorManager.class)
    public VCProcessorManager vcProcessorManager(Map<String, VCProcessor> processorMap,
                                                 Map<String, VCGenerator> generatorMap,
                                                 Map<String, VCPreChecker> checkerMap) {
        return new DefaultVCProcessorManager(processorMap, generatorMap, checkerMap);
    }

    @Bean
    public VCWebFilter vcWebFilter(VCProcessorManager processorManager,
                                   VCVerifyResolver verifyResolver) {
        return new VCWebFilter(processorManager, verifyResolver);
    }

    @Bean
    public RouterFunction<ServerResponse> vcRouterFunction(VCProcessorManager processorManager) {
        return RouterFunctions.route()
                .GET(VCConstants.PATH_PREFIX + "/{type}", request -> {
                    String type = request.pathVariable("type");
                    return processorManager.handle(VCType.getEnum(type), request);
                })
                .POST(VCConstants.PATH_PREFIX + "/{type}/check", request -> {
                    String type = request.pathVariable("type");
                    return processorManager.check(VCType.getEnum(type), request);
                })
                .build();
    }
}
