package com.ingot.cloud.pms.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.model.domain.SysOauthClientDetails;
import com.ingot.cloud.pms.model.domain.SysRole;
import com.ingot.cloud.pms.model.domain.SysUser;
import com.ingot.cloud.pms.service.SysOauthClientDetailsService;
import com.ingot.cloud.pms.service.SysRoleService;
import com.ingot.cloud.pms.service.SysUserService;
import com.ingot.cloud.pms.service.UserDetailService;
import com.ingot.framework.core.model.dto.user.UserAuthDetails;
import com.ingot.framework.core.model.dto.user.UserDetailsDto;
import com.ingot.framework.core.model.enums.UserDetailsModeEnum;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.security.exception.oauth2.BadRequestException;
import com.ingot.framework.security.exception.oauth2.ForbiddenException;
import com.ingot.framework.security.exception.oauth2.UnauthorizedException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>Description  : UserDetailServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/12/29.</p>
 * <p>Time         : 5:27 下午.</p>
 */
@Service
@AllArgsConstructor
public class UserDetailServiceImpl implements UserDetailService {
    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysOauthClientDetailsService sysOauthClientDetailsService;

    @Override
    public UserAuthDetails getUserAuthDetails(long tenantId, UserDetailsDto params) {
        UserDetailsModeEnum model = params.getMode();
        if (model != null) {
            switch (model) {
                case PASSWORD:
                    return withPasswordMode(tenantId, params);
                case SOCIAL:
                    return withSocialMode(tenantId, params);
            }
        }
        throw new ForbiddenException("授权模式不正确：" + model);
    }

    private UserAuthDetails withPasswordMode(long tenantId, UserDetailsDto params) {
        String username = params.getUniqueCode();
        SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getTenantId, tenantId)
                .eq(SysUser::getUsername, username));
        // 校验用户
        checkUser(user);

        UserAuthDetails userDetails = ofUser(user);

        // 查询拥有的角色
        List<SysRole> roles = sysRoleService.getAllRolesOfUser(user.getId(), user.getDeptId());
        List<String> roleCodes = roles.stream()
                .map(SysRole::getCode).collect(Collectors.toList());
        userDetails.setRoles(roleCodes);

        List<Long> roleIds = roles.stream().map(SysRole::getId).collect(Collectors.toList());
        List<SysOauthClientDetails> clients = sysOauthClientDetailsService.getClientsByRoles(roleIds);

        SysOauthClientDetails client = clients.stream()
                .filter(item -> StrUtil.equals(item.getClientId(), params.getClientId()))
                .findFirst().orElse(null);
        if (client == null) {
            throw new UnauthorizedException("未授权该应用");
        }
        userDetails.setAuthType(client.getAuthType());

        return userDetails;
    }

    private UserAuthDetails withSocialMode(long tenantId, UserDetailsDto params) {
        return null;
    }

    private UserAuthDetails ofUser(SysUser user) {
        UserAuthDetails userDetails = new UserAuthDetails();
        userDetails.setId(user.getId());
        userDetails.setDeptId(user.getDeptId());
        userDetails.setTenantId(user.getTenantId());
        userDetails.setUsername(user.getUsername());
        userDetails.setPassword(user.getPassword());
        userDetails.setStatus(user.getStatus());
        return userDetails;
    }

    private void checkUser(SysUser user) {
        if (user == null) {
            throw new BadRequestException("用户不存在");
        }
        if (user.getStatus().ordinal() > UserStatusEnum.ENABLE.ordinal()) {
            throw new UnauthorizedException("用户" + user.getStatus().getDesc());
        }
    }
}
