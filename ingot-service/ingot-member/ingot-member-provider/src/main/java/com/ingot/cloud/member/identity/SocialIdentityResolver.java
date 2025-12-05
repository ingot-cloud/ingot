package com.ingot.cloud.member.identity;

import com.ingot.cloud.member.service.biz.BizUserService;
import com.ingot.cloud.member.service.domain.MemberUserTenantService;
import com.ingot.cloud.pms.api.rpc.PmsTenantDetailsService;
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
    private final MemberUserTenantService userTenantService;

    private final BizUserService bizUserService;
    private final PmsTenantDetailsService pmsTenantDetailsService;

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
                    userTenantService, bizUserService, pmsTenantDetailsService);
        });
    }
}
