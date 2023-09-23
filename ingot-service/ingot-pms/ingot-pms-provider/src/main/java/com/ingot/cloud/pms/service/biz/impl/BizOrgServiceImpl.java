package com.ingot.cloud.pms.service.biz.impl;

import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.dto.org.CreateOrgDTO;
import com.ingot.cloud.pms.core.TenantEngine;
import com.ingot.cloud.pms.service.biz.BizOrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>Description  : BizOrgServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/21.</p>
 * <p>Time         : 11:09 AM.</p>
 */
@Service
@RequiredArgsConstructor
public class BizOrgServiceImpl implements BizOrgService {
    private final TenantEngine tenantEngine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrg(CreateOrgDTO params) {
        // 1. 创建tenant
        SysTenant tenant = tenantEngine.createTenant(params);

        // 2. 创建部门
        SysDept dept = tenantEngine.createTenantDept(tenant);

        // 3. 创建角色，角色组
        List<SysRole> roles = tenantEngine.createTenantRoles(tenant);

        // 4. 创建权限
        List<SysAuthority> authorities = tenantEngine.createTenantAuthorityAndMenu(tenant);

        // 5. 创建默认用户, 设置部门，设置角色
        tenantEngine.createTenantUser(params, tenant, roles, dept);

        // 6. 角色绑定权限
        tenantEngine.tenantRoleBindAuthorities(roles, authorities);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeOrg(long id) {
        // 1. 用户取消关联组织，部门，角色
        tenantEngine.removeTenantUserRelation(id);

        // 2. 移除组织，移除部门
        tenantEngine.removeTenantAndDept(id);

        // 3. 移除权限，移除角色
        tenantEngine.removeTenantAuthorityAndRole(id);
    }

}
