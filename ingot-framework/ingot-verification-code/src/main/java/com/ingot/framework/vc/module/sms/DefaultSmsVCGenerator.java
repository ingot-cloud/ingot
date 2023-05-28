package com.ingot.framework.vc.module.sms;

import cn.hutool.core.util.RandomUtil;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.common.VC;
import com.ingot.framework.vc.common.VCType;
import com.ingot.framework.vc.properties.SMSCodeProperties;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : 短信验证码默认实现.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/5/28.</p>
 * <p>Time         : 12:12 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultSmsVCGenerator implements VCGenerator {
    private final SMSCodeProperties smsCodeProperties;

    @Override
    public VC generate() {
        String code = RandomUtil.randomNumbers(smsCodeProperties.getLength());
        return VC.instance(VCType.SMS, code, smsCodeProperties.getExpireIn());
    }
}
