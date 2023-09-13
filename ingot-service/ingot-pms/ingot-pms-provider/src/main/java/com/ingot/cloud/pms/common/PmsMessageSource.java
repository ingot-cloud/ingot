package com.ingot.cloud.pms.common;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;

/**
 * <p>Description  : PmsMessageSource.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/13.</p>
 * <p>Time         : 4:20 PM.</p>
 */
public class PmsMessageSource extends ReloadableResourceBundleMessageSource {
    private static final MessageSourceAccessor accessor = new MessageSourceAccessor(new PmsMessageSource());

    public PmsMessageSource() {
        setBasename("classpath:i18n/pms/messages");
        setDefaultEncoding(StandardCharsets.UTF_8.name());
    }

    public static MessageSourceAccessor getAccessor() {
        return PmsMessageSource.accessor;
    }
}
