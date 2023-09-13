package com.ingot.cloud.pms.social.impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysSocialDetails;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.domain.SysUserSocial;
import com.ingot.cloud.pms.service.domain.SysSocialDetailsService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.cloud.pms.service.domain.SysUserSocialService;
import com.ingot.cloud.pms.social.SocialProcessor;
import com.ingot.framework.core.constants.SocialConstants;
import com.ingot.framework.core.model.enums.SocialTypeEnums;
import com.ingot.framework.core.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : MiniProgramSocialProcessor.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/1/19.</p>
 * <p>Time         : 10:15 AM.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MiniProgramSocialProcessor implements SocialProcessor<SysUser> {
    private final SysUserService sysUserService;
    private final SysUserSocialService sysUserSocialService;
    private final SysSocialDetailsService sysSocialDetailsService;

    @Override
    public boolean support(SocialTypeEnums socialType) {
        return socialType == SocialTypeEnums.ADMIN_MINI_PROGRAM;
    }

    @Override
    public String getUniqueID(String code) {
        SysSocialDetails socialDetails = sysSocialDetailsService.getOne(
                Wrappers.<SysSocialDetails>lambdaQuery()
                        .eq(SysSocialDetails::getType, SocialTypeEnums.ADMIN_MINI_PROGRAM));
        if (socialDetails == null) {
            log.debug("未设置微信小程序appId，appSecret等信息");
            return null;
        }

        String url = String.format(SocialConstants.MINI_APP_AUTHORIZATION_CODE_URL,
                socialDetails.getAppId(), socialDetails.getAppSecret(), code);
        String result = HttpUtil.get(url);
        log.debug("微信小程序响应报文:{}", result);

        Object obj = JSONUtil.parseObj(result).get("openid");
        return obj.toString();
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
        long count = sysUserSocialService.count(Wrappers.<SysUserSocial>lambdaQuery()
                .eq(SysUserSocial::getType, SocialTypeEnums.ADMIN_MINI_PROGRAM)
                .eq(SysUserSocial::getUniqueId, uniqueID)
                .eq(SysUserSocial::getUserId, user.getId()));
        if (count > 0) {
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
