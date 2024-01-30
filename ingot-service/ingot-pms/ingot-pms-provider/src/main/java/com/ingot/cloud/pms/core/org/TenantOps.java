package com.ingot.cloud.pms.core.org;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.api.model.transform.AuthorityTrans;
import com.ingot.cloud.pms.api.model.transform.MenuTrans;
import com.ingot.cloud.pms.api.model.vo.authority.AuthorityTreeNodeVO;
import com.ingot.cloud.pms.api.model.vo.menu.MenuTreeNodeVO;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.constants.CacheConstants;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.core.model.enums.CommonStatusEnum;
import com.ingot.framework.core.utils.tree.TreeNode;
import com.ingot.framework.data.redis.utils.RedisUtils;
import com.ingot.framework.security.common.constants.RoleConstants;
import com.ingot.framework.tenant.TenantContextHolder;
import com.ingot.framework.tenant.TenantEnv;
import com.ingot.framework.tenant.properties.TenantProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * <p>Description  : TenantOps.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2023/11/22.</p>
 * <p>Time         : 11:49.</p>
 */
@Component
@RequiredArgsConstructor
public class TenantOps {
    private final SysTenantService sysTenantService;
    private final SysRoleService sysRoleService;
    private final SysRoleGroupService sysRoleGroupService;
    private final SysRoleAuthorityService sysRoleAuthorityService;
    private final SysRoleUserService sysRoleUserService;
    private final SysAuthorityService sysAuthorityService;
    private final SysApplicationTenantService sysApplicationTenantService;
    private final SysMenuService sysMenuService;
    private final AppRoleService appRoleService;
    private final AppRoleGroupService appRoleGroupService;
    private final AppRoleUserService appRoleUserService;

    private final TenantProperties tenantProperties;
    private final AuthorityTrans authorityTrans;
    private final MenuTrans menuTrans;
    private final RedisTemplate<String, Object> redisTemplate;

