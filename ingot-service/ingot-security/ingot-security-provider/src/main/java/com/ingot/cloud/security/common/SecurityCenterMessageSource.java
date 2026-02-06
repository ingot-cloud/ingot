package com.ingot.cloud.security.common;

import java.nio.charset.StandardCharsets;

import com.ingot.framework.core.context.InMessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * 安全中心 MessageSource
 *
 * @author jy
 * @since 2026/2/2
 */
public class SecurityCenterMessageSource extends ReloadableResourceBundleMessageSource implements InMessageSource {
    private static final MessageSourceAccessor accessor = new MessageSourceAccessor(new SecurityCenterMessageSource());

    public SecurityCenterMessageSource() {
        setBasename("classpath:i18n/sc/messages");
        setDefaultEncoding(StandardCharsets.UTF_8.name());
    }

    public static MessageSourceAccessor getAccessor() {
        return SecurityCenterMessageSource.accessor;
    }
}
