package com.ingot.cloud.member.identity;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.service.biz.BizUserService;
import com.ingot.cloud.member.service.domain.MemberUserService;
import com.ingot.cloud.member.service.domain.MemberUserTenantService;
import com.ingot.cloud.pms.api.rpc.PmsTenantDetailsService;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserIdentityTypeEnum;
import com.ingot.framework.security.core.identity.UserIdentityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : UsernameIdentityResolver.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:24.</p>
 */
@Service
@RequiredArgsConstructor
public class UsernameIdentityResolver implements UserIdentityResolver {
    private final MemberUserService memberUserService;
    private final MemberUserTenantService userTenantService;

    private final BizUserService bizUserService;
    private final PmsTenantDetailsService pmsTenantDetailsService;

    @Override
    public boolean supports(UserIdentityTypeEnum type) {
        return type == UserIdentityTypeEnum.USERNAME;
    }

    @Override
    public UserDetailsResponse load(UserDetailsRequest request) {
        String username = request.getUsername();
        // 1.作为手机号查询
        MemberUser user = memberUserService.getOne(Wrappers.<MemberUser>lambdaQuery()
                .eq(MemberUser::getPhone, username));
        if (user != null) {
            return IdentityUtil.map(user, request.getUserType(), request.getTenant(),
                    userTenantService, bizUserService, pmsTenantDetailsService);
        }
        // 2.作为用户名查询
        user = memberUserService.getOne(Wrappers.<MemberUser>lambdaQuery()
                .eq(MemberUser::getUsername, username));
        return IdentityUtil.map(user, request.getUserType(), request.getTenant(),
                userTenantService, bizUserService, pmsTenantDetailsService);
    }


}
