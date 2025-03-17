package com.ingot.framework.vc.common;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;

/**
 * <p>Description  : InVCMessageSource.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 11:30 AM.</p>
 */
public class InVCMessageSource extends ReloadableResourceBundleMessageSource {

    public InVCMessageSource() {
        setBasename("classpath:i18n/vc/messages");
        setDefaultEncoding(StandardCharsets.UTF_8.name());
    }

    public static MessageSourceAccessor getAccessor() {
        return new MessageSourceAccessor(new InVCMessageSource());
    }

}