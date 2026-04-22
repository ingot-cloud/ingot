package com.ingot.cloud.member.identity;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.BooleanUtil;
import com.ingot.cloud.member.api.model.convert.MemberUserConvert;
import com.ingot.cloud.member.api.model.domain.MemberRole;
import com.ingot.cloud.member.api.model.domain.MemberUser;
import com.ingot.cloud.member.api.model.domain.MemberUserTenant;
import com.ingot.cloud.member.service.biz.BizUserService;
import com.ingot.cloud.member.service.domain.MemberUserTenantService;
import com.ingot.cloud.pms.api.rpc.RemotePmsTenantDetailsService;
import com.ingot.framework.commons.constants.PermissionConstants;
import com.ingot.framework.commons.model.common.TenantMainDTO;
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
     */
    public static UserDetailsResponse map(MemberUser user,
                                          UserTypeEnum userType,
                                          Long tenant,
                                          MemberUserTenantService userTenantService,
                                          BizUserService bizUserService,
                                          RemotePmsTenantDetailsService remotePmsTenantDetailsService) {
        return TenantEnv.applyAs(tenant, () -> Optional.ofNullable(user)
                .map(value -> {
                    List<TenantMainDTO> allows = getAllowTenants(user, remotePmsTenantDetailsService, userTenantService);

                    // 租户维度可访问性：allows 不为空，且登录 tenant 在允许列表内
                    boolean tenantAccessible = CollUtil.isNotEmpty(allows)
                            && (tenant == null || allows.stream()
                            .anyMatch(item -> Long.parseLong(item.getId()) == tenant));

                    // 账号维度：来自 member_user.enabled / member_user.locked
                    boolean userEnabled = Boolean.TRUE.equals(value.getEnabled()) && tenantAccessible;
                    boolean userLocked = Boolean.TRUE.equals(value.getLocked());

                    UserDetailsResponse result = MemberUserConvert.INSTANCE.toUserDetails(value);
                    result.setTenant(tenant);
                    result.setUserType(userType.getValue());
                    result.setAllows(allows);
                    result.setEnabled(userEnabled);
                    result.setLocked(userLocked);

                    // 账号不可用则无需查询 scope
                    if (!userEnabled || userLocked) {
                        return result;
                    }

                    List<String> scopes = new ArrayList<>();
                    // 强制修改密码：仅下发初始密码修改权限
                    if (BooleanUtil.isTrue(value.getMustChangePwd())) {
                        scopes.add(PermissionConstants.INIT_PASSWORD);
                        result.setScopes(scopes);
                        return result;
                    }

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

    private static List<TenantMainDTO> getAllowTenants(MemberUser user,
                                                       RemotePmsTenantDetailsService remotePmsTenantDetailsService,
                                                       MemberUserTenantService userTenantService) {
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
