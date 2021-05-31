package com.ingot.cloud.pms.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.SysRoleUser;
import com.ingot.cloud.pms.common.CommonRoleRelation;
import com.ingot.cloud.pms.mapper.SysRoleUserMapper;
import com.ingot.cloud.pms.service.SysRoleUserService;
import com.ingot.framework.core.model.dto.common.RelationDto;
import com.ingot.framework.core.validation.service.AssertI18nService;
import com.ingot.framework.store.mybatis.service.BaseServiceImpl;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class SysRoleUserServiceImpl extends BaseServiceImpl<SysRoleUserMapper, SysRoleUser> implements SysRoleUserService {
    private static final int BIND_TYPE_ROLE = 1;
    private static final int BIND_TYPE_USER = 2;

    private final AssertI18nService assertI18nService;

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
        CommonRoleRelation.bind(CommonRoleRelation.TYPE_TARGET, params,
                (roleId, targetId) -> remove(Wrappers.<SysRoleUser>lambdaQuery()
                        .eq(SysRoleUser::getRoleId, roleId)
                        .eq(SysRoleUser::getUserId, targetId)),
                (roleId, targetId) -> {
                    getBaseMapper().insertIgnore(roleId, targetId);
                    return true;
                },
                assertI18nService, "SysRoleUserServiceImpl.RemoveFailed");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindUsers(RelationDto<Long, Long> params) {
        CommonRoleRelation.bind(CommonRoleRelation.TYPE_ROLE, params,
                (roleId, targetId) -> remove(Wrappers.<SysRoleUser>lambdaQuery()
                        .eq(SysRoleUser::getRoleId, roleId)
                        .eq(SysRoleUser::getUserId, targetId)),
                (roleId, targetId) -> {
                    getBaseMapper().insertIgnore(roleId, targetId);
                    return true;
                },
                assertI18nService, "SysRoleUserServiceImpl.RemoveFailed");
    }
}
