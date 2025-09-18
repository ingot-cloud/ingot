package com.ingot.cloud.pms.core.org;

import java.util.*;

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
import com.ingot.cloud.pms.api.model.vo.dept.DeptTreeNodeVO;
import com.ingot.cloud.pms.core.BizIdGen;
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
    private final SysApplicationService sysApplicationService;
    private final SysApplicationTenantService sysApplicationTenantService;

    private final BizIdGen bizIdGen;
    private final TenantProperties tenantProperties;
    private final AuthorityConvert authorityConvert;
    private final MenuConvert menuConvert;
    private final DeptConvert deptConvert;
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
        sysTenantService.createTenant(tenant);
        return tenant;
    }

    /**
     * 创建租户部门<br>
     * 1. 创建以租户名称命名的部门<br>
     * 2. 创建默认子部门(系统默认)<br>
     */
    public SysDept createTenantDept(SysTenant tenant) {
        // 获取所有预设部门
        List<DeptTreeNodeVO> templateTree = CollUtil.emptyIfNull(sysDeptService.treeList());
        DeptTreeNodeVO templateMainDept = templateTree.stream()
                .filter(item -> Objects.equals(item.getMainFlag(), Boolean.TRUE))
                .findFirst()
                .orElse(null);
        // 获取主部门下面所有模本部门信息
        List<DeptTreeNodeVO> templateList = templateMainDept == null ? ListUtil.empty()
                : CollUtil.emptyIfNull(templateMainDept.getChildren())
                .stream().map(item -> (DeptTreeNodeVO) item).toList();

        return TenantEnv.applyAs(tenant.getId(), () -> {
            // 1. 创建主部门(当前组织名称的部门)
            SysDept mainDept = new SysDept();
            mainDept.setName(tenant.getName());
            mainDept.setMainFlag(Boolean.TRUE);
            sysDeptService.createDept(mainDept);

            // 2. 创建默认子部门
            if (templateMainDept == null) {
                return mainDept;
            }

            List<SysDept> collect = new ArrayList<>();
            TenantUtils.createDeptFn(collect, templateList, mainDept.getId(), deptConvert, sysDeptService);
            return mainDept;
        });
    }

    /**
     * 创建租户角色
     */
    public List<SysRole> createTenantRoles(SysTenant tenant) {
        List<SysRole> templateRoles = sysRoleService.list(Wrappers.<SysRole>lambdaQuery()
                .eq(SysRole::getType, OrgTypeEnum.Tenant));
        List<SysRoleGroup> templateRoleGroups = sysRoleGroupService.list(Wrappers.<SysRoleGroup>lambdaQuery()
                .eq(SysRoleGroup::getType, OrgTypeEnum.Tenant));

        return TenantEnv.applyAs(tenant.getId(), () -> {
            List<Long> templateGroupIds = templateRoleGroups.stream().map(SysRoleGroup::getId).toList();
            List<SysRoleGroup> orgRoleGroups = templateRoleGroups.stream()
                    .map(item -> {
                        SysRoleGroup group = new SysRoleGroup();
                        BeanUtil.copyProperties(item, group);
                        group.setId(null);
                        group.setTenantId(null);
                        group.setModelId(item.getId());
                        return group;
                    }).toList();
            sysRoleGroupService.saveBatch(orgRoleGroups);

            List<SysRole> orgRoles = templateRoles.stream()
                    .map(item -> {
                        SysRole role = new SysRole();
                        BeanUtil.copyProperties(item, role);
                        role.setId(null);
                        role.setTenantId(null);
                        role.setModelId(item.getId());
                        role.setGroupId(orgRoleGroups.get(templateGroupIds.indexOf(item.getGroupId())).getId());
                        role.setCreatedAt(DateUtil.now());
                        role.setUpdatedAt(role.getCreatedAt());
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
                application, tenantProperties, menuConvert, authorityConvert, sysAuthorityService, sysMenuService);
        return TenantUtils.createAppAndReturnAuthority(
                org.getId(), application, loadAppInfo,
                menuConvert, sysAuthorityService, sysMenuService, sysApplicationTenantService);
    }

    /**
     * 创建租户管理员
     */
    public void createTenantUser(CreateOrgDTO params, SysTenant tenant, List<SysRole> roles, SysDept dept) {
        TenantEnv.runAs(tenant.getId(), () -> {
            SysRole role = roles.stream()
                    .filter(item -> StrUtil.equals(item.getCode(), RoleConstants.ROLE_ORG_ADMIN_CODE))
                    .findFirst().orElseThrow();

            // 如果已经存在注册用户，那么直接关联新组织信息
            SysUser user = sysUserService.getOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getPhone, params.getPhone()));
            if (user == null) {
                user = new SysUser();
                user.setInitPwd(Boolean.TRUE);
                user.setUsername(params.getPhone());
                user.setPhone(params.getPhone());
                user.setPassword(params.getPhone());
                user.setNickname(params.getPhone());
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
     * 租户默认角色关联权限<br>
     * 1. 管理员关联所有权限
     * 2. 给所有非管理员角色的组织类型角色绑定默认权限
     */
    public void tenantRoleBindAuthorities(SysTenant tenant, List<SysRole> roles, List<SysAuthority> authorities) {
        // 组织角色模版ID, 不包括管理员角色(RoleConstants.ROLE_ORG_ADMIN_CODE)
        List<Long> modelRoles = roles.stream()
                .filter(role -> role.getType() == OrgTypeEnum.Tenant)
                .filter(role -> !StrUtil.equals(role.getCode(), RoleConstants.ROLE_ORG_ADMIN_CODE))
                .map(SysRole::getModelId)
                .toList();
        // 角色模版ID->权限编码 映射关系
        Map<Long, List<String>> roleAuthorityCodes = new HashMap<>();
        for (Long roleId : modelRoles) {
            List<String> codes = CollUtil.emptyIfNull(sysRoleAuthorityService.getAuthoritiesByRole(roleId))
                    .stream().map(SysAuthority::getCode)
                    .toList();
            if (CollUtil.isNotEmpty(codes)) {
                roleAuthorityCodes.put(roleId, codes);
            }
        }

        Long orgId = tenant.getId();
        TenantEnv.runAs(orgId, () -> {
            // 管理员关联所有权限
            SysRole role = roles.stream()
                    .filter(item -> StrUtil.equals(item.getCode(), RoleConstants.ROLE_ORG_ADMIN_CODE))
                    .findFirst().orElseThrow();
            TenantUtils.bindAuthorities(orgId, role.getId(), authorities, sysRoleAuthorityService);

            // 非管理员组织角色，关联默认权限
            roles.stream()
                    .filter(item -> roleAuthorityCodes.containsKey(item.getModelId()))
                    .forEach(item -> {
                        List<String> codes = roleAuthorityCodes.get(item.getModelId());
                        List<SysAuthority> targetAuthorities = authorities.stream()
                                .filter(authority -> codes.contains(authority.getCode()))
                                .toList();
                        TenantUtils.bindAuthorities(orgId, item.getId(), targetAuthorities, sysRoleAuthorityService);
                    });

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
