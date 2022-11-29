package com.ingot.framework.core.config;

import java.nio.charset.StandardCharsets;

import com.ingot.framework.core.context.IngotMessageSource;
import com.ingot.framework.core.context.support.IngotReloadableResourceBundleMessageSource;
import com.ingot.framework.core.validation.service.AssertI18nService;
import com.ingot.framework.core.validation.service.I18nService;
import com.ingot.framework.core.validation.service.impl.AssertI18nServiceImpl;
import com.ingot.framework.core.validation.service.impl.I18ServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

/**
 * <p>Description  : MessageConfig.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/23.</p>
 * <p>Time         : 10:26 上午.</p>
 */
@AutoConfiguration
public class MessageSourceConfig {

    public static final String BASENAME = "classpath:i18n/messages";

    @Bean
    public IngotMessageSource messageSource() {
        IngotReloadableResourceBundleMessageSource messageSource =
                new IngotReloadableResourceBundleMessageSource();
        messageSource.setBasename(BASENAME);
//        messageSource.setUseCodeAsDefaultMessage(true);
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        return messageSource;
    }

    @Bean
    public LocalValidatorFactoryBean validator(MessageSource messageSource) {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.setValidationMessageSource(messageSource);
        return validator;
    }

    @Bean
    public I18nService i18nService(MessageSource messageSource) {
        return new I18ServiceImpl(messageSource);
    }

    @Bean
    public AssertI18nService assertI18nService(I18nService i18nService) {
        return new AssertI18nServiceImpl(i18nService);
    }
}
