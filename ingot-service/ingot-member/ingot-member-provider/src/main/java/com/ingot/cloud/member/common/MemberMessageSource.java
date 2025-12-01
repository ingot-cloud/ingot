package com.ingot.cloud.member.common;

import java.nio.charset.StandardCharsets;

import com.ingot.framework.core.context.InMessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * <p>Description  : MemberMessageSource.</p>
 * <p>Author       : jymot.</p>
 * <p>Date         : 2025/12/01.</p>
 */
public class MemberMessageSource extends ReloadableResourceBundleMessageSource implements InMessageSource {
    private static final MessageSourceAccessor accessor = new MessageSourceAccessor(new MemberMessageSource());

    public MemberMessageSource() {
        setBasename("classpath:i18n/member/messages");
        setDefaultEncoding(StandardCharsets.UTF_8.name());
    }

    public static MessageSourceAccessor getAccessor() {
        return MemberMessageSource.accessor;
    }
}

