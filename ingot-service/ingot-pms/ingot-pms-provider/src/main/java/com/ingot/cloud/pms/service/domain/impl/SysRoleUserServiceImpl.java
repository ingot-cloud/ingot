package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.common.CommonRoleRelationService;
import com.ingot.cloud.pms.mapper.SysRoleUserMapper;
import com.ingot.cloud.pms.service.domain.SysRoleUserService;
import com.ingot.framework.core.model.dto.common.RelationDTO;
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

    private final Do<Long> remove = (roleId, targetId) -> remove(Wrappers.<SysRoleUser>lambdaQuery()
            .eq(SysRoleUser::getRoleId, roleId)
            .eq(SysRoleUser::getUserId, targetId));
    private final Do<Long> bind = (roleId, targetId) -> {
        getBaseMapper().insertIgnore(roleId, targetId);
        return true;
    };

    @Override
    public boolean removeByUserId(long userId) {
        return remove(Wrappers.<SysRoleUser>lambdaQuery()
                .eq(SysRoleUser::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserRole(long userId, List<Long> roles) {
        long userCount = count(Wrappers.<SysRoleUser>lambdaQuery().eq(SysRoleUser::getUserId, userId));
        if (userCount != 0) {
            assertionChecker.checkOperation(removeByUserId(userId),
                    "SysRoleUserServiceImpl.UpdateRoleFailed");
        }

        if (CollUtil.isEmpty(roles)) {
            return;
        }
        boolean result = roles.stream().allMatch(roleId -> {
            SysRoleUser entity = new SysRoleUser();
            entity.setUserId(userId);
            entity.setRoleId(roleId);
            return entity.insert();
        });
        assertionChecker.checkOperation(result,
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
}
