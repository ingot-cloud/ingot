package com.ingot.framework.vc.module.sms;

import com.ingot.framework.vc.common.VC;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : 短信验证码发送默认实现.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/28.</p>
 * <p>Time         : 12:23 PM.</p>
 */
@Slf4j
public class DefaultSmsCodeSender implements SmsCodeSender {

    @Override
    public void send(String phone, String ip, VC code) {
        log.warn("[验证码发送器] - 请配置真实的短信验证码发送器(SmsCodeSender)");
        log.info("[验证码发送器] - 向手机[{}]发送短信验证码[{}]ip={}", phone, code.getValue(), ip);
    }
}
