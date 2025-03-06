package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleUserMapper;
import com.ingot.cloud.pms.service.domain.SysRoleUserService;
import com.ingot.framework.core.model.common.RelationDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    private final Do<Long> remove = (roleId, targetId) -> remove(Wrappers.<SysRoleUser>lambdaQuery()
            .eq(SysRoleUser::getRoleId, roleId)
            .eq(SysRoleUser::getUserId, targetId));
    private final Do<Long> bind = (roleId, targetId) -> {
        SysRoleUser roleUser = new SysRoleUser();
        roleUser.setRoleId(roleId);
        roleUser.setUserId(targetId);
        getBaseMapper().insert(roleUser);
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
        bindRoles(params, remove, bind,
                "SysRoleUserServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindUsers(RelationDTO<Long, Long> params) {
        bindTargets(params, remove, bind,
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
}
