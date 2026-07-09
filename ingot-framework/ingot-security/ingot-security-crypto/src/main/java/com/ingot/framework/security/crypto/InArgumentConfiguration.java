package com.ingot.framework.security.crypto;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.crypto.hybrid.HybridCryptoService;
import com.ingot.framework.security.crypto.hybrid.HybridKeyManager;
import com.ingot.framework.security.crypto.web.HybridCryptoInterceptor;
import com.ingot.framework.security.crypto.web.InDecryptParamResolver;
import com.ingot.framework.security.replay.ReplayGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * <p>Description  : 参数配置.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 2:53 PM.</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequiredArgsConstructor
public class InArgumentConfiguration implements WebMvcConfigurer {
    private final InCryptoProperties properties;
    private final ObjectMapper objectMapper;
    private final HybridKeyManager hybridKeyManager;
    private final HybridCryptoService hybridCryptoService;
    private final ObjectProvider<ReplayGuard> replayGuardProvider;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new InDecryptParamResolver(properties, objectMapper, hybridCryptoService));
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HybridCryptoInterceptor(
                properties, hybridKeyManager, hybridCryptoService, replayGuardProvider));
    }
}
