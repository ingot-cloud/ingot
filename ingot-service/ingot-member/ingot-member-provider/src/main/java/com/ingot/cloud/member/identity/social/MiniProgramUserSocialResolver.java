package com.ingot.cloud.member.identity.social;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.api.model.domain.MemberUserSocial;
import com.ingot.cloud.member.common.BizSocialUtils;
import com.ingot.cloud.member.service.domain.MemberSocialDetailsService;
import com.ingot.cloud.member.service.domain.MemberUserService;
import com.ingot.cloud.member.service.domain.MemberUserSocialService;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.security.core.identity.social.UserSocialResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : MiniProgramUserSocialResolver.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:59.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiniProgramUserSocialResolver implements UserSocialResolver<MemberUser> {
    private final MemberUserService userService;
    private final MemberUserSocialService userSocialService;
    private final MemberSocialDetailsService socialDetailsService;

    @Override
    public boolean supports(SocialTypeEnum socialType) {
        return socialType == SocialTypeEnum.WECHAT_MINI_PROGRAM;
    }

    @Override
    public String getUniqueID(String code) {
        return BizSocialUtils.getMiniProgramOpenId(socialDetailsService, SocialTypeEnum.WECHAT_MINI_PROGRAM, code);
    }

    @Override
    public MemberUser getUserInfo(String uniqueID) {
        MemberUserSocial userSocial = userSocialService.getOne(Wrappers.<MemberUserSocial>lambdaQuery()
                .eq(MemberUserSocial::getType, SocialTypeEnum.WECHAT_MINI_PROGRAM)
                .eq(MemberUserSocial::getUniqueId, uniqueID));
        if (userSocial == null) {
            log.debug("微信小程序未绑定openId={}", uniqueID);
            return null;
        }

        return userService.getById(userSocial.getUserId());
    }

    @Override
    public void bind(MemberUser user, String uniqueID) {
        MemberUserSocial current = userSocialService.getOne(Wrappers.<MemberUserSocial>lambdaQuery()
                .eq(MemberUserSocial::getType, SocialTypeEnum.WECHAT_MINI_PROGRAM)
                .eq(MemberUserSocial::getUniqueId, uniqueID)
                .eq(MemberUserSocial::getUserId, user.getId()));
        // 如果当前存在绑定关系，那么更新绑定关系
        if (current != null) {
            current.setUserId(user.getId());
            current.setBindAt(DateUtil.now());
            userSocialService.updateById(current);
            return;
        }

        MemberUserSocial userSocial = new MemberUserSocial();
        userSocial.setUserId(user.getId());
        userSocial.setType(SocialTypeEnum.WECHAT_MINI_PROGRAM);
        userSocial.setUniqueId(uniqueID);
        userSocial.setBindAt(DateUtil.now());
        userSocialService.save(userSocial);
    }
}
