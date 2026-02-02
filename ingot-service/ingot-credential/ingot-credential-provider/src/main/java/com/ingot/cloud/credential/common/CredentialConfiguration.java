package com.ingot.cloud.credential.common;

import com.ingot.framework.core.config.MessageSourceConfig;
import com.ingot.framework.core.context.InMessageSource;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * CredentialConfiguration
 *
 * @author jy
 * @since 2026/2/2
 */
@Configuration
@AutoConfigureBefore(MessageSourceConfig.class)
public class CredentialConfiguration {

    @Bean
    public InMessageSource messageSource() {
        return new CredentialMessageSource();
    }
}
