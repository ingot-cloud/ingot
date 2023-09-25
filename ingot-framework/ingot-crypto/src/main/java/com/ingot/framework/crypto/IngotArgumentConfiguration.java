package com.ingot.framework.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.crypto.web.IngotDecryptParamResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * <p>Description  : IngotArgumentConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/25.</p>
 * <p>Time         : 2:53 PM.</p>
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@RequiredArgsConstructor
public class IngotArgumentConfiguration implements WebMvcConfigurer {
    private final IngotCryptoProperties properties;
    private final ObjectMapper objectMapper;

    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
        argumentResolvers.add(new IngotDecryptParamResolver(properties, objectMapper));
    }
}
