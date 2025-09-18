package com.ingot.framework.core.config;

import com.ingot.framework.core.utils.RequestLogAop;
import com.ingot.framework.core.utils.preconditions.PreconditionsAop;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;

/**
 * <p>Description  : AspectConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/9/6.</p>
 * <p>Time         : 4:32 PM.</p>
 */
@AutoConfiguration
public class AspectConfig {

    @Bean
    @Order(Integer.MIN_VALUE + 1)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    public RequestLogAop requestLogAop(CoreProperties properties) {
        return new RequestLogAop(properties);
    }

    @Bean
    public PreconditionsAop preconditionsAop() {
        return new PreconditionsAop();
    }
}
