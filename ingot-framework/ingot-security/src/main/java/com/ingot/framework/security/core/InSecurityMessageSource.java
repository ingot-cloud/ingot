package com.ingot.framework.security.core;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;

/**
 * <p>Description  : Security Message Source.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/28.</p>
 * <p>Time         : 10:00 PM.</p>
 */
public class InSecurityMessageSource extends ReloadableResourceBundleMessageSource {

    public InSecurityMessageSource() {
        setBasename("classpath:i18n/security/messages");
        setDefaultEncoding(StandardCharsets.UTF_8.name());
    }

    public static MessageSourceAccessor getAccessor() {
        return new MessageSourceAccessor(new InSecurityMessageSource());
    }

}