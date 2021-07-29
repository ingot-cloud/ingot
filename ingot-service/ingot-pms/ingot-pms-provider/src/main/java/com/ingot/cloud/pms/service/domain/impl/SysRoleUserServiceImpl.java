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
import com.ingot.framework.core.model.dto.common.RelationDto;
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
public class SysRoleUserServiceImpl extends CommonRoleRelationService<SysRoleUserMapper, SysRoleUser> implements SysRoleUserService {

    @Override
    public boolean removeByUserId(long userId) {
        return remove(Wrappers.<SysRoleUser>lambdaQuery()
                .eq(SysRoleUser::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserRole(long userId, List<Long> roles) {
        int userCount = count(Wrappers.<SysRoleUser>lambdaQuery().eq(SysRoleUser::getUserId, userId));
        if (userCount != 0) {
            assertI18nService.checkOperation(removeByUserId(userId),
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
        assertI18nService.checkOperation(result,
                "SysRoleUserServiceImpl.UpdateRoleFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void userBindRoles(RelationDto<Long, Long> params) {
        bindRoles(params,
                (roleId, targetId) -> remove(Wrappers.<SysRoleUser>lambdaQuery()
                        .eq(SysRoleUser::getRoleId, roleId)
                        .eq(SysRoleUser::getUserId, targetId)),
                (roleId, targetId) -> {
                    getBaseMapper().insertIgnore(roleId, targetId);
                    return true;
                }, "SysRoleUserServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindUsers(RelationDto<Long, Long> params) {
        bindTargets(params,
                (roleId, targetId) -> remove(Wrappers.<SysRoleUser>lambdaQuery()
                        .eq(SysRoleUser::getRoleId, roleId)
                        .eq(SysRoleUser::getUserId, targetId)),
                (roleId, targetId) -> {
                    getBaseMapper().insertIgnore(roleId, targetId);
                    return true;
                }, "SysRoleUserServiceImpl.RemoveFailed");
    }

    @Override
    public IPage<SysUser> getRoleUsers(long roleId, Page<?> page, boolean isBind) {
        return baseMapper.getRoleUsers(page, roleId, isBind);
    }
}
