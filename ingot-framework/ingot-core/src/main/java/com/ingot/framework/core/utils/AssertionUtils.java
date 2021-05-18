package com.ingot.framework.core.utils;

import com.ingot.framework.base.exception.BizException;
import com.ingot.framework.base.exception.IllegalOperationException;
import com.ingot.framework.core.wrapper.IngotResponse;

/**
 * <p>Description  : ResponseUtils.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/5/24.</p>
 * <p>Time         : 4:45 PM.</p>
 */
public final class AssertionUtils {

    /**
     * 检测IngotResponse，如果返回失败则抛出异常
     */
    public static void checkIngotResponse(IngotResponse<?> response){
        if (!response.isSuccess()) {
            throw new BizException(response.getCode(), response.getMessage());
        }
    }

    /**
     * 检测数据操作，如果表达式为 false，则抛出 OperationException 异常
     * @param expression 表达式
     * @param message 提示信息
     */
    public static void checkOperation(boolean expression, String message){
        check(expression, new IllegalOperationException(message));
    }

    /**
     * 检测表达式，如果表达式为 false，则抛出指定异常
     * @param expression 表达式
     * @param e 异常
     */
    public static void check(boolean expression, RuntimeException e){
        if (!expression) {
            throw e;
        }
    }
}
