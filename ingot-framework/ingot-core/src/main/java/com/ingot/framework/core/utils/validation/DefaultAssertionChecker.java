package com.ingot.framework.core.utils.validation;

import com.ingot.framework.core.error.exception.BizException;
import com.ingot.framework.core.context.IngotMessageSource;
import com.ingot.framework.core.utils.AssertionUtils;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : 默认 {@link AssertionChecker} 实现.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/29.</p>
 * <p>Time         : 3:11 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultAssertionChecker implements AssertionChecker {
    private final IngotMessageSource messageSource;

    @Override
    public void checkOperation(boolean expression, String code) {
        AssertionUtils.checkOperation(expression, messageSource.getMessage(code));
    }

    @Override
    public void checkOperation(boolean expression, String code, String defaultMessage) {
        AssertionUtils.checkOperation(expression, messageSource.getMessage(code, defaultMessage));
    }

    @Override
    public void checkOperation(boolean expression, String code, Object... args) {
        AssertionUtils.checkOperation(expression, messageSource.getMessage(code, args));
    }

    @Override
    public void checkBiz(boolean expression, String bizCode, String messageCode) {
        String message = messageSource.getMessage(messageCode);
        AssertionUtils.check(expression, new BizException(bizCode, message));
    }
}
