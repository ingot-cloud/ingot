package com.ingot.cloud.member.common;

import com.ingot.framework.core.config.MessageSourceConfig;
import com.ingot.framework.core.context.InMessageSource;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * <p>Description  : MemberConfiguration.</p>
 * <p>Author       : jymot.</p>
 * <p>Date         : 2025/12/01.</p>
 */
@Configuration
@AutoConfigureBefore(MessageSourceConfig.class)
public class MemberConfiguration {

    @Bean
    public InMessageSource messageSource() {
        return new MemberMessageSource();
    }
}

