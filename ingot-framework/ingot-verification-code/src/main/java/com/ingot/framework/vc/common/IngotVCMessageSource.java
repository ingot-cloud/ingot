package com.ingot.framework.vc.common;

import java.nio.charset.StandardCharsets;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * <p>Description  : IngotVCMessageSource.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 11:30 AM.</p>
 */
public class IngotVCMessageSource extends ReloadableResourceBundleMessageSource {

    public IngotVCMessageSource() {
        setBasename("classpath:i18n/vc/messages");
        setDefaultEncoding(StandardCharsets.UTF_8.name());
    }

    public static MessageSourceAccessor getAccessor() {
        return new MessageSourceAccessor(new IngotVCMessageSource());
    }

}