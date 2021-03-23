package com.ingot.framework.core.config;

import com.ingot.framework.core.validation.service.I18nService;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * <p>Description  : MessageConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/23.</p>
 * <p>Time         : 10:26 上午.</p>
 */
@Configuration
public class MessageConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource messageSource = new ReloadableResourceBundleMessageSource();
        messageSource.setBasename("classpath:i18n/messages");
        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

    @Bean
    public I18nService i18nService() {
        return new I18nService(messageSource());
    }
}
