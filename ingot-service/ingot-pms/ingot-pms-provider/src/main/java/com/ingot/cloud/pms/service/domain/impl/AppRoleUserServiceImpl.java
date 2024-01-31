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
import com.ingot.framework.core.model.common.RelationDTO;
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

    private final Do<Long> remove = (roleId, targetId) -> remove(Wrappers.<AppRoleUser>lambdaQuery()
            .eq(AppRoleUser::getRoleId, roleId)
            .eq(AppRoleUser::getUserId, targetId));
    private final Do<Long> bind = (roleId, targetId) -> {
        getBaseMapper().insertIgnore(roleId, targetId);
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
    public IPage<AppUser> getRoleUsers(long roleId, Page<?> page, boolean isBind, AppUser condition) {
        return baseMapper.getRoleUsers(page, roleId, isBind, condition);
    }
}
