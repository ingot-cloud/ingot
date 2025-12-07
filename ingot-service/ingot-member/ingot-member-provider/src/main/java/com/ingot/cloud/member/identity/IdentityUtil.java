package com.ingot.cloud.member.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.ingot.cloud.member.api.model.convert.MemberUserConvert;
import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.api.model.domain.MemberUserTenant;
import com.ingot.cloud.member.common.BizUtils;
import com.ingot.cloud.member.service.biz.BizUserService;
import com.ingot.cloud.member.service.domain.MemberUserTenantService;
import com.ingot.cloud.pms.api.rpc.RemotePmsTenantDetailsService;
import com.ingot.framework.commons.model.common.AllowTenantDTO;
import com.ingot.framework.commons.model.enums.UserStatusEnum;
import com.ingot.framework.commons.model.security.TenantDetailsResponse;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.authority.InAuthorityUtils;
import com.ingot.framework.security.oauth2.core.OAuth2ErrorUtils;
import com.ingot.framework.tenant.TenantEnv;

/**
 * <p>Description  : IdentityUtil.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/12/3.</p>
 * <p>Time         : 15:52.</p>
 */
public class IdentityUtil {
    /**
     * 映射用户信息
     *
     * @param user                    用户信息
     * @param userType                用户类型
     * @param tenant                  租户ID
     * @param userTenantService       用户组织服务
     * @param bizUserService          用户服务
     * @param remotePmsTenantDetailsService 租户服务
     * @return 用户信息
     */
    public static UserDetailsResponse map(MemberUser user,
                                          UserTypeEnum userType,
                                          Long tenant,
                                          MemberUserTenantService userTenantService,
                                          BizUserService bizUserService,
                                          RemotePmsTenantDetailsService remotePmsTenantDetailsService) {
        return TenantEnv.applyAs(tenant, () -> Optional.ofNullable(user)
                .map(value -> {
                    List<AllowTenantDTO> allows = getAllowTenants(user, remotePmsTenantDetailsService, userTenantService);
                    UserStatusEnum userStatus = BizUtils.getUserStatus(allows, value.getStatus(), tenant);
                    value.setStatus(userStatus);

                    UserDetailsResponse result = MemberUserConvert.INSTANCE.toUserDetails(value);
                    result.setTenant(tenant);
                    result.setUserType(userType.getValue());
                    result.setAllows(allows);

                    // 如果已经被禁用那么直接返回
                    if (userStatus == UserStatusEnum.LOCK) {
                        return result;
                    }

                    // 设置用户Scope
                    List<String> scopes = new ArrayList<>();

                    // 确认登录的租户不为空，那么查询用户在当前租户下的Scope
                    if (tenant != null) {
                        scopes.addAll(getScopes(tenant, user, bizUserService));
                    } else {
                        scopes.addAll(allows.stream()
                                .flatMap(org ->
                                        TenantEnv.applyAs(Long.parseLong(org.getId()),
                                                        () -> getScopes(Long.parseLong(org.getId()), user,
                                                                bizUserService))
                                                .stream())
                                .toList());
                    }
                    result.setScopes(scopes);
                    return result;
                }).orElse(null));
    }

    private static List<AllowTenantDTO> getAllowTenants(MemberUser user,
                                                        RemotePmsTenantDetailsService remotePmsTenantDetailsService,
                                                        MemberUserTenantService userTenantService) {
        // 1.获取可以访问的租户列表
        List<MemberUserTenant> userTenantList = userTenantService.getUserOrgs(user.getId());
        if (CollUtil.isEmpty(userTenantList)) {
            return ListUtil.empty();
        }

        TenantDetailsResponse response = remotePmsTenantDetailsService.getTenantByIds(userTenantList.stream()
                        .map(MemberUserTenant::getTenantId).collect(Collectors.toList()))
                .ifError(OAuth2ErrorUtils::checkResponse)
                .getData();
        return CollUtil.emptyIfNull(response.getAllows())
                .stream()
                .peek(item -> item.setMain(userTenantList.stream()
                        .anyMatch(t ->
                                Objects.equals(t.getTenantId(), Long.parseLong(item.getId())) && t.getMain())))
                .toList();
    }

    private static List<String> getScopes(Long tenant,
                                          MemberUser user,
                                          BizUserService bizUserService) {
        List<MemberRole> roles = bizUserService.getUserRoles(user.getId());
        if (CollUtil.isEmpty(roles)) {
            return ListUtil.empty();
        }
        return getRoleCodes(roles, tenant);
    }

    private static List<String> getRoleCodes(List<MemberRole> roles, Long loginTenant) {
        if (CollUtil.isEmpty(roles)) {
            return ListUtil.empty();
        }
        return roles.stream()
                .map(item ->
                        InAuthorityUtils.authorityWithTenant(item.getCode(), loginTenant))
                .toList();
    }
}
