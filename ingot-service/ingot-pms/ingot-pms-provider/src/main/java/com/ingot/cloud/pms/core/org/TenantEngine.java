package com.ingot.cloud.pms.core.org;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.convert.AuthorityConvert;
import com.ingot.cloud.pms.api.model.convert.DeptConvert;
import com.ingot.cloud.pms.api.model.convert.MenuConvert;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.dto.org.CreateOrgDTO;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnum;
import com.ingot.cloud.pms.api.model.types.RoleType;
import com.ingot.cloud.pms.core.BizIdGen;
import com.ingot.cloud.pms.service.biz.BizDeptService;
import com.ingot.cloud.pms.service.biz.BizRoleService;
import com.ingot.cloud.pms.service.biz.BizUserService;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.commons.constants.CacheConstants;
import com.ingot.framework.commons.constants.RoleConstants;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.data.redis.utils.RedisUtils;
import com.ingot.framework.tenant.TenantEnv;
import com.ingot.framework.tenant.properties.TenantProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
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
    private final SysDeptService sysDeptService;
    private final SysRoleService sysRoleService;
    private final SysRoleGroupService sysRoleGroupService;
    private final SysAuthorityService sysAuthorityService;
    private final SysUserService sysUserService;
    private final SysUserTenantService sysUserTenantService;
    private final SysUserDeptService sysUserDeptService;
    private final SysRoleUserService sysRoleUserService;
    private final SysRoleUserDeptService sysRoleUserDeptService;
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final SysMenuService sysMenuService;
    private final AppUserTenantService appUserTenantService;
    private final AppRoleService appRoleService;
    private final AppRoleUserService appRoleUserService;
    private final SysApplicationTenantService sysApplicationTenantService;


    private final TenantDeptService tenantDeptService;
    private final TenantUserDeptPrivateService tenantUserDeptPrivateService;

    private final BizUserService bizUserService;
    private final BizRoleService bizRoleService;

    private final BizIdGen bizIdGen;
    private final RedisTemplate<String, Object> redisTemplate;
    private final BizDeptService bizDeptService;

    /**
     * 创建租户
     */
    public SysTenant createTenant(CreateOrgDTO params) {
        String orgCode = bizIdGen.genOrgCode();
        SysTenant tenant = new SysTenant();
        tenant.setName(params.getName());
        tenant.setAvatar(params.getAvatar());
        tenant.setCode(orgCode);
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
    @CacheEvict(value = {CacheConstants.MENU_DETAILS, CacheConstants.AUTHORITY_DETAILS}, allEntries = true)
    public void destroy(long id) {
        TenantEnv.runAs(id, () -> {
            // 系统用户取消关联组织
            sysUserTenantService.remove(Wrappers.<SysUserTenant>lambdaQuery()
                    .eq(SysUserTenant::getTenantId, id));

            // 取消组织用户关联部门
            sysUserDeptService.remove(Wrappers.lambdaQuery());
            // 取消组织用户关联角色
            sysRoleUserService.remove(Wrappers.lambdaQuery());
            // 取消组织角色用户部门关联关系
            sysRoleUserDeptService.remove(Wrappers.lambdaQuery());

            // app用户取消关联组织信息
            appUserTenantService.remove(Wrappers.<AppUserTenant>lambdaQuery()
                    .eq(AppUserTenant::getTenantId, id));
            // 取消关联角色
            appRoleUserService.remove(Wrappers.lambdaQuery());

            // 移除组织
            sysTenantService.removeTenantById(id);
            // 移除部门
            sysDeptService.remove(Wrappers.lambdaQuery());
            // 移除菜单
            sysMenuService.remove(Wrappers.lambdaQuery());
            // 移除权限
            sysAuthorityService.remove(Wrappers.lambdaQuery());
            // 移除应用
            sysApplicationTenantService.remove(Wrappers.lambdaQuery());

            sysRoleService.remove(Wrappers.<SysRole>lambdaQuery()
                    .eq(SysRole::getTenantId, id));

            sysRoleGroupService.remove(Wrappers.<SysRoleGroup>lambdaQuery()
                    .eq(SysRoleGroup::getTenantId, id));

            sysRoleAuthorityService.remove(Wrappers.lambdaQuery());

            appRoleService.remove(Wrappers.<AppRole>lambdaQuery()
                    .eq(AppRole::getTenantId, id));

            // clear cache
            RedisUtils.deleteKeys(redisTemplate, ListUtil.list(false, "*"));
        });
    }
}
