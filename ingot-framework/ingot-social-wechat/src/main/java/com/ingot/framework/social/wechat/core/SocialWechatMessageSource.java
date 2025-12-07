package com.ingot.framework.social.wechat.core;

import java.nio.charset.StandardCharsets;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * <p>Description  : SocialWechatMessageSource.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/7.</p>
 * <p>Time         : 12:38.</p>
 */
public class SocialWechatMessageSource extends ReloadableResourceBundleMessageSource {

    public SocialWechatMessageSource() {
        setBasename("classpath:i18n/social/wechat/messages");
        setDefaultEncoding(StandardCharsets.UTF_8.name());
    }

    public static MessageSourceAccessor getAccessor() {
        return new MessageSourceAccessor(new SocialWechatMessageSource());
    }
}
