package com.ingot.framework.vc.module.sms;

import com.ingot.framework.vc.common.VC;

/**
 * <p>Description  : SmsCodeSender.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/18.</p>
 * <p>Time         : 5:22 PM.</p>
 */
public interface SmsCodeSender {

    /**
     * 发送验证码
     *
     * @param phone 手机号
     * @param code  验证码
     * @param ip    IP
     */
    void send(String phone, String ip, VC code);
}
