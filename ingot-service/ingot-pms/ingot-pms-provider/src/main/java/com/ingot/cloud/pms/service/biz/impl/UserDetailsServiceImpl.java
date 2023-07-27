package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.Oauth2RegisteredClient;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.transform.UserTrans;
import com.ingot.cloud.pms.service.biz.UserDetailsService;
import com.ingot.cloud.pms.service.domain.Oauth2RegisteredClientService;
import com.ingot.cloud.pms.service.domain.SysAuthorityService;
import com.ingot.cloud.pms.service.domain.SysRoleService;
import com.ingot.cloud.pms.service.domain.SysUserService;
import com.ingot.cloud.pms.social.SocialProcessor;
import com.ingot.framework.core.model.enums.SocialTypeEnums;
import com.ingot.framework.security.common.utils.SocialUtils;
import com.ingot.framework.security.core.userdetails.UserDetailsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
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
    private final Oauth2RegisteredClientService oauth2RegisteredClientService;
    private final Map<String, SocialProcessor> socialProcessorMap;
    private final UserTrans userTrans;

    @Override
    public UserDetailsResponse getUserAuthDetails(String username) {
        SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
        return map(user);
    }

    @Override
    public UserDetailsResponse getUserAuthDetailsSocial(String unique) {
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
        return map(processor.info(uniqueID));
    }

    private UserDetailsResponse map(SysUser user) {
        return Optional.ofNullable(user)
                .map(value -> {
                    UserDetailsResponse result = userTrans.toUserDetails(value);
                    // 查询拥有的角色
                    List<SysRole> roles = sysRoleService.getAllRolesOfUser(user.getId(), user.getDeptId());
                    List<String> roleCodes = roles.stream()
                            .map(SysRole::getCode)
                            .collect(Collectors.toList());
                    // 拥有的权限
                    Set<String> authorities = sysAuthorityService.getAuthorityByRoles(roles)
                            .stream().map(SysAuthority::getCode).collect(Collectors.toSet());
                    roleCodes.addAll(authorities);
                    result.setRoles(roleCodes);

                    List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
                    // 查询可访问的客户端
                    List<String> clientIds = Optional.ofNullable(oauth2RegisteredClientService.getClientsByRoles(roleIds))
                            .map(clients -> clients.stream()
                                    .map(Oauth2RegisteredClient::getClientId).collect(Collectors.toSet()))
                            .map(ListUtil::toList)
                            .orElse(CollUtil.empty(List.class));
                    result.setClients(clientIds);
                    return result;
                }).orElse(null);
    }
}
