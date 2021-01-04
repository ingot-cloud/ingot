package com.ingot.cloud.pms.social.impl;

import com.ingot.cloud.pms.model.domain.SysUser;
import com.ingot.cloud.pms.social.SocialProcessor;
import com.ingot.framework.core.model.dto.user.UserDetailsDto;
import com.ingot.framework.core.model.enums.SocialTypeEnum;
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
    public SysUser exec(long tenantId, UserDetailsDto params) {
        return null;
    }
}
