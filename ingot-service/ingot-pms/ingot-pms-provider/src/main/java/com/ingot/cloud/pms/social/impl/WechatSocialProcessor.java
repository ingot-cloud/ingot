package com.ingot.cloud.pms.social.impl;

import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.social.SocialProcessor;
import com.ingot.framework.core.model.enums.SocialTypeEnums;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : WechatSocialProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/4.</p>
 * <p>Time         : 9:58 上午.</p>
 */
@Component
public class WechatSocialProcessor implements SocialProcessor<SysUser> {

    @Override
    public boolean support(SocialTypeEnums socialType) {
        return false;
    }

    @Override
    public String getUniqueID(String code) {
        return null;
    }

    @Override
    public SysUser getUserInfo(String uniqueID) {
        return null;
    }

    @Override
    public void bind(SysUser user, String uniqueID) {

    }
}
