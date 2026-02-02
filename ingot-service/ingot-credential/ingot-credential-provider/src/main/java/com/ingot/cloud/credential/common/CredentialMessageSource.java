package com.ingot.cloud.credential.common;

import java.nio.charset.StandardCharsets;

import com.ingot.framework.core.context.InMessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * CredentialMessageSource
 *
 * @author jy
 * @since 2026/2/2
 */
public class CredentialMessageSource extends ReloadableResourceBundleMessageSource implements InMessageSource {
    private static final MessageSourceAccessor accessor = new MessageSourceAccessor(new CredentialMessageSource());

    public CredentialMessageSource() {
        setBasename("classpath:i18n/credential/messages");
        setDefaultEncoding(StandardCharsets.UTF_8.name());
    }

    public static MessageSourceAccessor getAccessor() {
        return CredentialMessageSource.accessor;
    }
}
