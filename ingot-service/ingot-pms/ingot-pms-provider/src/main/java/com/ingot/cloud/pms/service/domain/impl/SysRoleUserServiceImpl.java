package com.ingot.cloud.pms.service.domain.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
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
    public boolean updateUserRole(long userId, List<Long> roles) {
        boolean result = removeByUserId(userId);
        if (!result) {
            return false;
        }
        if (CollUtil.isEmpty(roles)) {
            return true;
        }
        return roles.stream().allMatch(roleId -> {
            SysRoleUser entity = new SysRoleUser();
            entity.setUserId(userId);
            entity.setRoleId(roleId);
            return entity.insert();
        });
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
}
