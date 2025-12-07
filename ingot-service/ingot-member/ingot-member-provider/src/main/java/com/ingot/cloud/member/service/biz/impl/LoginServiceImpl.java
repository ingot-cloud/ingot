package com.ingot.cloud.member.service.biz.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.member.api.model.dto.user.MemberUserCreateByPhoneDTO;
import com.ingot.cloud.member.service.biz.BizUserService;
import com.ingot.cloud.member.service.biz.LoginService;
import com.ingot.cloud.pms.api.model.dto.auth.MiniProgramRegisterDTO;
import com.ingot.cloud.pms.api.model.dto.auth.SocialRegisterDTO;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.security.core.identity.social.UserSocialService;
import com.ingot.framework.social.wechat.core.WxMaServiceHelper;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : LoginServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 17:16.</p>
 */
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {
    private final BizUserService bizUserService;
    private final UserSocialService userSocialService;
    private final WxMaServiceHelper wxMaServiceHelper;
    private final AssertionChecker assertionChecker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(SocialRegisterDTO params) {
        assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getPhone()), "LoginServiceImpl.PhoneCantBeNull");
        assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getCode()), "LoginServiceImpl.SocialCodeCantBeNull");

        Object user = switch (params.getType()) {
            case WECHAT_MINI_PROGRAM -> {
                MemberUserCreateByPhoneDTO userCreateDTO = new MemberUserCreateByPhoneDTO();
                userCreateDTO.setPhone(params.getPhone());
                userCreateDTO.setNickname(params.getNickname());
                userCreateDTO.setAvatar(params.getAvatar());
                yield bizUserService.createIfPhoneNotUsed(userCreateDTO);
            }
            default -> throw new RuntimeException("暂未开放其它社交平台自主注册接口");
        };


        String uniqueId = userSocialService.getUniqueID(params.getType(), params.getCode());
        userSocialService.bind(params.getType(), user, uniqueId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void miniProgramRegister(MiniProgramRegisterDTO params) {
        // 如果存在phoneCode，那么需要进行数据填充
        if (StrUtil.isNotEmpty(params.getPhoneCode())) {
            WxMaService service = wxMaServiceHelper.getActiveService();
            fillWechatData(service, params);
        }

        params.setType(SocialTypeEnum.WECHAT_MINI_PROGRAM);
        register(params);
    }

    private void fillWechatData(WxMaService service, MiniProgramRegisterDTO params) {
        try {
            WxMaPhoneNumberInfo phoneNumberInfo = service.getUserService().getPhoneNoInfo(params.getPhoneCode());
            params.setPhone(phoneNumberInfo.getPhoneNumber());
            params.setNickname(phoneNumberInfo.getPhoneNumber());
        } catch (WxErrorException e) {
            throw new RuntimeException(e);
        }
    }
}
