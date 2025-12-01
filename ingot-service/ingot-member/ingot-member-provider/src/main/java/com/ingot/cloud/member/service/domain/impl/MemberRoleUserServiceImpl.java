package com.ingot.cloud.member.service.domain.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.domain.MemberRoleUser;
import com.ingot.cloud.member.mapper.MemberRoleUserMapper;
import com.ingot.cloud.member.service.domain.MemberRoleUserService;
import com.ingot.framework.commons.model.common.AssignDTO;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author jymot
 * @since 2025-11-29
 */
@Service
@RequiredArgsConstructor
public class MemberRoleUserServiceImpl extends BaseServiceImpl<MemberRoleUserMapper, MemberRoleUser> implements MemberRoleUserService {

    @Override
    public List<MemberRoleUser> getUserRoles(long userId) {
        return CollUtil.emptyIfNull(list(Wrappers.<MemberRoleUser>lambdaQuery()
                .eq(MemberRoleUser::getUserId, userId)));
    }

    @Override
    public List<MemberRoleUser> listRoleUsers(long roleId) {
        return CollUtil.emptyIfNull(list(Wrappers.<MemberRoleUser>lambdaQuery()
                        .eq(MemberRoleUser::getRoleId, roleId)))
                .stream()
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindUsers(AssignDTO<Long, Long> params) {
        Long roleId = params.getId();
        List<Long> bindIds = params.getAssignIds();
        List<Long> removeIds = params.getUnassignIds();

        if (CollUtil.isNotEmpty(removeIds)) {
            remove(Wrappers.<MemberRoleUser>lambdaQuery()
                    .eq(MemberRoleUser::getRoleId, roleId)
                    .in(MemberRoleUser::getUserId, removeIds));
        }

        if (CollUtil.isEmpty(bindIds)) {
            return;
        }

        // 避免重复绑定
        List<Long> alreadyExistsUserIds = CollUtil.emptyIfNull(list(Wrappers.<MemberRoleUser>lambdaQuery()
                .eq(MemberRoleUser::getRoleId, roleId)
                .in(MemberRoleUser::getUserId, bindIds))
                .stream().map(MemberRoleUser::getUserId).toList());

        List<MemberRoleUser> bindList = bindIds.stream()
                .filter(userId -> !alreadyExistsUserIds.contains(userId))
                .map(userId -> {
                    MemberRoleUser bind = new MemberRoleUser();
                    bind.setRoleId(roleId);
                    bind.setUserId(userId);
                    return bind;
                }).toList();
        if (CollUtil.isNotEmpty(bindList)) {
            saveBatch(bindList);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setRoles(long userId, List<Long> roles) {
        clearByUserId(userId);

        if (CollUtil.isEmpty(roles)) {
            return;
        }

        List<MemberRoleUser> roleUsers = roles.stream().map(item -> {
            MemberRoleUser roleUser = new MemberRoleUser();
            roleUser.setRoleId(item);
            roleUser.setUserId(userId);
            return roleUser;
        }).toList();

        saveBatch(roleUsers);
    }

    @Override
    public void clearByRoleId(long id) {
        remove(Wrappers.<MemberRoleUser>lambdaQuery()
                .eq(MemberRoleUser::getRoleId, id));
    }

    @Override
    public void clearByUserId(long userId) {
        remove(Wrappers.<MemberRoleUser>lambdaQuery()
                .eq(MemberRoleUser::getUserId, userId));
    }

    @Override
    public void clearByTenantId(long tenantId) {
        remove(Wrappers.<MemberRoleUser>lambdaQuery()
                .eq(MemberRoleUser::getTenantId, tenantId));
    }
}
