package com.ingot.framework.vc.module.email;

import com.ingot.framework.vc.common.VC;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : DefaultEmailCodeSender.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/15.</p>
 * <p>Time         : 4:43 PM.</p>
 */
@Slf4j
public class DefaultEmailCodeSender implements EmailCodeSender {

    @Override
    public void send(String email, String ip, VC code) {
        log.warn("[验证码发送器] - 请配置真实的邮件验证码发送器(EmailCodeSender)");
        log.info("[验证码发送器] - 向Email[{}]发送邮件验证码[{}]ip={}", email, code.getValue(), ip);
    }
}
