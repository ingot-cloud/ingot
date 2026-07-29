package com.ingot.framework.security.account.domain.config;

import java.nio.charset.StandardCharsets;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * AccountMessageSource
 *
 * @author jy
 * @since 2026/3/2
 */
public class AccountMessageSource extends ReloadableResourceBundleMessageSource {

    public AccountMessageSource() {
        setBasename("classpath:i18n/account/messages");
        setDefaultEncoding(StandardCharsets.UTF_8.name());
    }

    public static MessageSourceAccessor getAccessor() {
        return new MessageSourceAccessor(new AccountMessageSource());
    }
}
