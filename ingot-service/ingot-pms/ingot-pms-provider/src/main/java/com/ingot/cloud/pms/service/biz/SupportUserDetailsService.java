package com.ingot.cloud.pms.service.biz;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.api.model.types.UserTenantType;
import com.ingot.cloud.pms.api.model.types.UserType;
import com.ingot.cloud.pms.common.BizUtils;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.social.SocialProcessorManager;
import com.ingot.framework.commons.model.common.AllowTenantDTO;
import com.ingot.framework.commons.model.enums.SocialTypeEnum;
import com.ingot.framework.commons.model.enums.UserStatusEnum;
import com.ingot.framework.commons.model.security.UserDetailsRequest;
import com.ingot.framework.commons.model.security.UserDetailsResponse;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.core.authority.InAuthorityUtils;
import com.ingot.framework.security.oauth2.core.InAuthorizationGrantType;
import com.ingot.framework.tenant.TenantEnv;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

/**
 * <p>Description  : SupportUserDetailsService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/13.</p>
 * <p>Time         : 10:27 AM.</p>
 */
public interface SupportUserDetailsService<T extends UserType> {

    /**
     * 是否支持请求
     *
     * @param request {@link UserDetailsRequest}
     * @return Boolean
     */
    boolean support(UserDetailsRequest request);

    /**
     * 获取用户详情
     *
     * @param request {@link UserDetailsRequest}
     * @return {@link UserDetailsResponse}
     */
    UserDetailsResponse getUserDetails(UserDetailsRequest request);

    /**
     * 通用处理用户详情方法
     *
     * @param request                {@link UserDetailsRequest}
     * @param socialProcessorManager {@link SocialProcessorManager}
     * @return {@link UserDetailsResponse}
     */
    default UserDetailsResponse commonGetUserDetails(UserDetailsRequest request,
                                                     SocialProcessorManager socialProcessorManager) {
        AuthorizationGrantType grantType = new AuthorizationGrantType(request.getGrantType());
        if (ObjectUtil.equals(InAuthorizationGrantType.PASSWORD, grantType)) {
            return getUserAuthDetails(request);
        }
        if (ObjectUtil.equals(InAuthorizationGrantType.SOCIAL, grantType)) {
            return getUserAuthDetailsSocial(request, socialProcessorManager);
        }
        return null;
    }

    /**
     * 获取用户详情，用于密码模式
     *
     * @param request {@link UserDetailsRequest}
     * @return {@link UserDetailsResponse}
     */
    UserDetailsResponse getUserAuthDetails(UserDetailsRequest request);

    /**
     * 获取用户详情，用于社交模式
     *
     * @param request                {@link UserDetailsRequest}
     * @param socialProcessorManager {@link SocialProcessorManager}
     * @return {@link UserDetailsResponse}
     */
    default UserDetailsResponse getUserAuthDetailsSocial(UserDetailsRequest request,
                                                         SocialProcessorManager socialProcessorManager) {
        return TenantEnv.applyAs(request.getTenant(), () -> {
            SocialTypeEnum socialType = request.getSocialType();
            String socialCode = request.getSocialCode();
            String uniqueID = socialProcessorManager.getUniqueID(socialType, socialCode);
            return map(socialProcessorManager.getUserInfo(socialType, uniqueID),
                    request.getUserType(), request.getTenant());
        });
    }

    /**
     * 用户转换为 {@link UserDetailsResponse}
     *
     * @param user     用户
     * @param userType {@link UserTypeEnum}
     * @param tenant   登录租户
     * @return {@link UserDetailsResponse}
     */
    default UserDetailsResponse map(T user, UserTypeEnum userType, Long tenant) {
        return TenantEnv.applyAs(tenant, () -> Optional.ofNullable(user)
                .map(value -> {
                    List<AllowTenantDTO> allows = getAllowTenants(user);
                    UserStatusEnum userStatus = BizUtils.getUserStatus(allows, value.getStatus(), tenant);
                    value.setStatus(userStatus);

                    UserDetailsResponse result = userToUserDetailsResponse(value);
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
                        scopes.addAll(TenantEnv.applyAs(tenant, () -> getScopes(tenant, user)));
                    } else {
                        scopes.addAll(allows.stream()
                                .flatMap(org ->
                                        TenantEnv.applyAs(Long.parseLong(org.getId()),
                                                () -> getScopes(Long.parseLong(org.getId()), user)).stream())
                                .toList());
                    }
                    result.setScopes(scopes);
                    return result;
                }).orElse(null));
    }

    /**
     * 获取用户允许访问的组织列表
     *
     * @param user 用户
     * @return {@link AllowTenantDTO}
     */
    List<AllowTenantDTO> getAllowTenants(T user);

    /**
     * 用户转 {@link UserDetailsResponse}
     *
     * @param user 用户
     * @return {@link UserDetailsResponse}
     */
    UserDetailsResponse userToUserDetailsResponse(T user);

    /**
     * 获取用户Scope
     *
     * @param tenant 登录组织
     * @param user   用户
     * @return Scope List
     */
    List<String> getScopes(Long tenant, T user);

    /**
     * 获取用户可用租户列表
     *
     * @param user             用户
     * @param userTenantList   用户关联的租户列表
     * @param sysTenantService {@link SysTenantService}
     * @return {@link AllowTenantDTO}
     */
    default List<AllowTenantDTO> getAllowTenantList(T user,
                                                    List<? extends UserTenantType> userTenantList,
                                                    SysTenantService sysTenantService) {
        if (CollUtil.isEmpty(userTenantList)) {
            return ListUtil.empty();
        }
        return BizUtils.getAllows(sysTenantService,
                userTenantList.stream()
                        .map(UserTenantType::getTenantId).collect(Collectors.toSet()),
                (item) -> item.setMain(userTenantList.stream()
                        .anyMatch(t ->
                                Objects.equals(t.getTenantId(), Long.parseLong(item.getId())) && t.getMain())));
    }

    /**
     * 获取角色编码
     *
     * @param roles       角色类型列表
     * @param loginTenant 登录租户
     * @return 角色编码列表
     */
    default List<String> getRoleCodes(List<? extends RoleType> roles, Long loginTenant) {
        if (CollUtil.isEmpty(roles)) {
            return ListUtil.empty();
        }
        return roles.stream()
                .map(item ->
                        InAuthorityUtils.authorityWithTenant(item.getCode(), loginTenant))
                .toList();
    }

}
