package com.ingot.cloud.pms.core.org;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.dto.org.CreateOrgDTO;
import com.ingot.cloud.pms.api.model.enums.DeptRoleScopeEnum;
import com.ingot.cloud.pms.api.model.enums.OrgTypeEnums;
import com.ingot.cloud.pms.api.model.transform.AuthorityTrans;
import com.ingot.cloud.pms.api.model.transform.MenuTrans;
import com.ingot.cloud.pms.core.BizIdGen;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.utils.DateUtils;
import com.ingot.framework.security.common.constants.RoleConstants;
import com.ingot.framework.tenant.TenantEnv;
import com.ingot.framework.tenant.properties.TenantProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

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
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final SysMenuService sysMenuService;
    private final AppUserTenantService appUserTenantService;
    private final AppRoleService appRoleService;
    private final AppRoleUserService appRoleUserService;
    private final SysApplicationService sysApplicationService;
    private final SysApplicationTenantService sysApplicationTenantService;

    private final BizIdGen bizIdGen;
    private final TenantProperties tenantProperties;
    private final AuthorityTrans authorityTrans;
    private final MenuTrans menuTrans;

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
     * 创建租户部门
     */
    public SysDept createTenantDept(SysTenant tenant) {
        return TenantEnv.applyAs(tenant.getId(), () -> {
            SysDept dept = new SysDept();
            dept.setName(tenant.getName());
            dept.setScope(DeptRoleScopeEnum.CURRENT_CHILD);
            dept.setMainFlag(Boolean.TRUE);
            sysDeptService.createDept(dept);
            return dept;
        });
    }

    /**
     * 创建租户角色
     */
    public List<SysRole> createTenantRoles(SysTenant tenant) {
        List<SysRole> templateRoles = sysRoleService.list(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getType, OrgTypeEnums.Tenant));
        List<SysRoleGroup> templateRoleGroups = sysRoleGroupService.list(Wrappers.<SysRoleGroup>lambdaQuery()
                .eq(SysRoleGroup::getType, OrgTypeEnums.Tenant));

        return TenantEnv.applyAs(tenant.getId(), () -> {
            List<Long> templateGroupIds = templateRoleGroups.stream().map(SysRoleGroup::getId).toList();
            List<SysRoleGroup> orgRoleGroups = templateRoleGroups.stream()
                    .map(item -> {
                        SysRoleGroup group = new SysRoleGroup();
                        group.setName(item.getName());
                        group.setType(item.getType());
                        group.setSort(item.getSort());
                        return group;
                    }).toList();
            sysRoleGroupService.saveBatch(orgRoleGroups);

            List<SysRole> orgRoles = templateRoles.stream()
                    .map(item -> {
                        SysRole role = new SysRole();
                        role.setGroupId(orgRoleGroups.get(templateGroupIds.indexOf(item.getGroupId())).getId());
                        role.setName(item.getName());
                        role.setCode(item.getCode());
                        role.setType(item.getType());
                        role.setStatus(item.getStatus());
                        role.setCreatedAt(DateUtils.now());
                        return role;
                    }).toList();
            sysRoleService.saveBatch(orgRoles);
            return orgRoles;
        });
    }

    /**
     * 创建组合权限和菜单
     */
    public List<SysAuthority> createTenantAuthorityAndMenu(SysTenant tenant) {
        // 获取所有应用
        List<SysApplication> appList = sysApplicationService.list();
        if (CollUtil.isEmpty(appList)) {
            return ListUtil.empty();
        }

        return TenantEnv.applyAs(tenant.getId(), () ->
                appList.stream()
                        .flatMap(application -> createApp(tenant, application).stream())
                        .toList());
    }

    private List<SysAuthority> createApp(SysTenant org, SysApplication application) {
        LoadAppInfo loadAppInfo = TenantUtils.getLoadAppInfo(
                application, tenantProperties, menuTrans, authorityTrans, sysAuthorityService, sysMenuService);
        return TenantUtils.createAppAndReturnAuthority(
                org.getId(), application, loadAppInfo,
                menuTrans, sysAuthorityService, sysMenuService, sysApplicationTenantService);
    }

    /**
     * 创建租户管理员
     */
    public void createTenantUser(CreateOrgDTO params, SysTenant tenant, List<SysRole> roles, SysDept dept) {
        TenantEnv.runAs(tenant.getId(), () -> {
            SysRole role = roles.stream()
                    .filter(item -> StrUtil.equals(item.getCode(), RoleConstants.ROLE_MANAGER_CODE))
                    .findFirst().orElseThrow();

            // 如果已经存在注册用户，那么直接关联新组织信息
            SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPhone, params.getPhone()));
            if (user == null) {
                user = new SysUser();
                user.setInitPwd(Boolean.TRUE);
                user.setUsername(params.getPhone());
                user.setPhone(params.getPhone());
                user.setPassword(params.getPhone());
                user.setNickname(params.getNickname());
                sysUserService.createUser(user);
            }

            // 加入租户
            sysUserTenantService.joinTenant(user.getId(), tenant);
            // 设置部门
            sysDeptService.setDepts(user.getId(), List.of(dept.getId()));
            // 设置主角色
            sysRoleUserService.setUserRoles(user.getId(), List.of(role.getId()));
        });
    }

    /**
     * 租户默认角色关联权限
     */
    public void tenantRoleBindAuthorities(SysTenant tenant, List<SysRole> roles, List<SysAuthority> authorities) {
        TenantEnv.runAs(tenant.getId(), () -> {
            SysRole role = roles.stream()
                    .filter(item -> StrUtil.equals(item.getCode(), RoleConstants.ROLE_MANAGER_CODE))
                    .findFirst().orElseThrow();
            TenantUtils.bindAuthorities(tenant.getId(), role.getId(), authorities, sysRoleAuthorityService);
        });
    }

    /**
     * 销毁所有关联信息
     *
     * @param id 租户ID
     */
    public void destroy(long id) {
        TenantEnv.runAs(id, () -> {
            // 系统用户id
            List<Long> sysUserIdList = sysUserTenantService.list(
                            Wrappers.<SysUserTenant>lambdaQuery()
                                    .eq(SysUserTenant::getTenantId, id))
                    .stream().map(SysUserTenant::getUserId).toList();

            // 系统用户取消关联组织
            sysUserTenantService.remove(Wrappers.<SysUserTenant>lambdaQuery()
                    .eq(SysUserTenant::getTenantId, id));

            if (CollUtil.isNotEmpty(sysUserIdList)) {
                // 取消关联部门
                sysUserDeptService.remove(Wrappers.<SysUserDept>lambdaQuery()
                        .in(SysUserDept::getUserId, sysUserIdList));
                // 取消关联角色
                sysRoleUserService.remove(Wrappers.<SysRoleUser>lambdaQuery()
                        .in(SysRoleUser::getUserId, sysUserIdList));
            }


            // app用户ID
            List<Long> appUserIdList = appUserTenantService.list(
                            Wrappers.<AppUserTenant>lambdaQuery()
                                    .eq(AppUserTenant::getTenantId, id))
                    .stream().map(AppUserTenant::getUserId).toList();

            // app用户取消关联组织信息
            appUserTenantService.remove(Wrappers.<AppUserTenant>lambdaQuery()
                    .eq(AppUserTenant::getTenantId, id));

            if (CollUtil.isNotEmpty(appUserIdList)) {
                // 取消关联角色
                appRoleUserService.remove(Wrappers.<AppRoleUser>lambdaQuery()
                        .in(AppRoleUser::getUserId, appUserIdList));
            }

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

            // 移除角色相关
            List<Long> roleIds = sysRoleService.list(Wrappers.<SysRole>lambdaQuery()
                    .eq(SysRole::getTenantId, id)).stream().map(SysRole::getId).toList();

            sysRoleService.remove(Wrappers.<SysRole>lambdaQuery()
                    .eq(SysRole::getTenantId, id));

            sysRoleGroupService.remove(Wrappers.<SysRoleGroup>lambdaQuery()
                    .eq(SysRoleGroup::getTenantId, id));

            sysRoleAuthorityService.remove(Wrappers.<SysRoleAuthority>lambdaQuery()
                    .in(SysRoleAuthority::getRoleId, roleIds));

            appRoleService.remove(Wrappers.<AppRole>lambdaQuery()
                    .eq(AppRole::getTenantId, id));

        });
    }
}
