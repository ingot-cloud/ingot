package com.ingot.framework.vc;

/**
 * <p>Description  : VCSendChecker.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/28.</p>
 * <p>Time         : 1:30 PM.</p>
 */
public interface VCSendChecker {

    /**
     * 发送验证码校验
     *
     * @param receiver 验证码接收者
     * @param remoteIP 远端IP
     */
    void check(String receiver, String remoteIP);
}
