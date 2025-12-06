package com.ingot.cloud.member.service.domain.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.member.api.model.domain.MemberRolePermission;
import com.ingot.cloud.member.mapper.MemberRolePermissionMapper;
import com.ingot.cloud.member.service.domain.MemberRolePermissionService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.model.common.AssignDTO;
import com.ingot.framework.commons.model.common.SetDTO;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
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
public class MemberRolePermissionServiceImpl extends BaseServiceImpl<MemberRolePermissionMapper, MemberRolePermission> implements MemberRolePermissionService {
    @Override
    @CacheEvict(
            value = CacheConstants.MEMBER_ROLE_PERMISSIONS,
            key = "'role-' + #params.id"
    )
    @Transactional(rollbackFor = Exception.class)
    public void roleSetPermissions(SetDTO<Long, Long> params) {
        Long roleId = params.getId();
        List<Long> bindIds = params.getSetIds();

        // 清空当前权限
        remove(Wrappers.<MemberRolePermission>lambdaQuery()
                .eq(MemberRolePermission::getRoleId, roleId));

        if (CollUtil.isNotEmpty(bindIds)) {
            List<MemberRolePermission> bindList = getBindList(roleId, bindIds);
            saveBatch(bindList);
        }
    }

    @Override
    @CacheEvict(
            value = CacheConstants.MEMBER_ROLE_PERMISSIONS,
            key = "'role-' + #params.id"
    )
    @Transactional(rollbackFor = Exception.class)
    public void roleAssignPermissions(AssignDTO<Long, Long> params) {
        Long roleId = params.getId();
        List<Long> bindIds = params.getAssignIds();
        List<Long> unbindIds = params.getUnassignIds();

        if (CollUtil.isNotEmpty(unbindIds)) {
            remove(Wrappers.<MemberRolePermission>lambdaQuery()
                    .eq(MemberRolePermission::getRoleId, roleId)
                    .in(MemberRolePermission::getPermissionId, unbindIds));
        }

        if (CollUtil.isNotEmpty(bindIds)) {
            List<MemberRolePermission> bindList = getBindList(roleId, bindIds);
            saveBatch(bindList);
        }
    }

    private List<MemberRolePermission> getBindList(long roleId, List<Long> bindIds) {
        return CollUtil.emptyIfNull(bindIds).stream()
                .map(permissionId -> {
                    MemberRolePermission bind = new MemberRolePermission();
                    bind.setRoleId(roleId);
                    bind.setPermissionId(permissionId);
                    return bind;
                }).toList();
    }

    @Override
    @Cacheable(
            value = CacheConstants.MEMBER_ROLE_PERMISSIONS,
            key = "'role-' + #id",
            unless = "#result.isEmpty()"
    )
    public List<Long> getRoleBindPermissionIds(long id) {
        return CollUtil.emptyIfNull(
                list(Wrappers.<MemberRolePermission>lambdaQuery()
                        .eq(MemberRolePermission::getRoleId, id))
                        .stream()
                        .map(MemberRolePermission::getPermissionId)
                        .toList()
        );
    }

    @Override
    @CacheEvict(
            value = CacheConstants.MEMBER_ROLE_PERMISSIONS,
            allEntries = true
    )
    public void clearByPermissionId(long permissionId) {
        remove(Wrappers.<MemberRolePermission>lambdaQuery()
                .eq(MemberRolePermission::getPermissionId, permissionId));
    }

    @Override
    @CacheEvict(
            value = CacheConstants.MEMBER_ROLE_PERMISSIONS,
            allEntries = true
    )
    public void clearByRoleId(long roleId) {
        remove(Wrappers.<MemberRolePermission>lambdaQuery()
                .eq(MemberRolePermission::getRoleId, roleId));
    }

    @Override
    @CacheEvict(
            value = CacheConstants.MEMBER_ROLE_PERMISSIONS,
            allEntries = true
    )
    public void clearByTenantId(long tenantId) {
        remove(Wrappers.<MemberRolePermission>lambdaQuery()
                .eq(MemberRolePermission::getTenantId, tenantId));
    }
}
