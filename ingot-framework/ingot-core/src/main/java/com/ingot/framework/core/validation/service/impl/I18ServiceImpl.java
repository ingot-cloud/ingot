package com.ingot.framework.core.validation.service.impl;

import com.ingot.framework.core.validation.service.I18nService;
import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.lang.Nullable;

import java.util.Locale;

/**
 * <p>Description  : I18ServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/28.</p>
 * <p>Time         : 8:39 上午.</p>
 */
@AllArgsConstructor
public class I18ServiceImpl implements I18nService {
    private final MessageSource messageSource;
    private static final Object[] EMPTY = new Object[0];

    @Override
    public String getMessage(String code) {
        return getMessage(code, EMPTY);
    }

    @Override
    public String getMessage(String code, String defaultMessage) {
        return getMessage(code, EMPTY, defaultMessage);
    }

    @Override
    public String getMessage(String code, @Nullable Object[] args) {
        return getMessage(code, args, LocaleContextHolder.getLocale());
    }

    @Override
    public String getMessage(String code, @Nullable Object[] args, String defaultMessage) {
        return getMessage(code, args, defaultMessage, LocaleContextHolder.getLocale());
    }

    @Override
    public String getMessage(String code, @Nullable Object[] args, Locale locale) {
        return messageSource.getMessage(code, args, locale);
    }

    @Override
    public String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale) {
        return messageSource.getMessage(code, args, defaultMessage, locale);
    }
}
