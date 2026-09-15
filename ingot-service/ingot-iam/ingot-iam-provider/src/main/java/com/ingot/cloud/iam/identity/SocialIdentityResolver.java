package com.ingot.cloud.iam.identity;

import com.ingot.cloud.iam.api.model.domain.SysUser;
import com.ingot.cloud.iam.authorization.engine.EffectiveAuthorizationService;
import com.ingot.cloud.iam.service.biz.BizUserDeptService;
import com.ingot.cloud.iam.service.domain.SysTenantService;
import com.ingot.cloud.iam.service.domain.SysUserTenantService;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserIdentityTypeEnum;
import com.ingot.framework.commons.oss.OssService;
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
    private final AccountIdentityService accounts;
    private final SysTenantService sysTenantService;
    private final SysUserTenantService sysUserTenantService;

    private final EffectiveAuthorizationService effectiveAuthorizationService;
    private final BizUserDeptService bizUserDeptService;

    private final OssService ossService;

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
            SysUser socialUser = userSocialService.getUserInfo(socialType, uniqueID);
            if (socialUser != null && socialUser.getId() != null) {
                return accounts.loadByAccountId(socialUser.getId(), request)
                        .orElseGet(() -> IdentityUtil.map(socialUser, request.getUserType(), request.getTenant(),
                                sysTenantService, sysUserTenantService,
                                effectiveAuthorizationService, bizUserDeptService, ossService));
            }
            return IdentityUtil.map(socialUser, request.getUserType(), request.getTenant(),
                    sysTenantService, sysUserTenantService,
                    effectiveAuthorizationService, bizUserDeptService, ossService);
        });
    }
}
