package com.ingot.framework.core.utils.validation;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.utils.AssertionUtil;
import com.ingot.framework.core.context.InMessageSource;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : 默认 {@link AssertionChecker} 实现.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2022/11/29.</p>
 * <p>Time         : 3:11 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultAssertionChecker implements AssertionChecker {
    private final InMessageSource messageSource;

    @Override
    public void checkOperation(boolean expression, String code) {
        checkOperation(true, expression, code);
    }

    @Override
    public void checkOperation(boolean condition, boolean expression, String code) {
        if (!condition) {
            return;
        }
        AssertionUtil.checkOperation(expression, messageSource.getMessage(code, code));
    }

    @Override
    public void checkOperation(boolean expression, String code, String defaultMessage) {
        AssertionUtil.checkOperation(expression, messageSource.getMessage(code, defaultMessage));
    }

    @Override
    public void checkOperation(boolean expression, String code, Object... args) {
        AssertionUtil.checkOperation(expression, messageSource.getMessage(code, args));
    }

    @Override
    public void checkBiz(boolean expression, String bizCode, String messageCode) {
        String message = messageSource.getMessage(messageCode);
        AssertionUtil.check(expression, new BizException(bizCode, message));
    }
}
