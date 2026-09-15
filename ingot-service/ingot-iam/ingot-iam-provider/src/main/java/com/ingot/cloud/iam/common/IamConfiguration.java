package com.ingot.cloud.iam.common;

import com.ingot.framework.core.config.MessageSourceConfig;
import com.ingot.framework.core.context.InMessageSource;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : IamConfiguration.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/13.</p>
 * <p>Time         : 6:13 PM.</p>
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(MessageSourceConfig.class)
public class IamConfiguration {
    @Bean
    public InMessageSource messageSource() {
        return new IamMessageSource();
    }
}
