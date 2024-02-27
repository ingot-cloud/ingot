package com.ingot.cloud.pms.social.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.domain.SysUserSocial;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.cloud.pms.service.domain.SysUserSocialService;
import com.ingot.cloud.pms.social.SocialProcessor;
import com.ingot.cloud.pms.core.SocialUtils;
import com.ingot.framework.core.model.enums.SocialTypeEnums;
import com.ingot.framework.core.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : AdminMiniProgramSocialProcessor.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/18.</p>
 * <p>Time         : 13:32.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminMiniProgramSocialProcessor implements SocialProcessor<SysUser> {
    private final SysUserService sysUserService;
    private final SysUserSocialService sysUserSocialService;
    private final SysSocialDetailsService sysSocialDetailsService;

    @Override
    public boolean support(SocialTypeEnums socialType) {
        return socialType == SocialTypeEnums.ADMIN_MINI_PROGRAM;
    }

    @Override
    public String getUniqueID(String code) {
        return SocialUtils.getMiniProgramOpenId(sysSocialDetailsService, SocialTypeEnums.ADMIN_MINI_PROGRAM, code);
    }

    @Override
    public SysUser getUserInfo(String uniqueID) {
        SysUserSocial userSocial = sysUserSocialService.getOne(Wrappers.<SysUserSocial>lambdaQuery()
                .eq(SysUserSocial::getType, SocialTypeEnums.ADMIN_MINI_PROGRAM)
                .eq(SysUserSocial::getUniqueId, uniqueID));
        if (userSocial == null) {
            log.debug("微信小程序未绑定openId={}", uniqueID);
            return null;
        }

        return sysUserService.getById(userSocial.getUserId());
    }

    @Override
    public void bind(SysUser user, String uniqueID) {
        SysUserSocial current = sysUserSocialService.getOne(Wrappers.<SysUserSocial>lambdaQuery()
                .eq(SysUserSocial::getType, SocialTypeEnums.ADMIN_MINI_PROGRAM)
                .eq(SysUserSocial::getUniqueId, uniqueID)
                .eq(SysUserSocial::getUserId, user.getId()));
        // 如果当前存在绑定关系，那么更新绑定关系
        if (current != null) {
            current.setUserId(user.getId());
            current.setBindAt(DateUtils.now());
            sysUserSocialService.updateById(current);
            return;
        }

        SysUserSocial userSocial = new SysUserSocial();
        userSocial.setUserId(user.getId());
        userSocial.setType(SocialTypeEnums.ADMIN_MINI_PROGRAM);
        userSocial.setUniqueId(uniqueID);
        userSocial.setBindAt(DateUtils.now());
        sysUserSocialService.save(userSocial);
    }
}

