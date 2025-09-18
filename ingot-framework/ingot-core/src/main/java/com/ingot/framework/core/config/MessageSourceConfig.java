package com.ingot.framework.core.config;

import java.nio.charset.StandardCharsets;

import com.ingot.framework.core.context.InMessageSource;
import com.ingot.framework.core.context.support.InReloadableResourceBundleMessageSource;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.core.utils.validation.DefaultAssertionChecker;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
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
    @ConditionalOnMissingBean(InMessageSource.class)
    public InMessageSource messageSource() {
        InReloadableResourceBundleMessageSource messageSource =
                new InReloadableResourceBundleMessageSource();
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
    @ConditionalOnMissingBean(AssertionChecker.class)
    public AssertionChecker assertionChecker(InMessageSource messageSource) {
        return new DefaultAssertionChecker(messageSource);
    }
}
