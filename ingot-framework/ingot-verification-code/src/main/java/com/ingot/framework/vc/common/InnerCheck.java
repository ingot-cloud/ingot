package com.ingot.framework.vc.common;

import com.ingot.framework.core.utils.AssertionUtils;

/**
 * <p>Description  : InnerCheck.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 1:09 PM.</p>
 */
public class InnerCheck {

    /**
     * 检查表达式，并且抛出指定消息异常
     *
     * @param expression  表达式
     * @param messageCode 消息编码
     */
    public static void check(boolean expression, String messageCode) {
        AssertionUtils.check(expression, () -> {
            throw new VCException(InVCMessageSource.getAccessor().getMessage(messageCode));
        });
    }

    /**
     * 检查包大师，并且抛出指定消息异常
     *
     * @param expression  表达式
     * @param code        状态码
     * @param messageCode 消息编码
     */
    public static void check(boolean expression, VCErrorCode code, String messageCode) {
        AssertionUtils.check(expression, () -> {
            throw new VCException(code,
                    InVCMessageSource.getAccessor().getMessage(messageCode));
        });
    }

    /**
     * 检查包大师，并且抛出指定消息异常
     *
     * @param expression  表达式
     * @param code        状态码
     * @param messageCode 消息编码
     * @param formatCode  格式化
     */
    public static void check(boolean expression, VCErrorCode code, String messageCode, String[] formatCode) {
        AssertionUtils.check(expression, () -> {
            throw new VCException(code,
                    InVCMessageSource.getAccessor().getMessage(messageCode, formatCode));
        });
    }
}
