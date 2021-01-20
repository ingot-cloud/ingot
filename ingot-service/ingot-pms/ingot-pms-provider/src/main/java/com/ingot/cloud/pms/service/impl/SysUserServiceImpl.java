package com.ingot.cloud.pms.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.dto.user.UserDto;
import com.ingot.cloud.pms.api.model.dto.user.UserInfoDto;
import com.ingot.cloud.pms.api.model.vo.user.UserPageItemVo;
import com.ingot.cloud.pms.mapper.SysUserMapper;
import com.ingot.cloud.pms.service.SysRoleService;
import com.ingot.cloud.pms.service.SysUserService;
import com.ingot.framework.security.core.userdetails.IngotUser;
import com.ingot.framework.security.exception.UnauthorizedException;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
@AllArgsConstructor
public class SysUserServiceImpl extends BaseServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    private final SysRoleService sysRoleService;

    @Override
    public UserInfoDto getUserInfo(IngotUser user) {
        SysUser userInfo = getById(user.getId());
        if (userInfo == null) {
            throw new UnauthorizedException("用户异常");
        }

        List<SysRole> roles = sysRoleService.getAllRolesOfUser(user.getId(), user.getDeptId());
        List<String> roleCodes = roles.stream()
                .map(SysRole::getCode).collect(Collectors.toList());

        UserInfoDto result = new UserInfoDto();
        result.setUser(userInfo);
        result.setRoles(roleCodes);
        return result;
    }

    @Override
    public IPage<UserPageItemVo> conditionPage(Page<SysUser> page, UserDto condition) {
        List<Long> clientIds = condition.getClientIds();
        // 如果客户端ID不为空，那么查询客户端拥有的所有角色，和condition中的角色列表合并
        if (CollUtil.isNotEmpty(clientIds)) {
            Set<Long> roleIds = sysRoleService.getAllRolesOfClients(clientIds)
                    .stream().map(SysRole::getId).collect(Collectors.toSet());
            if (CollUtil.isNotEmpty(roleIds)) {
                List<Long> currentRoleIds = condition.getRoleIds();
                if (CollUtil.isNotEmpty(currentRoleIds)) {
                    roleIds.addAll(currentRoleIds);
                }
                condition.setRoleIds(CollUtil.newArrayList(roleIds));
            }
        }

        return baseMapper.conditionPage(page, condition);
    }
}
