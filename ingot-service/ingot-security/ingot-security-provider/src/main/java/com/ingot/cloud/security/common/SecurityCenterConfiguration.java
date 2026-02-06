package com.ingot.cloud.security.common;

import com.ingot.framework.core.config.MessageSourceConfig;
import com.ingot.framework.core.context.InMessageSource;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 安全中心配置
 *
 * @author jy
 * @since 2026/2/2
 */
@Configuration
@AutoConfigureBefore(MessageSourceConfig.class)
public class SecurityCenterConfiguration {

    @Bean
    public InMessageSource messageSource() {
        return new SecurityCenterMessageSource();
    }
}
