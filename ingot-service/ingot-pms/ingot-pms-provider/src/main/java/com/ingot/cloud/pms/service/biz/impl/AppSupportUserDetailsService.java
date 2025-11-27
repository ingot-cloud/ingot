package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.UserConvert;
import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.api.model.domain.Member;
import com.ingot.cloud.pms.api.model.domain.AppUserTenant;
import com.ingot.cloud.pms.service.biz.SupportUserDetailsService;
import com.ingot.cloud.pms.service.domain.AppRoleService;
import com.ingot.cloud.pms.service.domain.MemberService;
import com.ingot.cloud.pms.service.domain.AppUserTenantService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.social.SocialProcessorManager;
import com.ingot.framework.commons.model.common.AllowTenantDTO;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * <p>Description  : AppSupportUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/13.</p>
 * <p>Time         : 10:31 AM.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppSupportUserDetailsService implements SupportUserDetailsService<Member> {
    private final SysTenantService sysTenantService;
    private final MemberService memberService;
    private final AppRoleService appRoleService;
    private final AppUserTenantService appUserTenantService;

    private final SocialProcessorManager socialProcessorManager;
    private final UserConvert userConvert;

    @Override
    public boolean support(UserDetailsRequest request) {
        return request.getUserType() == UserTypeEnum.APP;
    }

    @Override
    public UserDetailsResponse getUserDetails(UserDetailsRequest request) {
        return commonGetUserDetails(request, socialProcessorManager);
    }

    public UserDetailsResponse getUserAuthDetails(UserDetailsRequest request) {
        String username = request.getUsername();
        // 1.作为手机号查询
        Member user = memberService.getOne(Wrappers.<Member>lambdaQuery()
                .eq(Member::getPhone, username));
        if (user != null) {
            return map(user, request.getUserType(), request.getTenant());
        }
        // 2.作为用户名
        user = memberService.getOne(Wrappers.<Member>lambdaQuery()
                .eq(Member::getUsername, username));
        return map(user, request.getUserType(), request.getTenant());
    }

    @Override
    public List<AllowTenantDTO> getAllowTenants(Member user) {
        // 1.获取可以访问的租户列表
        List<AppUserTenant> userTenantList = appUserTenantService.list(
                Wrappers.<AppUserTenant>lambdaQuery()
                        .eq(AppUserTenant::getUserId, user.getId()));
        return getAllowTenantList(user, userTenantList, sysTenantService);
    }

    @Override
    public UserDetailsResponse userToUserDetailsResponse(Member user) {
        return userConvert.toUserDetails(user);
    }

    @Override
    public List<String> getScopes(Long tenant, Member user) {
        List<AppRole> roles = appRoleService.getRolesOfUser(user.getId());
        if (CollUtil.isEmpty(roles)) {
            return ListUtil.empty();
        }
        return getRoleCodes(roles, tenant);
    }
}
