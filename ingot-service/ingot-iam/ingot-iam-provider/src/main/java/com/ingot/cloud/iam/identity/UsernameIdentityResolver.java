package com.ingot.cloud.iam.identity;

import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserIdentityTypeEnum;
import com.ingot.framework.security.account.web.support.AuthContextSupport;
import com.ingot.framework.security.core.identity.UserIdentityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>按新模型账号选择单一成员身份，账号未命中时不回退旧用户表。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class UsernameIdentityResolver implements UserIdentityResolver {
    private final AccountIdentityService accountIdentities;
    private final AuthContextSupport authContextSupport;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supports(UserIdentityTypeEnum type) {
        return type == UserIdentityTypeEnum.USERNAME;
    }

    /**
     * 加载登录资料；只查询 {@code iam_account}。
     *
     * @param request 登录条件
     * @return 认证资料；账号未命中时为空
     */
    @Override
    public UserDetailsResponse load(UserDetailsRequest request) {
        return accountIdentities.load(request).map(response -> {
            if (response.getId() != null) {
                authContextSupport.fill(response, response.getId(), request.getUserType());
            }
            return response;
        }).orElse(null);
    }
}
