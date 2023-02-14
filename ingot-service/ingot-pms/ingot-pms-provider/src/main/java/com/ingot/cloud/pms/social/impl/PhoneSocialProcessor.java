package com.ingot.cloud.pms.social.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.cloud.pms.social.SocialProcessor;
import com.ingot.framework.common.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.ingot.framework.core.constants.SocialConstants.BEAN_PHONE;

/**
 * <p>Description  : PhoneSocialProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/4.</p>
 * <p>Time         : 9:55 上午.</p>
 */
@Component(BEAN_PHONE)
@RequiredArgsConstructor
public class PhoneSocialProcessor implements SocialProcessor {
    private final SysUserService sysUserService;

    @Override
    public String uniqueID(String code) {
        return code;
    }

    @Override
    public SysUser info(String uniqueID) {
        return sysUserService.getOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPhone, uniqueID));
    }

    @Override
    public void bind(SysUser user, String uniqueID) {
        user.setPhone(uniqueID);
        user.setUpdatedAt(DateUtils.now());
        user.updateById();
    }
}
