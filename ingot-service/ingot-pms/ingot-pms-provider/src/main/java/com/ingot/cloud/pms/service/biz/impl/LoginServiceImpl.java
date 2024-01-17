package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.api.model.dto.auth.SocialRegisterDTO;
import com.ingot.cloud.pms.api.model.dto.user.AppUserCreateDTO;
import com.ingot.cloud.pms.service.biz.BizAppUserService;
import com.ingot.cloud.pms.service.biz.LoginService;
import com.ingot.cloud.pms.social.SocialProcessorManager;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import lombok.RequiredArgsConstructor;
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
    private final AssertionChecker assertionChecker;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(SocialRegisterDTO params) {
        // 不支持后台用户注册
        assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getPhone()), "LoginServiceImpl.PhoneCantBeNull");
        assertionChecker.checkOperation(StrUtil.isNotEmpty(params.getCode()), "LoginServiceImpl.SocialCodeCantBeNull");

        AppUserCreateDTO userCreateDTO = new AppUserCreateDTO();
        userCreateDTO.setPhone(params.getPhone());
        userCreateDTO.setNickname(params.getNickname());
        userCreateDTO.setAvatar(params.getAvatar());
        AppUser user = bizAppUserService.createIfPhoneNotUsed(userCreateDTO);

        String uniqueId = socialProcessorManager.getUniqueID(params.getType(), params.getCode());
        socialProcessorManager.bind(params.getType(), user, uniqueId);
    }
}
