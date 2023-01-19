package com.ingot.cloud.pms.social.impl;

import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.social.SocialProcessor;
import org.springframework.stereotype.Component;

import static com.ingot.framework.core.constants.SocialConstants.BEAN_MINI_PROGRAM;

/**
 * <p>Description  : MiniProgramSocialProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/1/19.</p>
 * <p>Time         : 10:15 AM.</p>
 */
@Component(BEAN_MINI_PROGRAM)
public class MiniProgramSocialProcessor implements SocialProcessor {

    @Override
    public SysUser exec(String code) {

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
