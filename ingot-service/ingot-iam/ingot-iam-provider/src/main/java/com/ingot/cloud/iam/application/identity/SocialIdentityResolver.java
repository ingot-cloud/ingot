package com.ingot.cloud.iam.application.identity;

import com.ingot.cloud.iam.domain.identity.AccountSocialPrincipal;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserIdentityTypeEnum;
import com.ingot.framework.security.account.web.support.AuthContextSupport;
import com.ingot.framework.security.core.identity.UserIdentityResolver;
import com.ingot.framework.security.core.identity.social.UserSocialService;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>社交绑定命中 {@code iam_account} 后选择单一成员身份，未命中不回退旧用户表。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class SocialIdentityResolver implements UserIdentityResolver {
    private final UserSocialService userSocialService;
    private final AccountIdentityService accounts;
    private final AuthContextSupport authContextSupport;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(UserIdentityTypeEnum type) {
        return type == UserIdentityTypeEnum.SOCIAL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserDetailsResponse load(UserDetailsRequest request) {
        return TenantEnv.applyAs(request.getTenant(), () -> {
            SocialTypeEnum socialType = request.getSocialType();
            String uniqueID = userSocialService.getUniqueID(socialType, request.getSocialCode());
            AccountSocialPrincipal socialUser = userSocialService.getUserInfo(socialType, uniqueID);
            if (socialUser == null || socialUser.accountId() == null) {
                return null;
            }
            return accounts.loadByAccountId(socialUser.accountId(), request).map(response -> {
                if (response.getId() != null) {
                    authContextSupport.fill(response, response.getId(), request.getUserType());
                }
                return response;
            }).orElse(null);
        });
    }
}
