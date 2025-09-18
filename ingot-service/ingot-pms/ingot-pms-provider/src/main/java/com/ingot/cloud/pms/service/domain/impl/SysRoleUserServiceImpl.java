package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.vo.user.UserWithDeptVO;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleUserMapper;
import com.ingot.cloud.pms.service.domain.SysRoleUserService;
import com.ingot.framework.commons.model.common.RelationDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author magician
 * @since 2020-11-20
 */
@Service
public class SysRoleUserServiceImpl extends CommonRoleRelationService<SysRoleUserMapper, SysRoleUser, Long> implements SysRoleUserService {

    private final RoleBindTargets<Long> removeRbt = (roleId, targetIds) -> {
        if (CollUtil.size(targetIds) == 1) {
            return remove(Wrappers.<SysRoleUser>lambdaQuery()
                    .eq(SysRoleUser::getRoleId, roleId)
                    .eq(SysRoleUser::getUserId, targetIds.get(0)));
        }

        return remove(Wrappers.<SysRoleUser>lambdaQuery()
                .eq(SysRoleUser::getRoleId, roleId)
                .in(SysRoleUser::getUserId, targetIds.get(0)));
    };
    private final RoleBindTargets<Long> bindRbt = (roleId, targetIds) -> {
        if (CollUtil.size(targetIds) == 1) {
            SysRoleUser roleUser = new SysRoleUser();
            roleUser.setRoleId(roleId);
            roleUser.setUserId(targetIds.get(0));
            save(roleUser);
            return true;
        }

        List<SysRoleUser> roleUsers = targetIds.stream().map(targetId -> {
            SysRoleUser roleUser = new SysRoleUser();
            roleUser.setRoleId(roleId);
            roleUser.setUserId(targetId);
            return roleUser;
        }).toList();
        saveBatch(roleUsers);
        return true;
    };
    private final TargetBindRoles<Long> removeTbr = (targetId, roleIds) -> {
        if (CollUtil.size(roleIds) == 1) {
            return remove(Wrappers.<SysRoleUser>lambdaQuery()
                    .eq(SysRoleUser::getRoleId, roleIds.get(0))
                    .eq(SysRoleUser::getUserId, targetId));
        }

        return remove(Wrappers.<SysRoleUser>lambdaQuery()
                .in(SysRoleUser::getRoleId, roleIds)
                .eq(SysRoleUser::getUserId, targetId));
    };
    private final TargetBindRoles<Long> bindTbr = (targetId, roleIds) -> {
        if (CollUtil.size(roleIds) == 1) {
            SysRoleUser roleUser = new SysRoleUser();
            roleUser.setRoleId(roleIds.get(0));
            roleUser.setUserId(targetId);
            save(roleUser);
            return true;
        }

        List<SysRoleUser> roleUsers = roleIds.stream().map(roleId -> {
            SysRoleUser roleUser = new SysRoleUser();
            roleUser.setRoleId(roleId);
            roleUser.setUserId(targetId);
            return roleUser;
        }).toList();
        saveBatch(roleUsers);
        return true;
    };

    @Override
    public boolean removeByUserId(long userId) {
        return remove(Wrappers.<SysRoleUser>lambdaQuery()
                .eq(SysRoleUser::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setUserRoles(long userId, List<Long> roles) {
        long userCount = count(Wrappers.<SysRoleUser>lambdaQuery().eq(SysRoleUser::getUserId, userId));
        if (userCount != 0) {
            assertionChecker.checkOperation(removeByUserId(userId),
                    "SysRoleUserServiceImpl.UpdateRoleFailed");
        }

        if (CollUtil.isEmpty(roles)) {
            return;
        }

        List<SysRoleUser> roleUsers = CollUtil.newHashSet(roles)
                .stream()
                .map(roleId -> {
                    SysRoleUser entity = new SysRoleUser();
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
    public IPage<SysUser> getRoleUsers(long roleId, Page<?> page, boolean isBind, SysUser condition) {
        return baseMapper.getRoleUsers(page, roleId, isBind, condition);
    }

    @Override
    public List<SysUser> getRoleUsers(long roleId) {
        return baseMapper.getRoleUserList(roleId);
    }

    @Override
    public List<UserWithDeptVO> getRoleUserWithDeptList(long roleId) {
        return baseMapper.getRoleUserWithDeptList(roleId);
    }

    @Override
    public List<SysUser> getRoleListUsers(List<Long> roleIds) {
        if (CollUtil.isEmpty(roleIds)) {
            return ListUtil.empty();
        }

        if (CollUtil.size(roleIds) == 1) {
            return getRoleUsers(roleIds.get(0));
        }

        return baseMapper.getRoleListUsers(roleIds);
    }
}