    public void createRole(SysRole role) {
        long modelId = role.getId();
        role.setModelId(modelId);
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            role.setId(null);
                            role.setTenantId(null);
                            sysRoleService.createRole(role, true);
                        }));
    }

    public void createRole(AppRole role) {
        long modelId = role.getId();
        role.setModelId(modelId);
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            role.setId(null);
                            role.setTenantId(null);
                            appRoleService.createRole(role, true);
                        }));
    }

    public void updateRole(SysRole role) {
        long modelId = role.getId();
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            SysRole orgRole = sysRoleService.getOne(Wrappers.<SysRole>lambdaQuery()
                                    .eq(SysRole::getModelId, modelId));

                            role.setId(orgRole.getId());
                            role.setTenantId(null);
                            sysRoleService.updateRoleById(role, true);
                        }));
    }

    public void updateRole(AppRole role) {
        long modelId = role.getId();
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            AppRole orgRole = appRoleService.getOne(Wrappers.<AppRole>lambdaQuery()
                                    .eq(AppRole::getModelId, modelId));

                            role.setId(orgRole.getId());
                            role.setTenantId(null);
                            appRoleService.updateRoleById(role, true);
                        }));
    }

    public void removeRole(SysRole role) {
        long modelId = role.getId();
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            SysRole orgRole = sysRoleService.getOne(Wrappers.<SysRole>lambdaQuery()
                                    .eq(SysRole::getModelId, modelId));

                            // 去掉关联权限
                            sysRoleAuthorityService.remove(
                                    Wrappers.<SysRoleAuthority>lambdaQuery()
                                            .eq(SysRoleAuthority::getRoleId, orgRole.getId()));

                            // 去掉关联用户
                            sysRoleUserService.remove(
                                    Wrappers.<SysRoleUser>lambdaQuery()
                                            .eq(SysRoleUser::getRoleId, orgRole.getId()));

                            sysRoleService.removeRoleById(orgRole.getId(), true);
                        }));
    }

    public void removeRole(AppRole role) {
        long modelId = role.getId();
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            AppRole orgRole = appRoleService.getOne(Wrappers.<AppRole>lambdaQuery()
                                    .eq(AppRole::getModelId, modelId));

                            // 去掉关联用户
                            appRoleUserService.remove(
                                    Wrappers.<AppRoleUser>lambdaQuery()
                                            .eq(AppRoleUser::getRoleId, orgRole.getId()));

                            appRoleService.removeRoleById(orgRole.getId(), true);
                        }));
    }

    public void createRoleGroup(SysRoleGroup group) {
        group.setModelId(group.getId());
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            group.setId(null);
                            group.setTenantId(null);
                            sysRoleService.createGroup(group, true);
                        }));
    }

    public void createRoleGroup(AppRoleGroup group) {
        group.setModelId(group.getId());
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            group.setId(null);
                            group.setTenantId(null);
                            appRoleService.createGroup(group, true);
                        }));
    }

    public void updateRoleGroup(SysRoleGroup group) {
        long modelId = group.getId();
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            SysRoleGroup orgGroup = sysRoleGroupService.getOne(Wrappers.<SysRoleGroup>lambdaQuery()
                                    .eq(SysRoleGroup::getModelId, modelId));

                            group.setId(orgGroup.getId());
                            group.setTenantId(null);
                            sysRoleService.updateGroup(group, true);
                        }));
    }

    public void updateRoleGroup(AppRoleGroup group) {
        long modelId = group.getId();
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            AppRoleGroup orgGroup = appRoleGroupService.getOne(Wrappers.<AppRoleGroup>lambdaQuery()
                                    .eq(AppRoleGroup::getModelId, modelId));

                            group.setId(orgGroup.getId());
                            group.setTenantId(null);
                            appRoleService.updateGroup(group, true);
                        }));
    }

    public void removeRoleGroup(SysRoleGroup group) {
        long modelId = group.getId();
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            SysRoleGroup orgGroup = sysRoleGroupService.getOne(Wrappers.<SysRoleGroup>lambdaQuery()
                                    .eq(SysRoleGroup::getModelId, modelId));

                            sysRoleService.deleteGroup(orgGroup.getId(), true);
                        }));
    }

    public void removeRoleGroup(AppRoleGroup group) {
        long modelId = group.getId();
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            AppRoleGroup orgGroup = appRoleGroupService.getOne(Wrappers.<AppRoleGroup>lambdaQuery()
                                    .eq(AppRoleGroup::getModelId, modelId));

                            appRoleService.deleteGroup(orgGroup.getId(), true);
                        }));
    }

    public void roleBindAuthorities(RelationDTO<Long, Long> params, SysRole role) {
        List<Long> bindIds = params.getBindIds();
        List<Long> removeIds = params.getRemoveIds();

        List<String> bindCodes = CollUtil.isEmpty(bindIds) ? null : sysAuthorityService.list(
                        Wrappers.<SysAuthority>lambdaQuery()
                                .in(SysAuthority::getId, bindIds))
                .stream().map(SysAuthority::getCode).toList();
        List<String> removeCodes = CollUtil.isEmpty(removeIds) ? null : sysAuthorityService.list(
                        Wrappers.<SysAuthority>lambdaQuery()
                                .in(SysAuthority::getId, removeIds))
                .stream().map(SysAuthority::getCode).toList();

        RelationDTO<Long, Long> orgRelation = new RelationDTO<>();
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            SysRole orgRole = sysRoleService.getOne(Wrappers.<SysRole>lambdaQuery()
                                    .eq(SysRole::getCode, role.getCode()));

                            orgRelation.setId(orgRole.getId());
                            if (CollUtil.isNotEmpty(bindCodes)) {
                                orgRelation.setBindIds(sysAuthorityService.list(
                                                Wrappers.<SysAuthority>lambdaQuery()
                                                        .in(SysAuthority::getCode, bindCodes))
                                        .stream().map(SysAuthority::getId).toList());
                            }
                            if (CollUtil.isNotEmpty(removeCodes)) {
                                orgRelation.setRemoveIds(sysAuthorityService.list(
                                                Wrappers.<SysAuthority>lambdaQuery()
                                                        .in(SysAuthority::getCode, removeCodes))
                                        .stream().map(SysAuthority::getId).toList());
                            }

                            sysRoleAuthorityService.roleBindAuthorities(orgRelation);
                        }));
    }

    /**
     * 创建应用<br>
     * 1.查询应用绑定的菜单和权限<br>
     * 2.给每个组织创建相应权限和菜单<br>
     * 3.给管理员同步权限
     * <p>
     * 创建应用时需要进行 Cache Evict {@link CacheConstants#AUTHORITY_DETAILS} 和 {@link CacheConstants#MENU_DETAILS}
     *
     * @param application {@link SysApplication}
     */
    public void createApplication(SysApplication application) {
        LoadAppInfo loadAppInfo = TenantUtils.getLoadAppInfo(
                application, tenantProperties, menuTrans, authorityTrans, sysAuthorityService, sysMenuService);

        getOrgs().forEach(org -> TenantEnv.runAs(org.getId(), () -> {
            List<SysAuthority> authorityCollect = TenantUtils.createAppAndReturnAuthority(
                    org.getId(), application, loadAppInfo,
                    menuTrans, sysAuthorityService, sysMenuService, sysApplicationTenantService);

            // 管理员绑定第一个权限
            SysRole role = sysRoleService.getOne(Wrappers.<SysRole>lambdaQuery()
                    .eq(SysRole::getCode, RoleConstants.ROLE_MANAGER_CODE));
            TenantUtils.bindAuthorities(org.getId(), role.getId(), authorityCollect, sysRoleAuthorityService);

            // 删除缓存keys
            RedisUtils.deleteKeys(redisTemplate,
                    ListUtil.list(false,
                            CacheConstants.AUTHORITY_DETAILS + "*",
                            CacheConstants.MENU_DETAILS + "*"
                    ));
        }));
    }

    /**
     * 更新应用状态
     *
     * @param application 当前应用
     * @param status      {@link CommonStatusEnum} 需要更新的状态
     */
    public void updateApplication(SysApplication application, CommonStatusEnum status) {
        TenantEnv.globalRun(() -> {
            SysApplicationTenant params = new SysApplicationTenant();
            params.setStatus(status);
            sysApplicationTenantService.update(params,
                    Wrappers.<SysApplicationTenant>lambdaUpdate()
                            .eq(SysApplicationTenant::getAppId, application.getId()));
        });
    }

    /**
     * 移除应用<br>
     * 1. 删除所有组织相关菜单<br>
     * 2. 删除所有组织相关权限<br>
     * 3. 取消管理员关联的该应用权限<br>
     * 3. 删除应用关联信息
     * <p>
     * 创建应用时需要进行 Cache Evict {@link CacheConstants#AUTHORITY_DETAILS} 和 {@link CacheConstants#MENU_DETAILS}
     *
     * @param application 应用
     */
    public void removeApplication(SysApplication application) {
        getOrgs().forEach(org -> TenantEnv.runAs(org.getId(), () -> {
            SysApplicationTenant applicationTenant = sysApplicationTenantService.getOne(
                    Wrappers.<SysApplicationTenant>lambdaQuery().eq(SysApplicationTenant::getAppId, application.getId()));

            long rootMenu = applicationTenant.getMenuId();
            long rootAuthority = applicationTenant.getAuthorityId();
            List<MenuTreeNodeVO> menuList = TenantUtils.getTargetMenus(
                    org.getId(), rootMenu, sysMenuService, menuTrans);
            List<AuthorityTreeNodeVO> authorityList = TenantUtils.getTargetAuthorities(
                    org.getId(), rootAuthority, sysAuthorityService, authorityTrans);

            // 删除菜单
            sysMenuService.remove(Wrappers.<SysMenu>lambdaQuery()
                    .in(SysMenu::getId, menuList.stream().map(TreeNode::getId).toList()));

            // 删除权限
            sysAuthorityService.remove(Wrappers.<SysAuthority>lambdaQuery()
                    .in(SysAuthority::getId, authorityList.stream().map(TreeNode::getId).toList()));

            List<Long> roleIds = CollUtil.emptyIfNull(sysRoleService.list()).stream().map(SysRole::getId).toList();
            TenantUtils.unbindAuthorities(org.getId(), roleIds, authorityList, sysRoleAuthorityService);

            // 删除app
            sysApplicationTenantService.removeById(applicationTenant.getId());

            // 删除缓存keys
            RedisUtils.deleteKeys(redisTemplate,
                    ListUtil.list(false,
                            CacheConstants.AUTHORITY_DETAILS + "*",
                            CacheConstants.MENU_DETAILS + "*"
                    ));
        }));
    }

    private List<SysTenant> getOrgs() {
        long currentOrgId = TenantContextHolder.get();
        return CollUtil.emptyIfNull(sysTenantService.list())
                .stream()
                .filter(item -> item.getId() != currentOrgId)
                .toList();
    }
}
