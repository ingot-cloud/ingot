package com.ingot.cloud.pms.identity;

import com.ingot.cloud.pms.service.biz.BizAppService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserIdentityTypeEnum;
import com.ingot.framework.security.core.identity.UserIdentityResolver;
import com.ingot.framework.security.core.identity.social.UserSocialService;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : SocialIdentityResolver.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:37.</p>
 */
@Service
@RequiredArgsConstructor
public class SocialIdentityResolver implements UserIdentityResolver {
    private final UserSocialService userSocialService;
    private final SysTenantService sysTenantService;
    private final SysUserTenantService sysUserTenantService;

    private final BizAppService bizAppService;
    private final BizRoleService bizRoleService;
    private final BizUserService bizUserService;

    @Override
    public boolean supports(UserIdentityTypeEnum type) {
        return type == UserIdentityTypeEnum.SOCIAL;
    }

    @Override
    public UserDetailsResponse load(UserDetailsRequest request) {
        return TenantEnv.applyAs(request.getTenant(), () -> {
            SocialTypeEnum socialType = request.getSocialType();
            String socialCode = request.getSocialCode();
            String uniqueID = userSocialService.getUniqueID(socialType, socialCode);
            return IdentityUtil.map(userSocialService.getUserInfo(socialType, uniqueID),
                    request.getUserType(), request.getTenant(),
                    sysTenantService, sysUserTenantService, bizUserService, bizAppService, bizRoleService);
        });
    }
}
