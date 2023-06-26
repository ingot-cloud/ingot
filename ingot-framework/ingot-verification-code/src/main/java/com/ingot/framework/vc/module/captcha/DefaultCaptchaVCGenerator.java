package com.ingot.framework.vc.module.captcha;

import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.common.VC;
import com.ingot.framework.vc.common.VCType;
import com.ingot.framework.vc.properties.ImageCodeProperties;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DefaultCaptchaVCGenerator.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/25.</p>
 * <p>Time         : 3:08 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultCaptchaVCGenerator implements VCGenerator {
    private final ImageCodeProperties properties;

    @Override
    public VC generate() {
        return VC.instance(VCType.IMAGE, "1", 1);
    }
}
