package com.ingot.cloud.pms.social.impl;

import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.social.SocialProcessor;
import com.ingot.framework.core.model.dto.user.UserDetailsDto;
import org.springframework.stereotype.Component;

import static com.ingot.framework.core.constants.SocialConstants.BEAN_WECHAT;

/**
 * <p>Description  : WechatSocialProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/4.</p>
 * <p>Time         : 9:58 上午.</p>
 */
@Component(BEAN_WECHAT)
public class WechatSocialProcessor implements SocialProcessor {

    @Override
    public SysUser exec(UserDetailsDto params) {
        return null;
    }
}
