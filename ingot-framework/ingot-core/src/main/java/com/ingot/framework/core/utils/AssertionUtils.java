package com.ingot.framework.core.utils;

import com.ingot.framework.core.error.exception.BizException;
import com.ingot.framework.core.error.exception.IllegalOperationException;
import com.ingot.framework.core.model.support.R;

/**
 * <p>Description  : AssertionUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/5/24.</p>
 * <p>Time         : 4:45 PM.</p>
 */
public final class AssertionUtils {

    /**
     * 检测{@link R}，如果返回失败则抛出异常
     */
    public static void checkR(R<?> response) {
        if (!response.isSuccess()) {
            throw new BizException(response.getCode(), response.getMessage());
        }
    }

    /**
     * 检测数据操作，如果表达式为 false，则抛出 {@link IllegalOperationException}
     *
     * @param expression 表达式
     * @param message    提示信息
     */
    public static void checkOperation(boolean expression, String message) {
        check(expression, new IllegalOperationException(message));
    }

    /**
     * 检测参数，如果表达式为false，则抛出 {@link IllegalArgumentException}
     *
     * @param expression 表达式
     */
    public static void checkArgument(boolean expression) {
        check(expression, new IllegalArgumentException());
    }


    /**
     * 检测参数，如果表达式为false，则抛出 {@link IllegalArgumentException}
     *
     * @param expression 表达式
     * @param message    异常消息
     */
    public static void checkArgument(boolean expression, String message) {
        check(expression, new IllegalArgumentException(message));
    }

    /**
     * 检测表达式，如果表达式为 false，则抛出指定异常
     *
     * @param expression 表达式
     * @param e          异常
     */
    public static void check(boolean expression, RuntimeException e) {
        if (!expression) {
            throw e;
        }
    }

    /**
     * 检测表达式，如果不满足则执行Function
     *
     * @param expression 表达式
     * @param runnable   Function
     */
    public static void check(boolean expression, Runnable runnable) {
        if (!expression) {
            runnable.run();
        }
    }
}
