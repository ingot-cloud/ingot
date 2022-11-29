package com.ingot.framework.core.validation;

import com.ingot.framework.common.exception.IllegalOperationException;

/**
 * <p>Description  : AssertionChecker.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/28.</p>
 * <p>Time         : 8:46 上午.</p>
 */
public interface AssertionChecker {

    /**
     * 检查数据操作，如果表达式为 false，则抛出 {@link IllegalOperationException}
     *
     * @param expression 表达式结果
     * @param code       message code
     */
    void checkOperation(boolean expression, String code);

    /**
     * 检查数据操作，如果表达式为 false，则抛出 {@link IllegalOperationException}
     *
     * @param expression     表达式结果
     * @param code           message code
     * @param defaultMessage 默认消息
     */
    void checkOperation(boolean expression, String code, String defaultMessage);

    /**
     * 检查数据操作，如果表达式为 false，则抛出 {@link IllegalOperationException}
     *
     * @param expression 表达式结果
     * @param code       message code
     * @param args       参数
     */
    void checkOperation(boolean expression, String code, Object... args);
}
