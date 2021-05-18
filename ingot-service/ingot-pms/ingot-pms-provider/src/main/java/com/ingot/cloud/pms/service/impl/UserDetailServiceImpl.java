package com.ingot.cloud.pms.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysOauthClientDetails;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.service.SysOauthClientDetailsService;
import com.ingot.cloud.pms.service.SysRoleService;
import com.ingot.cloud.pms.service.SysUserService;
import com.ingot.cloud.pms.service.UserDetailService;
import com.ingot.cloud.pms.social.SocialProcessor;
import com.ingot.framework.core.model.dto.user.UserAuthDetails;
import com.ingot.framework.core.model.dto.user.UserDetailsDto;
import com.ingot.framework.core.model.enums.SocialTypeEnum;
import com.ingot.framework.core.model.enums.UserDetailsModeEnum;
import com.ingot.framework.core.model.enums.UserStatusEnum;
import com.ingot.framework.security.exception.BadRequestException;
import com.ingot.framework.security.exception.ForbiddenException;
import com.ingot.framework.security.exception.UnauthorizedException;
import com.ingot.framework.security.utils.SocialUtils;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
    private final Map<String, SocialProcessor> socialProcessorMap;

    @Override
    public UserAuthDetails getUserAuthDetails(UserDetailsDto params) {
        UserDetailsModeEnum model = params.getMode();
        if (model == null) {
            throw new ForbiddenException("非法授权模式");
        }

        SysUser user;
        switch (model) {
            case PASSWORD:
                user = withPasswordMode(params);
                break;
            case SOCIAL:
                user = withSocialMode(params);
                break;
            default:
                throw new ForbiddenException("授权模式不正确：" + model);
        }

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

    private SysUser withPasswordMode(UserDetailsDto params) {
        String username = params.getUniqueCode();
        return sysUserService.getOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, username));
    }

    private SysUser withSocialMode(UserDetailsDto params) {
        String[] extract = SocialUtils.extract(params.getUniqueCode());
        SocialTypeEnum socialType = SocialTypeEnum.getEnum(extract[0]);
        if (socialType == null) {
            throw new BadRequestException("非法社交类型");
        }

        SocialProcessor processor = socialProcessorMap.get(socialType.getBeanName());
        if (processor == null) {
            throw new BadRequestException("非法社交类型");
        }

        params.setUniqueCode(extract[1]);
        return processor.exec(params);
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
            throw new BadRequestException("用户名或密码不正确");
        }
        if (user.getStatus().ordinal() > UserStatusEnum.ENABLE.ordinal()) {
            throw new UnauthorizedException("用户" + user.getStatus().getDesc());
        }
    }
}
