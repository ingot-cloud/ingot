package com.ingot.framework.core.model.status;

/**
 * <p>Description  : StatusCode.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/10.</p>
 * <p>Time         : 2:00 下午.</p>
 */
public interface ErrorCode {
    /**
     * 状态码
     * @return String
     */
    String getCode();

    /**
     * 状态文本
     * @return String
     */
    String getText();
}
