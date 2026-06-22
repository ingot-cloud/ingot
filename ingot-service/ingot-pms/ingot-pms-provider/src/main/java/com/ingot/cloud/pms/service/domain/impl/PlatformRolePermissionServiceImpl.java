package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.PlatformRolePermission;
import com.ingot.cloud.pms.mapper.PlatformRolePermissionMapper;
import com.ingot.cloud.pms.service.domain.PlatformRolePermissionService;
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
 * @since 2025-11-12
 */
@Service
public class PlatformRolePermissionServiceImpl extends BaseServiceImpl<PlatformRolePermissionMapper, PlatformRolePermission> implements PlatformRolePermissionService {

    @Override
    @CacheEvict(
            value = CacheConstants.PLATFORM_ROLE_PERMISSIONS,
            key = "'role-' + #params.id"
    )
    @Transactional(rollbackFor = Exception.class)
    public void roleSetPermissions(SetDTO<Long, Long> params) {
        Long roleId = params.getId();
        List<Long> bindIds = params.getSetIds();

        // 清空当前权限
        remove(Wrappers.<PlatformRolePermission>lambdaQuery()
                .eq(PlatformRolePermission::getRoleId, roleId));

        if (CollUtil.isNotEmpty(bindIds)) {
            List<PlatformRolePermission> bindList = getBindList(roleId, bindIds);
            saveBatch(bindList);
        }
    }

    @Override
    @CacheEvict(
            value = CacheConstants.PLATFORM_ROLE_PERMISSIONS,
            key = "'role-' + #params.id"
    )
    @Transactional(rollbackFor = Exception.class)
    public void roleAssignPermissions(AssignDTO<Long, Long> params) {
        Long roleId = params.getId();
        List<Long> bindIds = params.getAssignIds();
        List<Long> unbindIds = params.getUnassignIds();

        if (CollUtil.isNotEmpty(unbindIds)) {
            remove(Wrappers.<PlatformRolePermission>lambdaQuery()
                    .eq(PlatformRolePermission::getRoleId, roleId)
                    .in(PlatformRolePermission::getPermissionId, unbindIds));
        }

        if (CollUtil.isNotEmpty(bindIds)) {
            List<PlatformRolePermission> bindList = getBindList(roleId, bindIds);
            saveBatch(bindList);
        }
    }

    private List<PlatformRolePermission> getBindList(long roleId, List<Long> bindIds) {
        return CollUtil.emptyIfNull(bindIds).stream()
                .map(permissionId -> {
                    PlatformRolePermission bind = new PlatformRolePermission();
                    bind.setRoleId(roleId);
                    bind.setPermissionId(permissionId);
                    return bind;
                }).toList();
    }

    @Override
    @Cacheable(
            value = CacheConstants.PLATFORM_ROLE_PERMISSIONS,
            key = "'role-' + #id",
            unless = "#result.isEmpty()"
    )
    public List<Long> getRoleBindPermissionIds(long id) {
        return CollUtil.emptyIfNull(
                list(Wrappers.<PlatformRolePermission>lambdaQuery()
                        .eq(PlatformRolePermission::getRoleId, id))
                        .stream()
                        .map(PlatformRolePermission::getPermissionId)
                        .toList()
        );
    }

    @Override
    @CacheEvict(
            value = CacheConstants.PLATFORM_ROLE_PERMISSIONS,
            allEntries = true
    )
    public void clearByPermissionId(long permissionId) {
        remove(Wrappers.<PlatformRolePermission>lambdaQuery()
                .eq(PlatformRolePermission::getPermissionId, permissionId));
    }

    @Override
    @CacheEvict(
            value = CacheConstants.PLATFORM_ROLE_PERMISSIONS,
            allEntries = true
    )
    public void clearByPermissionIds(List<Long> ids) {
        remove(Wrappers.<PlatformRolePermission>lambdaQuery()
                .in(PlatformRolePermission::getPermissionId, ids));
    }

    @Override
    @CacheEvict(
            value = CacheConstants.PLATFORM_ROLE_PERMISSIONS,
            allEntries = true
    )
    public void clearByRoleId(long roleId) {
        remove(Wrappers.<PlatformRolePermission>lambdaQuery()
                .eq(PlatformRolePermission::getRoleId, roleId));
    }


}
