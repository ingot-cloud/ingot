package com.ingot.cloud.pms.service.biz.impl;

import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.pms.api.model.domain.SysAuthority;
import com.ingot.cloud.pms.api.model.domain.SysDept;
import com.ingot.cloud.pms.api.model.domain.SysRole;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.dto.org.CreateOrgDTO;
import com.ingot.cloud.pms.core.org.TenantEngine;
import com.ingot.cloud.pms.service.biz.BizOrgService;
import com.ingot.cloud.pms.service.domain.AppUserTenantService;
import com.ingot.cloud.pms.service.domain.SysTenantService;
import com.ingot.cloud.pms.service.domain.SysUserTenantService;
import com.ingot.framework.core.constants.OrgConstants;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>Description  : BizOrgServiceImpl.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/9/21.</p>
 * <p>Time         : 11:09 AM.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BizOrgServiceImpl implements BizOrgService {
    private final TenantEngine tenantEngine;
    private final SysTenantService sysTenantService;
    private final SysUserTenantService sysUserTenantService;
    private final AppUserTenantService appUserTenantService;
    private final AssertionChecker assertionChecker;

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
        tenantEngine.tenantRoleBindAuthorities(tenant, roles, authorities);
    }

    @Override
    public void updateBase(SysTenant params) {
        if (params.getStatus() != null && params.getStatus() == CommonStatusEnum.LOCK) {
            SysTenant org = sysTenantService.getById(params.getId());
            String code = org.getCode();
            // 平台默认组织不可更新状态
            if (StrUtil.equals(code, OrgConstants.INGOT_CLOUD_CODE)) {
                assertionChecker.checkOperation(!StrUtil.equals(code, OrgConstants.INGOT_CLOUD_CODE),
                        "Platform.canNotDisableIngotOrg");
            }
        }

        sysTenantService.updateTenantById(params);

        if (StrUtil.isNotEmpty(params.getName())) {
            sysUserTenantService.updateBase(params);
            appUserTenantService.updateBase(params);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeOrg(long id) {
        SysTenant org = sysTenantService.getById(id);
        String code = org.getCode();
        assertionChecker.checkOperation(!StrUtil.equals(code, OrgConstants.INGOT_CLOUD_CODE), "Platform.canNotRemoveIngotOrg");

        // 销毁组织
        TenantEnv.runAs(id, () -> tenantEngine.destroy(id));
    }

    public static void main(String[] args) {
        String password = "111111";
        PasswordEncoder encode = new BCryptPasswordEncoder();
        System.out.println(encode.encode(password));
    }
}
