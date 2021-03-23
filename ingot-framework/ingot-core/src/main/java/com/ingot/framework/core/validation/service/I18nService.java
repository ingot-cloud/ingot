package com.ingot.framework.core.validation.service;

import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;

import java.util.Locale;

/**
 * <p>Description  : I18nService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/23.</p>
 * <p>Time         : 10:18 上午.</p>
 */
@AllArgsConstructor
public class I18nService {
    private final MessageSource messageSource;
    private static final Object[] EMPTY = new Object[0];

    public String getMessage(String code) {
        return getMessage(code, EMPTY);
    }

    public String getMessage(String code, String defaultMessage) {
        return getMessage(code, EMPTY, defaultMessage);
    }

    public String getMessage(String code, @Nullable Object[] args) {
        return getMessage(code, args, LocaleContextHolder.getLocale());
    }

    public String getMessage(String code, @Nullable Object[] args, String defaultMessage) {
        return getMessage(code, args, defaultMessage, LocaleContextHolder.getLocale());
    }

    public String getMessage(String code, @Nullable Object[] args, Locale locale) {
        return messageSource.getMessage(code, args, locale);
    }

    public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }

}
