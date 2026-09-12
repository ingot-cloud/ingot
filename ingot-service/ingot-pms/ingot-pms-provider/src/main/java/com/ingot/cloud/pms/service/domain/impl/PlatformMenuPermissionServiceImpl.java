package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.PlatformMenuPermission;
import com.ingot.cloud.pms.mapper.PlatformMenuPermissionMapper;
import com.ingot.cloud.pms.service.domain.PlatformMenuPermissionService;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.data.mybatis.common.service.BaseServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>{@link PlatformMenuPermissionService} 的默认实现。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
public class PlatformMenuPermissionServiceImpl
        extends BaseServiceImpl<PlatformMenuPermissionMapper, PlatformMenuPermission>
        implements PlatformMenuPermissionService {

    @Override
    @Cacheable(
            value = CacheConstants.PLATFORM_MENU_PERMISSIONS,
            key = "'menu-' + #menuId",
            unless = "#result.isEmpty()"
    )
    public List<Long> listPermissionIds(long menuId) {
        return CollUtil.emptyIfNull(list(Wrappers.<PlatformMenuPermission>lambdaQuery()
                        .eq(PlatformMenuPermission::getMenuId, menuId)))
                .stream()
                .map(PlatformMenuPermission::getPermissionId)
                .toList();
    }

    @Override
    public java.util.Map<Long, List<Long>> mapPermissionIds() {
        return CollUtil.emptyIfNull(list()).stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        PlatformMenuPermission::getMenuId,
                        java.util.stream.Collectors.mapping(
                                PlatformMenuPermission::getPermissionId,
                                java.util.stream.Collectors.toList())));
    }

    @Override
    @CacheEvict(
            value = CacheConstants.PLATFORM_MENU_PERMISSIONS,
            key = "'menu-' + #menuId"
    )
    @Transactional(rollbackFor = Exception.class)
    public void replace(long menuId, List<Long> permissionIds) {
        clearByMenuId(menuId);
        if (CollUtil.isEmpty(permissionIds)) {
            return;
        }
        List<PlatformMenuPermission> rows = permissionIds.stream()
                .distinct()
                .map(permissionId -> {
                    PlatformMenuPermission row = new PlatformMenuPermission();
                    row.setMenuId(menuId);
                    row.setPermissionId(permissionId);
                    return row;
                })
                .toList();
        saveBatch(rows);
    }

    @Override
    @CacheEvict(
            value = CacheConstants.PLATFORM_MENU_PERMISSIONS,
            key = "'menu-' + #menuId"
    )
    public void clearByMenuId(long menuId) {
        remove(Wrappers.<PlatformMenuPermission>lambdaQuery()
                .eq(PlatformMenuPermission::getMenuId, menuId));
    }

    @Override
    @CacheEvict(
            value = CacheConstants.PLATFORM_MENU_PERMISSIONS,
            allEntries = true
    )
    public void clearByPermissionId(long permissionId) {
        remove(Wrappers.<PlatformMenuPermission>lambdaQuery()
                .eq(PlatformMenuPermission::getPermissionId, permissionId));
    }
}
