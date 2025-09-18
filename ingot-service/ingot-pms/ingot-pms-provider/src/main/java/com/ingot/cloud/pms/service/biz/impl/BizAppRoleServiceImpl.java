package com.ingot.cloud.pms.service.biz.impl;

import java.util.List;

import com.ingot.cloud.pms.api.model.domain.AppRole;
import com.ingot.cloud.pms.api.model.domain.AppRoleGroup;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.core.org.TenantOps;
import com.ingot.cloud.pms.service.biz.BizAppRoleService;
import com.ingot.cloud.pms.service.domain.AppRoleGroupService;
import com.ingot.cloud.pms.service.domain.AppRoleService;
import com.ingot.cloud.pms.service.domain.AppRoleUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizAppRoleServiceImpl.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2024/1/26.</p>
 * <p>Time         : 16:02.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizAppRoleServiceImpl implements BizAppRoleService {
    private final AppRoleService appRoleService;
    private final AppRoleGroupService appRoleGroupService;
    private final AppRoleUserService appRoleUserService;
    private final TenantOps tenantOps;

    @Override
    public void setOrgUserRoles(long userId, List<Long> roles) {
        appRoleUserService.setUserRoles(userId, roles);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRoleEffectOrg(AppRole role, boolean isAdmin) {
        appRoleService.createRole(role, isAdmin);
        if (role.getType() == OrgTypeEnum.Tenant) {
            tenantOps.createRole(role);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoleEffectOrg(AppRole role, boolean isAdmin) {
        appRoleService.updateRoleById(role, isAdmin);
        AppRole current = appRoleService.getById(role.getId());
        if (current.getType() == OrgTypeEnum.Tenant) {
            tenantOps.updateRole(role);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRoleEffectOrg(long id, boolean isAdmin) {
        AppRole current = appRoleService.getById(id);
        appRoleService.removeRoleById(id, isAdmin);
        if (current.getType() == OrgTypeEnum.Tenant) {
            tenantOps.removeRole(current);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createRoleGroupEffectOrg(AppRoleGroup group, boolean isAdmin) {
        appRoleService.createGroup(group, isAdmin);
        if (group.getType() == OrgTypeEnum.Tenant) {
            tenantOps.createRoleGroup(group);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRoleGroupEffectOrg(AppRoleGroup group, boolean isAdmin) {
        appRoleService.updateGroup(group, isAdmin);
        AppRoleGroup current = appRoleGroupService.getById(group.getId());
        if (current.getType() == OrgTypeEnum.Tenant) {
            tenantOps.updateRoleGroup(group);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRoleGroupEffectOrg(long id, boolean isAdmin) {
        AppRoleGroup current = appRoleGroupService.getById(id);
        appRoleService.deleteGroup(id, isAdmin);
        if (current.getType() == OrgTypeEnum.Tenant) {
            tenantOps.removeRoleGroup(current);
        }
    }
}
