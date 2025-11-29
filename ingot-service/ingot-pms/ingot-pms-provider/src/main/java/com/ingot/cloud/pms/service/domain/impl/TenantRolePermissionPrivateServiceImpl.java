package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.TenantRolePermissionPrivate;
import com.ingot.cloud.pms.api.model.dto.common.BizBindDTO;
import com.ingot.cloud.pms.mapper.TenantRolePermissionPrivateMapper;
import com.ingot.cloud.pms.service.domain.TenantRolePermissionPrivateService;
import com.ingot.framework.commons.constants.CacheConstants;
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
public class TenantRolePermissionPrivateServiceImpl extends BaseServiceImpl<TenantRolePermissionPrivateMapper, TenantRolePermissionPrivate> implements TenantRolePermissionPrivateService {

    @Override
    @CacheEvict(
            value = CacheConstants.TENANT_ROLE_PERMISSIONS,
            key = "'role-' + #params.id"
    )
    @Transactional(rollbackFor = Exception.class)
    public void roleSetPermissions(BizBindDTO params) {
        Long roleId = params.getId();
        List<Long> bindIds = params.getAssignIds();
        boolean metaFlag = params.isMetaFlag();

        // 清空当前权限
        remove(Wrappers.<TenantRolePermissionPrivate>lambdaQuery()
                .eq(TenantRolePermissionPrivate::getRoleId, roleId));

        if (CollUtil.isNotEmpty(bindIds)) {
            List<TenantRolePermissionPrivate> bindList = getBindList(roleId, bindIds, metaFlag);
            saveBatch(bindList);
        }
    }

    @Override
    @CacheEvict(
            value = CacheConstants.TENANT_ROLE_PERMISSIONS,
            key = "'role-' + #params.id"
    )
    @Transactional(rollbackFor = Exception.class)
    public void roleAssignPermissions(BizBindDTO params) {
        Long roleId = params.getId();
        List<Long> bindIds = params.getAssignIds();
        List<Long> unbindIds = params.getUnassignIds();
        boolean metaFlag = params.isMetaFlag();

        if (CollUtil.isNotEmpty(unbindIds)) {
            remove(Wrappers.<TenantRolePermissionPrivate>lambdaQuery()
                    .eq(TenantRolePermissionPrivate::getRoleId, roleId)
                    .in(TenantRolePermissionPrivate::getPermissionId, unbindIds));
        }

        if (CollUtil.isNotEmpty(bindIds)) {
            List<TenantRolePermissionPrivate> bindList = getBindList(roleId, bindIds, metaFlag);
            saveBatch(bindList);
        }
    }

    private List<TenantRolePermissionPrivate> getBindList(long roleId, List<Long> bindIds, boolean metaFlag) {
        return CollUtil.emptyIfNull(bindIds).stream()
                .map(permissionId -> {
                    TenantRolePermissionPrivate bind = new TenantRolePermissionPrivate();
                    bind.setRoleId(roleId);
                    bind.setPermissionId(permissionId);
                    bind.setMetaRole(metaFlag);
                    return bind;
                }).toList();
    }

    @Override
    @Cacheable(
            value = CacheConstants.TENANT_ROLE_PERMISSIONS,
            key = "'role-' + #id",
            unless = "#result.isEmpty()"
    )
    public List<TenantRolePermissionPrivate> getRoleBindPermissionIds(long id) {
        return CollUtil.emptyIfNull(
                list(Wrappers.<TenantRolePermissionPrivate>lambdaQuery()
                        .eq(TenantRolePermissionPrivate::getRoleId, id))
        );
    }

    @Override
    @CacheEvict(
            value = CacheConstants.TENANT_ROLE_PERMISSIONS,
            allEntries = true
    )
    public void clearByPermissionId(long permissionId) {
        remove(Wrappers.<TenantRolePermissionPrivate>lambdaQuery()
                .eq(TenantRolePermissionPrivate::getPermissionId, permissionId));
    }

    @Override
    @CacheEvict(
            value = CacheConstants.TENANT_ROLE_PERMISSIONS,
            allEntries = true
    )
    public void clearByRoleId(long roleId) {
        remove(Wrappers.<TenantRolePermissionPrivate>lambdaQuery()
                .eq(TenantRolePermissionPrivate::getRoleId, roleId));
    }

    @Override
    @CacheEvict(
            value = CacheConstants.TENANT_ROLE_PERMISSIONS,
            allEntries = true
    )
    public void clearByTenantId(long tenantId) {
        remove(Wrappers.<TenantRolePermissionPrivate>lambdaQuery()
                .eq(TenantRolePermissionPrivate::getTenantId, tenantId));
    }
}
