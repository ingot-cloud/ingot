package com.ingot.cloud.pms.service.domain.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.bo.role.BizAssignRoleBO;
import com.ingot.cloud.pms.api.model.bo.role.BizRoleAssignUsersBO;
import com.ingot.cloud.pms.api.model.convert.RoleConvert;
import com.ingot.cloud.pms.api.model.domain.TenantRoleUserPrivate;
import com.ingot.cloud.pms.mapper.TenantRoleUserPrivateMapper;
import com.ingot.cloud.pms.service.domain.TenantRoleUserPrivateService;
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
 * @since 2025-11-12
 */
@Service
@RequiredArgsConstructor
public class TenantRoleUserPrivateServiceImpl extends BaseServiceImpl<TenantRoleUserPrivateMapper, TenantRoleUserPrivate> implements TenantRoleUserPrivateService {
    private final RoleConvert roleConvert;

    @Override
    public List<TenantRoleUserPrivate> getUserRoles(long userId) {
        return CollUtil.emptyIfNull(list(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                .eq(TenantRoleUserPrivate::getUserId, userId)));
    }

    @Override
    public List<TenantRoleUserPrivate> listRoleUsers(long roleId) {
        return CollUtil.emptyIfNull(list(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                        .eq(TenantRoleUserPrivate::getRoleId, roleId)))
                .stream()
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void roleBindUsers(BizRoleAssignUsersBO params) {
        Long roleId = params.getId();
        List<Long> bindIds = params.getAssignIds();
        List<Long> removeIds = params.getUnassignIds();
        boolean metaFlag = params.isMetaFlag();
        Long deptId = params.getDeptId();

        if (CollUtil.isNotEmpty(removeIds)) {
            remove(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                    .eq(deptId != null, TenantRoleUserPrivate::getDeptId, deptId)
                    .eq(TenantRoleUserPrivate::getRoleId, roleId)
                    .in(TenantRoleUserPrivate::getUserId, removeIds));
        }

        if (CollUtil.isEmpty(bindIds)) {
            return;
        }

        // 避免重复绑定
        List<Long> alreadyExistsUserIds = CollUtil.emptyIfNull(list(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                .eq(TenantRoleUserPrivate::getRoleId, roleId)
                .in(TenantRoleUserPrivate::getUserId, bindIds))
                .stream().map(TenantRoleUserPrivate::getUserId).toList());

        List<TenantRoleUserPrivate> bindList = bindIds.stream()
                .filter(userId -> !alreadyExistsUserIds.contains(userId))
                .map(userId -> {
                    TenantRoleUserPrivate bind = new TenantRoleUserPrivate();
                    bind.setRoleId(roleId);
                    bind.setUserId(userId);
                    bind.setMetaRole(metaFlag);
                    bind.setDeptId(deptId);
                    return bind;
                }).toList();
        if (CollUtil.isNotEmpty(bindList)) {
            saveBatch(bindList);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setRoles(long userId, List<BizAssignRoleBO> roles) {
        long userCount = count(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                .eq(TenantRoleUserPrivate::getUserId, userId));
        if (userCount != 0) {
            clearByUserId(userId);
        }

        if (CollUtil.isEmpty(roles)) {
            return;
        }

        List<TenantRoleUserPrivate> roleUsers = roles.stream().map(item -> {
            TenantRoleUserPrivate roleUser = roleConvert.to(item);
            roleUser.setUserId(userId);
            return roleUser;
        }).toList();

        saveBatch(roleUsers);
    }

    @Override
    public void clearByRoleId(long id) {
        remove(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                .eq(TenantRoleUserPrivate::getRoleId, id));
    }

    @Override
    public void clearByUserId(long userId) {
        remove(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                .eq(TenantRoleUserPrivate::getUserId, userId));
    }

    @Override
    public void clearByDeptId(long deptId) {
        remove(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                .eq(TenantRoleUserPrivate::getDeptId, deptId));
    }

    @Override
    public void clearByRoleAndDept(long roleId, long deptId) {
        remove(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                .eq(TenantRoleUserPrivate::getRoleId, roleId)
                .eq(TenantRoleUserPrivate::getDeptId, deptId));
    }

    @Override
    public void clearByTenantId(long tenantId) {
        remove(Wrappers.<TenantRoleUserPrivate>lambdaQuery()
                .eq(TenantRoleUserPrivate::getTenantId, tenantId));
    }
}
