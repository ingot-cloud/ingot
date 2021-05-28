package com.ingot.framework.core.validation.service.impl;

import com.ingot.framework.core.utils.AssertionUtils;
import com.ingot.framework.core.validation.service.AssertI18nService;
import com.ingot.framework.core.validation.service.I18nService;
import lombok.AllArgsConstructor;

/**
 * <p>Description  : AssertI18nServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/28.</p>
 * <p>Time         : 8:53 上午.</p>
 */
@AllArgsConstructor
public class AssertI18nServiceImpl implements AssertI18nService {
    private final I18nService i18nService;

    @Override
    public void checkOperation(boolean expression, String i18Code) {
        AssertionUtils.checkOperation(expression, i18nService.getMessage(i18Code));
    }
}
