package com.ingot.cloud.pms.service.biz.impl;

import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.dto.org.CreateOrgDTO;
import com.ingot.cloud.pms.api.model.enums.DeptRoleScopeEnum;
import com.ingot.cloud.pms.core.BizIdGen;
import com.ingot.cloud.pms.core.TenantEngine;
import com.ingot.cloud.pms.service.biz.BizOrgService;
import com.ingot.cloud.pms.service.domain.SysDeptService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>Description  : BizOrgServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/21.</p>
 * <p>Time         : 11:09 AM.</p>
 */
@Service
@RequiredArgsConstructor
public class BizOrgServiceImpl implements BizOrgService {
    private final SysTenantService sysTenantService;
    private final SysDeptService sysDeptService;
    private final BizIdGen bizIdGen;
    private final TenantEngine tenantEngine;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createOrg(CreateOrgDTO params) {
        // 1. 创建tenant
        SysTenant tenant = createTenant(params);

        // 2. 创建部门
        SysDept dept = createDept(tenant);

        // 3. 创建角色，角色组
        createRoles(tenant);

        // 4. 创建权限

        // 5. 创建默认用户

        // 6. 角色绑定权限

        // 7. 用户绑定部门，用户绑定角色
        // todo
    }

    @Override
    public void removeOrg(long id) {

    }

    private SysTenant createTenant(CreateOrgDTO params) {
        String orgCode = bizIdGen.genOrgCode();
        SysTenant tenant = new SysTenant();
        tenant.setName(params.getName());
        tenant.setAvatar(params.getAvatar());
        tenant.setCode(orgCode);
        sysTenantService.createTenant(tenant);
        return tenant;
    }

    private SysDept createDept(SysTenant tenant) {
        return TenantEnv.applyAs(tenant.getId(), () -> {
            SysDept dept = new SysDept();
            dept.setName(tenant.getName());
            dept.setScope(DeptRoleScopeEnum.CURRENT_CHILD);
            dept.setMainFlag(Boolean.TRUE);
            sysDeptService.createDept(dept);
            return dept;
        });
    }

    private void createRoles(SysTenant tenant) {

    }
}
