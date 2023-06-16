package com.ingot.framework.vc.module.email;

import cn.hutool.core.util.RandomUtil;
import com.ingot.framework.vc.VCGenerator;
import com.ingot.framework.vc.common.VC;
import com.ingot.framework.vc.common.VCType;
import com.ingot.framework.vc.properties.EmailCodeProperties;
import lombok.RequiredArgsConstructor;

/**
 * <p>Description  : DefaultEmailVCGenerator.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/15.</p>
 * <p>Time         : 4:45 PM.</p>
 */
@RequiredArgsConstructor
public class DefaultEmailVCGenerator implements VCGenerator {
    private final EmailCodeProperties properties;

    @Override
    public VC generate() {
        String code = RandomUtil.randomNumbers(properties.getLength());
        return VC.instance(VCType.EMAIL, code, properties.getExpireIn());
    }
}
