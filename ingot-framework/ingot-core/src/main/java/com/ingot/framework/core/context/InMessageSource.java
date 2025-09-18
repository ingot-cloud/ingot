package com.ingot.framework.core.context;

import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;

/**
 * <p>Description  : IngotMessageSource.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/29.</p>
 * <p>Time         : 2:16 PM.</p>
 */
public interface InMessageSource extends MessageSource {

    /**
     * Try to resolve the message. Treat as an error if the message can't be found.
     *
     * @param code the message code to look up, e.g. 'calculator.noRateSet'
     * @return the resolved message (never null),
     * if no corresponding message was found throw {@link NoSuchMessageException}
     */
    default String getMessage(String code) {
        return getMessage(code, null, Locale.getDefault());
    }

    /**
     * Try to resolve the message. Treat as an error if the message can't be found.
     *
     * @param code           the message code to look up, e.g. 'calculator.noRateSet'
     * @param defaultMessage default message
     * @return the resolved message or defaultMessage
     */
    default String getMessage(String code, String defaultMessage) {
        return getMessage(code, null, defaultMessage, Locale.getDefault());
    }

    /**
     * Try to resolve the message. Treat as an error if the message can't be found.
     *
     * @param code the message code to look up, e.g. 'calculator.noRateSet'
     * @param args the message arguments
     * @return the resolved message
     */
    default String getMessage(String code, Object... args) {
        return getMessage(code, args, Locale.getDefault());
    }
}
