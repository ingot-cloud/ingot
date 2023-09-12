package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.transform.UserTrans;
import com.ingot.cloud.pms.service.biz.UserDetailsService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.cloud.pms.social.SocialProcessor;
import com.ingot.framework.core.model.common.AllowTenantDTO;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.model.enums.SocialTypeEnums;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.security.common.utils.SocialUtils;
import com.ingot.framework.security.core.userdetails.UserDetailsRequest;
import com.ingot.framework.security.core.userdetails.UserDetailsResponse;
import com.ingot.framework.security.oauth2.core.IngotAuthorizationGrantType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>Description  : UserDetailServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/29.</p>
 * <p>Time         : 5:27 下午.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysAuthorityService sysAuthorityService;
    private final SysTenantService sysTenantService;
    private final SysUserTenantService sysUserTenantService;
    private final Oauth2RegisteredClientService oauth2RegisteredClientService;
    private final Map<String, SocialProcessor> socialProcessorMap;
    private final UserTrans userTrans;

    @Override
    public UserDetailsResponse getUserDetails(UserDetailsRequest params) {
        String grantType = params.getGrantType();
        if (StrUtil.equals(IngotAuthorizationGrantType.PASSWORD.getValue(), grantType)) {
            return getUserAuthDetails(params);
        }
        if (StrUtil.equals(IngotAuthorizationGrantType.SOCIAL.getValue(), grantType)) {
            return getUserAuthDetailsSocial(params);
        }
        return null;
    }

    public UserDetailsResponse getUserAuthDetails(UserDetailsRequest params) {
        String username = params.getUsername();
        SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        return map(user, params.getUserType());
    }

    public UserDetailsResponse getUserAuthDetailsSocial(UserDetailsRequest params) {
        String unique = params.getUsername();
        String[] extract = SocialUtils.extract(unique);
        SocialTypeEnums socialType = SocialTypeEnums.get(extract[0]);
        if (socialType == null) {
            log.error("[UserDetailServiceImpl] 非法社交类型={}", extract[0]);
            return null;
        }

        SocialProcessor processor = socialProcessorMap.get(socialType.getBeanName());
        if (processor == null) {
            log.error("[UserDetailServiceImpl] 非法社交类型={}, 不存在该类型社交执行流程", extract[0]);
            return null;
        }

        String uniqueID = processor.uniqueID(extract[1]);
        return map(processor.info(uniqueID), params.getUserType());
    }

    private UserDetailsResponse map(SysUser user, String userType) {
        return Optional.ofNullable(user)
                .map(value -> {
                    List<AllowTenantDTO> allows = getTenantList(user);

                    UserStatusEnum userTenantStatus = CollUtil.isEmpty(allows)
                            ? UserStatusEnum.LOCK : UserStatusEnum.ENABLE;
                    value.setStatus(value.getStatus() == UserStatusEnum.ENABLE
                            && userTenantStatus == UserStatusEnum.ENABLE ?
                            UserStatusEnum.ENABLE : UserStatusEnum.LOCK);

                    UserDetailsResponse result = userTrans.toUserDetails(value);
                    result.setUserType(userType);
                    result.setAllows(allows);

                    // 查询拥有的角色
                    List<SysRole> roles = sysRoleService.getAllRolesOfUser(user.getId(), user.getDeptId());

                    setRoles(result, roles);

                    setClients(result, roles.stream().map(SysRole::getId).collect(Collectors.toList()));
                    return result;
                }).orElse(null);
    }

    private List<AllowTenantDTO> getTenantList(SysUser user) {
        // 1.获取可以访问的租户列表
        List<SysUserTenant> userTenantList = sysUserTenantService.list(
                Wrappers.<SysUserTenant>lambdaQuery()
                        .eq(SysUserTenant::getUserId, user.getId()));

        return sysTenantService.list(
                        Wrappers.<SysTenant>lambdaQuery()
                                .in(SysTenant::getId, userTenantList.stream()
                                        .map(SysUserTenant::getTenantId).collect(Collectors.toSet())))
                .stream()
                .filter(item -> item.getStatus() == CommonStatusEnum.ENABLE)
                .map(item -> {
                    AllowTenantDTO dto = new AllowTenantDTO();
                    dto.setId(item.getId());
                    dto.setName(item.getName());
                    dto.setAvatar(item.getAvatar());
                    dto.setMain(userTenantList.stream()
                            .anyMatch(t -> Objects.equals(t.getTenantId(), item.getId()) && t.getMain()));
                    return dto;
                })
                .toList();
    }

    private void setRoles(UserDetailsResponse result, List<SysRole> roles) {
        List<String> roleCodes = roles.stream()
                .map(SysRole::getCode)
                .collect(Collectors.toList());
        // 拥有的权限
        Set<String> authorities = sysAuthorityService.getAuthorityByRoles(roles)
                .stream().map(SysAuthority::getCode).collect(Collectors.toSet());
        roleCodes.addAll(authorities);
        result.setRoles(roleCodes);
    }

    private void setClients(UserDetailsResponse result, List<Long> roleIds) {
        // 查询可访问的客户端
        List<String> clientIds = Optional.ofNullable(oauth2RegisteredClientService.getClientsByRoles(roleIds))
                .map(clients -> clients.stream()
                        .map(Oauth2RegisteredClient::getClientId).collect(Collectors.toSet()))
                .map(ListUtil::toList)
                .orElse(ListUtil.toList());
        result.setClients(clientIds);
    }
}
