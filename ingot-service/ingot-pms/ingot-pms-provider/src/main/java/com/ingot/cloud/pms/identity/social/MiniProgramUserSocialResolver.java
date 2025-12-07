package com.ingot.cloud.pms.identity.social;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.domain.SysUserSocial;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.cloud.pms.service.domain.SysUserSocialService;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.security.core.identity.social.UserSocialResolver;
import com.ingot.framework.social.wechat.properties.SocialWechatProperties;
import com.ingot.framework.social.wechat.utils.BizSocialUtil;
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
public class MiniProgramUserSocialResolver implements UserSocialResolver<SysUser> {
    private final SysUserService sysUserService;
    private final SysUserSocialService sysUserSocialService;
    private final SysSocialDetailsService sysSocialDetailsService;
    private final SocialWechatProperties socialWechatProperties;

    @Override
    public boolean supports(SocialTypeEnum socialType) {
        return socialType == SocialTypeEnum.WECHAT_MINI_PROGRAM;
    }

    @Override
    public String getUniqueID(String code) {
        return BizSocialUtil.getMiniProgramOpenId(
                () -> CollUtil.emptyIfNull(sysSocialDetailsService.list(Wrappers.<SysSocialDetails>lambdaQuery()
                                .eq(SysSocialDetails::getType, SocialTypeEnum.WECHAT_MINI_PROGRAM)))
                        .stream()
                        .filter(item -> item.getAppId().equals(socialWechatProperties.getMiniProgramAppId()))
                        .findFirst().orElse(null),
                code);
    }

    @Override
    public SysUser getUserInfo(String uniqueID) {
        SysUserSocial userSocial = sysUserSocialService.getOne(Wrappers.<SysUserSocial>lambdaQuery()
                .eq(SysUserSocial::getType, SocialTypeEnum.WECHAT_MINI_PROGRAM)
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
                .eq(SysUserSocial::getType, SocialTypeEnum.WECHAT_MINI_PROGRAM)
                .eq(SysUserSocial::getUniqueId, uniqueID)
                .eq(SysUserSocial::getUserId, user.getId()));
        // 如果当前存在绑定关系，那么更新绑定关系
        if (current != null) {
            current.setUserId(user.getId());
            current.setBindAt(DateUtil.now());
            sysUserSocialService.updateById(current);
            return;
        }

        SysUserSocial userSocial = new SysUserSocial();
        userSocial.setUserId(user.getId());
        userSocial.setType(SocialTypeEnum.WECHAT_MINI_PROGRAM);
        userSocial.setUniqueId(uniqueID);
        userSocial.setBindAt(DateUtil.now());
        sysUserSocialService.save(userSocial);
    }
}
