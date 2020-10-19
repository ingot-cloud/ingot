package com.ingot.framework.base.status;

/**
 * <p>Description  : StatusCode.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/10/10.</p>
 * <p>Time         : 2:00 下午.</p>
 */
public interface StatusCode {
    /**
     * 状态码
     * @return String
     */
    String code();

    /**
     * 消息
     * @return String
     */
    String message();
}
