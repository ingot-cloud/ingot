package com.ingot.cloud.pms.service.biz.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.dto.auth.MiniProgramRegisterDTO;
import com.ingot.cloud.pms.api.model.dto.auth.SocialRegisterDTO;
import com.ingot.cloud.pms.api.model.dto.user.AppUserCreateDTO;
import com.ingot.cloud.pms.common.wechat.WechatProperties;
import com.ingot.cloud.pms.service.biz.BizAppUserService;
import com.ingot.cloud.pms.service.biz.LoginService;
import com.ingot.cloud.pms.social.SocialProcessorManager;
import com.ingot.framework.core.model.enums.SocialTypeEnums;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : LoginServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/17.</p>
 * <p>Time         : 13:47.</p>
 */
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {
    private final BizAppUserService bizAppUserService;
    private final SocialProcessorManager socialProcessorManager;
    private final WechatProperties wechatProperties;
    private final WxMaService wxMaService;
    private final AssertionChecker assertionChecker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(SocialRegisterDTO params) {
        assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getPhone()), "LoginServiceImpl.PhoneCantBeNull");
        assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getCode()), "LoginServiceImpl.SocialCodeCantBeNull");

        Object user = switch (params.getType()) {
            case APP_MINI_PROGRAM -> {
                AppUserCreateDTO userCreateDTO = new AppUserCreateDTO();
                userCreateDTO.setPhone(params.getPhone());
                userCreateDTO.setNickname(params.getNickname());
                userCreateDTO.setAvatar(params.getAvatar());
                yield bizAppUserService.createIfPhoneNotUsed(userCreateDTO);
            }
            case ADMIN_MINI_PROGRAM -> throw new RuntimeException("暂未开放后台自主注册接口");
            default -> throw new RuntimeException("");
        };


        String uniqueId = socialProcessorManager.getUniqueID(params.getType(), params.getCode());
        socialProcessorManager.bind(params.getType(), user, uniqueId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void appMiniProgramRegister(MiniProgramRegisterDTO params) {
        String appId = wechatProperties.getAppMiniProgramAppId();
        assertionChecker.checkOperation(StrUtil.isNotEmpty(appId), "LoginServiceImpl.AppMiniProgramAppID");

        WxMaService service = wxMaService.switchoverTo(appId);
        fillWechatData(service, params);

        params.setType(SocialTypeEnums.APP_MINI_PROGRAM);
        register(params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adminMiniProgramRegister(MiniProgramRegisterDTO params) {
        String appId = wechatProperties.getAdminMiniProgramAppId();
        assertionChecker.checkOperation(StrUtil.isNotEmpty(appId), "LoginServiceImpl.AdminMiniProgramAppID");

        WxMaService service = wxMaService.switchoverTo(appId);
        fillWechatData(service, params);

        params.setType(SocialTypeEnums.ADMIN_MINI_PROGRAM);
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
