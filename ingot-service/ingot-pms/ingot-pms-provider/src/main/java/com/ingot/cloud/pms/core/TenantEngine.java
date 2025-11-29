package com.ingot.cloud.pms.core;

import java.util.List;

import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.AppUserTenant;
import com.ingot.cloud.pms.api.model.domain.SysTenant;
import com.ingot.cloud.pms.api.model.domain.SysUser;
import com.ingot.cloud.pms.api.model.domain.TenantDept;
import com.ingot.cloud.pms.api.model.dto.org.CreateOrgDTO;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.data.redis.utils.RedisUtils;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : TenantEngine.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/8/1.</p>
 * <p>Time         : 4:25 PM.</p>
 */
@Component
@RequiredArgsConstructor
public class TenantEngine {
    private final SysTenantService sysTenantService;
    private final SysUserService sysUserService;
    private final SysUserTenantService sysUserTenantService;
    private final AppUserTenantService appUserTenantService;
    private final AppRoleService appRoleService;
    private final AppRoleUserService appRoleUserService;


    private final TenantAppConfigService tenantAppConfigService;
    private final TenantDeptService tenantDeptService;
    private final TenantRolePermissionPrivateService tenantRolePermissionPrivateService;
    private final TenantRolePrivateService tenantRolePrivateService;
    private final TenantRoleUserPrivateService tenantRoleUserPrivateService;
    private final TenantUserDeptPrivateService tenantUserDeptPrivateService;

    private final BizUserService bizUserService;
    private final BizRoleService bizRoleService;

    private final BizIdGen bizIdGen;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 创建租户
     */
    public SysTenant createTenant(CreateOrgDTO params) {
        String orgCode = bizIdGen.genOrgCode();
        SysTenant tenant = new SysTenant();
        tenant.setName(params.getName());
        tenant.setAvatar(params.getAvatar());
        tenant.setCode(orgCode);
        // 只能创建租户类型
        tenant.setOrgType(OrgTypeEnum.Tenant);
        sysTenantService.createTenant(tenant);
        return tenant;
    }

    /**
     * 创建租户部门<br>
     * 创建以租户名称命名的部门<br>
     */
    public TenantDept createTenantDept(SysTenant tenant) {
        return TenantEnv.applyAs(tenant.getId(), () -> {
            // 1. 创建主部门(当前组织名称的部门)
            TenantDept main = new TenantDept();
            main.setName(tenant.getName());
            main.setMainFlag(Boolean.TRUE);
            main.setSort(0);
            tenantDeptService.create(main);
            return main;
        });
    }

    /**
     * 初始化租户管理员
     */
    public void initTenantManager(CreateOrgDTO params, SysTenant tenant, TenantDept dept) {
        TenantEnv.runAs(tenant.getId(), () -> {
            // 如果已经存在注册用户，那么直接关联新组织信息
            SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPhone, params.getPhone()));
            if (user == null) {
                user = new SysUser();
                user.setInitPwd(Boolean.TRUE);
                user.setUsername(params.getPhone());
                user.setPhone(params.getPhone());
                user.setPassword(params.getPhone());
                user.setNickname(params.getPhone());
                sysUserService.create(user);
            }

            RoleType managerRole = bizRoleService.getByCode(RoleConstants.ROLE_ORG_ADMIN_CODE);

            // 加入租户
            sysUserTenantService.joinTenant(user.getId(), tenant);
            // 设置部门
            tenantUserDeptPrivateService.setDepartments(user.getId(), List.of(dept.getId()));
            // 设置主角色
            bizUserService.setUserRoles(user.getId(), List.of(managerRole.getId()));
        });
    }

    /**
     * 销毁所有关联信息
     *
     * @param id 租户ID
     */
    public void destroy(long id) {
        TenantEnv.runAs(id, () -> {
            // 移除组织
            sysTenantService.removeTenantById(id);

            // 系统用户取消关联组织
            sysUserTenantService.clearByTenantId(id);
            // 清空app配置
            tenantAppConfigService.clearByAppId(id);
            // 清空部门
            tenantDeptService.clearByTenantId(id);
            // 清空角色权限配置
            tenantRolePermissionPrivateService.clearByTenantId(id);
            // 清空角色
            tenantRolePrivateService.clearByTenantId(id);
            // 清空用户角色关联关系
            tenantRoleUserPrivateService.clearByTenantId(id);
            // 清空用户部门关联关系
            tenantUserDeptPrivateService.clearByTenantId(id);

            // app用户取消关联组织信息
            appUserTenantService.remove(Wrappers.<AppUserTenant>lambdaQuery()
                    .eq(AppUserTenant::getTenantId, id));

            // 取消关联角色
            appRoleUserService.remove(Wrappers.lambdaQuery());

            appRoleService.remove(Wrappers.lambdaQuery());

            // clear cache
            RedisUtils.deleteKeys(redisTemplate, ListUtil.list(false, "*"));
        });
    }
}
