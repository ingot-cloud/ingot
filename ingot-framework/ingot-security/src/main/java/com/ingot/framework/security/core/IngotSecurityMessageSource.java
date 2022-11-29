package com.ingot.framework.security.core;

import java.nio.charset.StandardCharsets;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * <p>Description  : IngotSecurityMessageSource.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/28.</p>
 * <p>Time         : 10:00 PM.</p>
 */
public class IngotSecurityMessageSource extends ReloadableResourceBundleMessageSource {

    public IngotSecurityMessageSource() {
        setBasename("classpath:i18n/security/messages");
        setDefaultEncoding(StandardCharsets.UTF_8.name());
    }

    public static MessageSourceAccessor getAccessor() {
        return new MessageSourceAccessor(new IngotSecurityMessageSource());
    }

}