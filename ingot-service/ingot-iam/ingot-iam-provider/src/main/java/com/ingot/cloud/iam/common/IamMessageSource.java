package com.ingot.cloud.iam.common;

import java.nio.charset.StandardCharsets;

import com.ingot.framework.core.context.InMessageSource;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

/**
 * <p>Description  : IamMessageSource.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/13.</p>
 * <p>Time         : 4:20 PM.</p>
 */
public class IamMessageSource extends ReloadableResourceBundleMessageSource implements InMessageSource {
    private static final MessageSourceAccessor accessor = new MessageSourceAccessor(new IamMessageSource());

    public IamMessageSource() {
        setBasename("classpath:i18n/iam/messages");
        setDefaultEncoding(StandardCharsets.UTF_8.name());
    }

    public static MessageSourceAccessor getAccessor() {
        return IamMessageSource.accessor;
    }
}
