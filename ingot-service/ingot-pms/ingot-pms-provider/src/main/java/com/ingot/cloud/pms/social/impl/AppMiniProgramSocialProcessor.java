package com.ingot.cloud.pms.social.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.Member;
import com.ingot.cloud.pms.api.model.domain.AppUserSocial;
import com.ingot.cloud.pms.core.BizSocialUtils;
import com.ingot.cloud.pms.service.domain.MemberService;
import com.ingot.cloud.pms.service.domain.AppUserSocialService;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.cloud.pms.social.SocialProcessor;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.commons.utils.DateUtil;
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
public class AppMiniProgramSocialProcessor implements SocialProcessor<Member> {
    private final SysSocialDetailsService sysSocialDetailsService;
    private final MemberService memberService;
    private final AppUserSocialService appUserSocialService;

    @Override
    public boolean support(SocialTypeEnum socialType) {
        return socialType == SocialTypeEnum.APP_MINI_PROGRAM;
    }

    @Override
    public String getUniqueID(String code) {
        return BizSocialUtils.getMiniProgramOpenId(sysSocialDetailsService, SocialTypeEnum.APP_MINI_PROGRAM, code);
    }

    @Override
    public Member getUserInfo(String uniqueID) {
        AppUserSocial userSocial = appUserSocialService.getOne(Wrappers.<AppUserSocial>lambdaQuery()
                .eq(AppUserSocial::getType, SocialTypeEnum.APP_MINI_PROGRAM)
                .eq(AppUserSocial::getUniqueId, uniqueID));
        if (userSocial == null) {
            log.debug("微信小程序未绑定openId={}", uniqueID);
            return null;
        }

        return memberService.getById(userSocial.getUserId());
    }

    @Override
    public void bind(Member user, String uniqueID) {
        AppUserSocial current = appUserSocialService.getOne(Wrappers.<AppUserSocial>lambdaQuery()
                .eq(AppUserSocial::getType, SocialTypeEnum.APP_MINI_PROGRAM)
                .eq(AppUserSocial::getUniqueId, uniqueID)
                .eq(AppUserSocial::getUserId, user.getId()));
        // 如果当前存在绑定关系，那么更新绑定关系
        if (current != null) {
            current.setUserId(user.getId());
            current.setBindAt(DateUtil.now());
            appUserSocialService.updateById(current);
            return;
        }

        AppUserSocial userSocial = new AppUserSocial();
        userSocial.setUserId(user.getId());
        userSocial.setType(SocialTypeEnum.APP_MINI_PROGRAM);
        userSocial.setUniqueId(uniqueID);
        userSocial.setBindAt(DateUtil.now());
        appUserSocialService.save(userSocial);
    }
}
