package com.ingot.cloud.pms.core;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.pms.api.model.domain.*;
import com.ingot.cloud.pms.service.domain.*;
import com.ingot.framework.core.model.common.RelationDTO;
import com.ingot.framework.tenant.TenantContextHolder;
import com.ingot.framework.tenant.TenantEnv;
import lombok.RequiredArgsConstructor;
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

    public void createRole(SysRole role) {
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            role.setId(null);
                            role.setTenantId(null);
                            sysRoleService.createRole(role, true);
                        }));
    }

    public void updateRole(SysRole role) {
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            SysRole orgRole = sysRoleService.getOne(Wrappers.<SysRole>lambdaQuery()
                                    .eq(SysRole::getCode, role.getCode()));

                            role.setId(orgRole.getId());
                            role.setTenantId(null);
                            sysRoleService.updateRoleById(role, true);
                        }));
    }

    public void removeRole(SysRole role) {
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            SysRole orgRole = sysRoleService.getOne(Wrappers.<SysRole>lambdaQuery()
                                    .eq(SysRole::getCode, role.getCode()));

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

    public void createRoleGroup(SysRoleGroup group) {
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            group.setId(null);
                            group.setTenantId(null);
                            sysRoleService.createGroup(group, true);
                        }));
    }

    public void updateRoleGroup(SysRoleGroup group) {
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            SysRoleGroup orgGroup = sysRoleGroupService.getOne(Wrappers.<SysRoleGroup>lambdaQuery()
                                    .eq(SysRoleGroup::getName, group.getName()));

                            group.setId(orgGroup.getId());
                            group.setTenantId(null);
                            sysRoleService.updateGroup(group, true);
                        }));
    }

    public void removeRoleGroup(SysRoleGroup group) {
        getOrgs().forEach(org ->
                TenantEnv.runAs(org.getId(),
                        () -> {
                            SysRoleGroup orgGroup = sysRoleGroupService.getOne(Wrappers.<SysRoleGroup>lambdaQuery()
                                    .eq(SysRoleGroup::getName, group.getName()));

                            sysRoleService.deleteGroup(orgGroup.getId(), true);
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

    private List<SysTenant> getOrgs() {
        long currentOrgId = TenantContextHolder.get();
        return CollUtil.emptyIfNull(sysTenantService.list())
                .stream()
                .filter(item -> item.getId() != currentOrgId)
                .toList();
    }
}
