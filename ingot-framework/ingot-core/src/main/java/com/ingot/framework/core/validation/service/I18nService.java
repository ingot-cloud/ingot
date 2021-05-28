package com.ingot.framework.core.validation.service;

import org.springframework.lang.Nullable;

import java.util.Locale;

/**
 * <p>Description  : I18nService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/3/23.</p>
 * <p>Time         : 10:18 上午.</p>
 */
public interface I18nService {

    String getMessage(String code);

    String getMessage(String code, String defaultMessage);

    String getMessage(String code, @Nullable Object[] args);

    String getMessage(String code, @Nullable Object[] args, String defaultMessage);

    String getMessage(String code, @Nullable Object[] args, Locale locale);

    String getMessage(String code, @Nullable Object[] args, @Nullable String defaultMessage, Locale locale);

}
