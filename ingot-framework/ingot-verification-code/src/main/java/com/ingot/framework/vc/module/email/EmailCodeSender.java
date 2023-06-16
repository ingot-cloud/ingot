package com.ingot.framework.vc.module.email;

import com.ingot.framework.vc.common.VC;

/**
 * <p>Description  : EmailCodeSender.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/15.</p>
 * <p>Time         : 4:38 PM.</p>
 */
public interface EmailCodeSender {

    /**
     * 发送验证码
     *
     * @param email 邮箱
     * @param code  验证码
     * @param ip    IP
     */
    void send(String email, String ip, VC code);
}
