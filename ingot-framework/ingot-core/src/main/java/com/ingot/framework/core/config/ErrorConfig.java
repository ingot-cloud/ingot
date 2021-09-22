package com.ingot.framework.core.config;

import com.ingot.framework.core.error.IngotErrorAttributes;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : ErrorConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/22.</p>
 * <p>Time         : 2:42 下午.</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@AutoConfigureBefore(ErrorMvcAutoConfiguration.class)
public class ErrorConfig {

    @Bean
    public ErrorAttributes errorAttributes() {
        return new IngotErrorAttributes();
    }
}
