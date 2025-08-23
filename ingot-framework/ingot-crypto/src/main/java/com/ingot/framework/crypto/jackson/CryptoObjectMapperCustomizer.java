package com.ingot.framework.crypto.jackson;

import com.ingot.framework.core.jackson.InJackson2ObjectMapperBuilderCustomizer;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * <p>Description  : CryptoObjectMapperCustomizer.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/8/23.</p>
 * <p>Time         : 08:55.</p>
 */
public class CryptoObjectMapperCustomizer implements InJackson2ObjectMapperBuilderCustomizer {
    @Override
    public void customize(Jackson2ObjectMapperBuilder builder) {
        builder.annotationIntrospector(new CryptoAnnotationIntrospector());
    }
}
