package com.ingot.framework.core.validation.service;

/**
 * <p>Description  : AssertI18Service.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/5/28.</p>
 * <p>Time         : 8:46 上午.</p>
 */
public interface AssertI18nService {

    /**
     * 检测数据操作，如果表达式为 false，则抛出 OperationException 异常
     * @param expression 表达式结果
     * @param i18nCode i18n key
     */
    void checkOperation(boolean expression, String i18nCode);
}
