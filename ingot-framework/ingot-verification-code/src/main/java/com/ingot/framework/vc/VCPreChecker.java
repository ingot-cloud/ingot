package com.ingot.framework.vc;

/**
 * <p>Description  : 验证码操作前置检查器.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/28.</p>
 * <p>Time         : 1:30 PM.</p>
 */
public interface VCPreChecker {

    /**
     * 发送验证码前校验
     *
     * @param receiver 验证码接收者
     * @param remoteIP 远端IP
     */
    void beforeSend(String receiver, String remoteIP);

    /**
     * 校验验证码前校验
     * @param remoteIP 远端IP
     */
    void beforeCheck(String remoteIP);
}
