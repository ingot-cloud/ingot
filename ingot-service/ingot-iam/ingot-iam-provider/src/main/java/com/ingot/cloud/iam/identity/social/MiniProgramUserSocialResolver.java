package com.ingot.cloud.iam.identity.social;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.api.model.domain.SysSocialDetails;
import com.ingot.cloud.iam.api.model.domain.SysUser;
import com.ingot.cloud.iam.api.model.domain.SysUserSocial;
import com.ingot.cloud.iam.service.domain.SysSocialDetailsService;
import com.ingot.cloud.iam.service.domain.SysUserSocialService;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.security.core.identity.social.UserSocialResolver;
import com.ingot.framework.social.wechat.properties.SocialWechatProperties;
import com.ingot.framework.social.wechat.utils.BizSocialUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>按社交绑定表解析小程序账号，{@code user_id} 指向 {@code iam_account}，不回读 {@code sys_user}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiniProgramUserSocialResolver implements UserSocialResolver<SysUser> {
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

    /**
     * 按绑定表 {@code user_id} 构造仅含账号 ID 的 {@link SysUser}，不查询 {@code sys_user}。
     *
     * @param uniqueID 小程序 openId
     * @return 命中绑定时的账号外形；未绑定返回 {@code null}
     */
    @Override
    public SysUser getUserInfo(String uniqueID) {
        SysUserSocial userSocial = sysUserSocialService.getOne(Wrappers.<SysUserSocial>lambdaQuery()
                .eq(SysUserSocial::getType, SocialTypeEnum.WECHAT_MINI_PROGRAM)
                .eq(SysUserSocial::getUniqueId, uniqueID));
        if (userSocial == null) {
            log.debug("微信小程序未绑定openId={}", uniqueID);
            return null;
        }

        SysUser account = new SysUser();
        account.setId(userSocial.getUserId());
        return account;
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
