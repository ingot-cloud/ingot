package com.ingot.cloud.pms.social.impl;

import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.social.SocialProcessor;
import org.springframework.stereotype.Component;

import static com.ingot.framework.core.constants.SocialConstants.BEAN_PHONE;

/**
 * <p>Description  : PhoneSocialProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/4.</p>
 * <p>Time         : 9:55 上午.</p>
 */
@Component(BEAN_PHONE)
public class PhoneSocialProcessor implements SocialProcessor {

    @Override
    public String uniqueID(String code) {
        return null;
    }

    @Override
    public SysUser info(String uniqueID) {
        return null;
    }

    @Override
    public void bind(SysUser user, String uniqueID) {

    }
}
