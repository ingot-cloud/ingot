package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.AppRoleUser;
import com.ingot.cloud.pms.api.model.domain.AppUser;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.AppRoleUserMapper;
import com.ingot.cloud.pms.service.domain.AppRoleUserService;
import com.ingot.framework.commons.model.common.RelationDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2023-09-12
 */
@Service
public class AppRoleUserServiceImpl extends CommonRoleRelationService<AppRoleUserMapper, AppRoleUser, Long> implements AppRoleUserService {

    private final RoleBindTargets<Long> removeRbt = (roleId, targetIds) -> {
        if (CollUtil.size(targetIds) == 1) {
            return remove(Wrappers.<AppRoleUser>lambdaQuery()
                    .eq(AppRoleUser::getRoleId, roleId)
                    .eq(AppRoleUser::getUserId, targetIds.get(0)));
        }

        return remove(Wrappers.<AppRoleUser>lambdaQuery()
                .eq(AppRoleUser::getRoleId, roleId)
                .in(AppRoleUser::getUserId, targetIds.get(0)));
    };
    private final RoleBindTargets<Long> bindRbt = (roleId, targetIds) -> {
        if (CollUtil.size(targetIds) == 1) {
            AppRoleUser roleUser = new AppRoleUser();
            roleUser.setRoleId(roleId);
            roleUser.setUserId(targetIds.get(0));
            save(roleUser);
            return true;
        }

        List<AppRoleUser> roleUsers = targetIds.stream().map(targetId -> {
            AppRoleUser roleUser = new AppRoleUser();
            roleUser.setRoleId(roleId);
            roleUser.setUserId(targetId);
            return roleUser;
        }).toList();
        saveBatch(roleUsers);
        return true;
    };
    private final TargetBindRoles<Long> removeTbr = (targetId, roleIds) -> {
        if (CollUtil.size(roleIds) == 1) {
            return remove(Wrappers.<AppRoleUser>lambdaQuery()
                    .eq(AppRoleUser::getRoleId, roleIds.get(0))
                    .eq(AppRoleUser::getUserId, targetId));
        }

        return remove(Wrappers.<AppRoleUser>lambdaQuery()
                .in(AppRoleUser::getRoleId, roleIds)
                .eq(AppRoleUser::getUserId, targetId));
    };
    private final TargetBindRoles<Long> bindTbr = (targetId, roleIds) -> {
        if (CollUtil.size(roleIds) == 1) {
            AppRoleUser roleUser = new AppRoleUser();
            roleUser.setRoleId(roleIds.get(0));
            roleUser.setUserId(targetId);
            save(roleUser);
            return true;
        }

        List<AppRoleUser> roleUsers = roleIds.stream().map(roleId -> {
            AppRoleUser roleUser = new AppRoleUser();
            roleUser.setRoleId(roleId);
            roleUser.setUserId(targetId);
            return roleUser;
        }).toList();
        saveBatch(roleUsers);
        return true;
    };

    @Override
    public boolean removeByUserId(long userId) {
        return remove(Wrappers.<AppRoleUser>lambdaQuery()
                .eq(AppRoleUser::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserRoles(long userId, List<Long> roles) {
        long userCount = count(Wrappers.<AppRoleUser>lambdaQuery().eq(AppRoleUser::getUserId, userId));
        if (userCount != 0) {
            assertionChecker.checkOperation(removeByUserId(userId),
                    "SysRoleUserServiceImpl.UpdateRoleFailed");
        }

        if (CollUtil.isEmpty(roles)) {
            return;
        }

        List<AppRoleUser> roleUsers = CollUtil.newHashSet(roles)
                .stream()
                .map(roleId -> {
                    AppRoleUser entity = new AppRoleUser();
                    entity.setUserId(userId);
                    entity.setRoleId(roleId);
                    return entity;
                }).toList();

        assertionChecker.checkOperation(saveBatch(roleUsers),
                "SysRoleUserServiceImpl.UpdateRoleFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userBindRoles(RelationDTO<Long, Long> params) {
        bindRoles(params, removeTbr, bindTbr,
                "SysRoleUserServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindUsers(RelationDTO<Long, Long> params) {
        bindTargets(params, removeRbt, bindRbt,
                "SysRoleUserServiceImpl.RemoveFailed");
    }

    @Override
    public IPage<AppUser> getRoleUsers(long roleId, Page<?> page, boolean isBind, AppUser condition) {
        return baseMapper.getRoleUsers(page, roleId, isBind, condition);
    }
}
