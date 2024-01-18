package com.ingot.cloud.pms.social.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.api.model.domain.AppUserSocial;
import com.ingot.cloud.pms.service.domain.AppUserService;
import com.ingot.cloud.pms.service.domain.AppUserSocialService;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.cloud.pms.social.SocialProcessor;
import com.ingot.cloud.pms.social.utils.SocialUtils;
import com.ingot.framework.core.model.enums.SocialTypeEnums;
import com.ingot.framework.core.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : AppMiniProgramSocialProcessor.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/17.</p>
 * <p>Time         : 15:33.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppMiniProgramSocialProcessor implements SocialProcessor<AppUser> {
    private final SysSocialDetailsService sysSocialDetailsService;
    private final AppUserService appUserService;
    private final AppUserSocialService appUserSocialService;

    @Override
    public boolean support(SocialTypeEnums socialType) {
        return socialType == SocialTypeEnums.APP_MINI_PROGRAM;
    }

    @Override
    public String getUniqueID(String code) {
        return SocialUtils.getMiniProgramOpenId(sysSocialDetailsService, code);
    }

    @Override
    public AppUser getUserInfo(String uniqueID) {
        AppUserSocial userSocial = appUserSocialService.getOne(Wrappers.<AppUserSocial>lambdaQuery()
                .eq(AppUserSocial::getType, SocialTypeEnums.APP_MINI_PROGRAM)
                .eq(AppUserSocial::getUniqueId, uniqueID));
        if (userSocial == null) {
            log.debug("微信小程序未绑定openId={}", uniqueID);
            return null;
        }

        return appUserService.getById(userSocial.getUserId());
    }

    @Override
    public void bind(AppUser user, String uniqueID) {
        AppUserSocial current = appUserSocialService.getOne(Wrappers.<AppUserSocial>lambdaQuery()
                .eq(AppUserSocial::getType, SocialTypeEnums.APP_MINI_PROGRAM)
                .eq(AppUserSocial::getUniqueId, uniqueID)
                .eq(AppUserSocial::getUserId, user.getId()));
        // 如果当前存在绑定关系，那么更新绑定关系
        if (current != null) {
            current.setUserId(user.getId());
            current.setBindAt(DateUtils.now());
            appUserSocialService.updateById(current);
            return;
        }

        AppUserSocial userSocial = new AppUserSocial();
        userSocial.setUserId(user.getId());
        userSocial.setType(SocialTypeEnums.APP_MINI_PROGRAM);
        userSocial.setUniqueId(uniqueID);
        userSocial.setBindAt(DateUtils.now());
        appUserSocialService.save(userSocial);
    }
}
