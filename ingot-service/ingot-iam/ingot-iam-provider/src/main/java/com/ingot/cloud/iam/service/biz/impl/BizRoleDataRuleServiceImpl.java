package com.ingot.cloud.iam.service.biz.impl;

import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ingot.cloud.iam.api.model.domain.PlatformPermission;
import com.ingot.cloud.iam.api.model.domain.PlatformResource;
import com.ingot.cloud.iam.api.model.domain.PlatformRoleDataRule;
import com.ingot.cloud.iam.api.model.domain.TenantDept;
import com.ingot.cloud.iam.api.model.domain.TenantRoleDataRulePrivate;
import com.ingot.cloud.iam.api.model.dto.role.RoleDataRuleItemDTO;
import com.ingot.cloud.iam.api.model.dto.role.RoleDataRuleSetDTO;
import com.ingot.cloud.iam.authorization.engine.DataScopeGrant;
import com.ingot.cloud.iam.authorization.engine.GrantCeilingService;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.service.biz.BizRoleDataRuleService;
import com.ingot.cloud.iam.service.domain.PlatformPermissionService;
import com.ingot.cloud.iam.service.domain.PlatformResourceService;
import com.ingot.cloud.iam.service.domain.PlatformRoleDataRuleService;
import com.ingot.cloud.iam.service.domain.TenantDeptService;
import com.ingot.cloud.iam.service.domain.TenantRoleDataRulePrivateService;
import com.ingot.cloud.iam.service.domain.TenantRolePrivateService;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>{@link BizRoleDataRuleService} 默认实现，校验权限、资源归属后再整体替换来源层。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class BizRoleDataRuleServiceImpl implements BizRoleDataRuleService {
    private final PlatformRoleDataRuleService platformRoleDataRuleService;
    private final TenantRoleDataRulePrivateService tenantRoleDataRuleService;
    private final PlatformPermissionService permissionService;
    private final PlatformResourceService resourceService;
    private final TenantDeptService tenantDeptService;
    private final TenantRolePrivateService tenantRolePrivateService;
    private final GrantCeilingService grantCeilingService;
    private final AuthorizationChangeNotifier authorizationChangeNotifier;
    private final AssertionChecker assertionChecker;

    @Override
    public List<PlatformRoleDataRule> listPlatform(long roleId) {
        return platformRoleDataRuleService.listByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replacePlatform(long roleId, RoleDataRuleSetDTO dto) {
        assertionChecker.checkOperation(dto != null, "ApplicationResourceServiceImpl.ParamNonNull");
        grantCeilingService.assertCanReplaceDataRules(toGrants(dto));
        List<PlatformRoleDataRule> rules = CollUtil.emptyIfNull(dto.getItems()).stream()
                .map(item -> toPlatformRule(roleId, item))
                .toList();
        platformRoleDataRuleService.replace(roleId, rules);
        authorizationChangeNotifier.markAll();
    }

    @Override
    public List<TenantRoleDataRulePrivate> listTenant(long roleId) {
        return tenantRoleDataRuleService.listByRole(roleId, isPlatformRole(roleId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceTenant(long roleId, RoleDataRuleSetDTO dto) {
        assertionChecker.checkOperation(dto != null, "ApplicationResourceServiceImpl.ParamNonNull");
        grantCeilingService.assertCanReplaceDataRules(toGrants(dto));
        boolean platformRole = isPlatformRole(roleId);
        List<TenantRoleDataRulePrivate> rules = CollUtil.emptyIfNull(dto.getItems()).stream()
                .map(item -> toTenantRule(roleId, platformRole, item))
                .toList();
        tenantRoleDataRuleService.replace(roleId, platformRole, rules);
        authorizationChangeNotifier.markAll();
    }

    private boolean isPlatformRole(long roleId) {
        return tenantRolePrivateService.getById(roleId) == null;
    }

    private PlatformRoleDataRule toPlatformRule(long roleId, RoleDataRuleItemDTO item) {
        validateItem(item);
        assertionChecker.checkOperation(item.getScopeType() != DataScopeTypeEnum.CUSTOM
                        || CollUtil.isEmpty(item.getScopes()),
                "RoleDataRuleService.PlatformCustomTenantDeptForbidden");
        PlatformRoleDataRule rule = new PlatformRoleDataRule();
        rule.setRoleId(roleId);
        rule.setPermissionId(item.getPermissionId());
        rule.setResourceId(item.getResourceId());
        rule.setScopeType(item.getScopeType());
        rule.setScopes(CollUtil.emptyIfNull(item.getScopes()));
        return rule;
    }

    private TenantRoleDataRulePrivate toTenantRule(long roleId, boolean platformRole, RoleDataRuleItemDTO item) {
        validateItem(item);
        if (item.getScopeType() == DataScopeTypeEnum.CUSTOM) {
            for (Long deptId : CollUtil.emptyIfNull(item.getScopes())) {
                TenantDept dept = tenantDeptService.getById(deptId);
                assertionChecker.checkOperation(dept != null, "BizDeptServiceImpl.DeptNotExist");
            }
        }
        TenantRoleDataRulePrivate rule = new TenantRoleDataRulePrivate();
        rule.setRoleId(roleId);
        rule.setPlatformRole(platformRole);
        rule.setPermissionId(item.getPermissionId());
        rule.setResourceId(item.getResourceId());
        rule.setScopeType(item.getScopeType());
        rule.setScopes(CollUtil.emptyIfNull(item.getScopes()));
        return rule;
    }

    private void validateItem(RoleDataRuleItemDTO item) {
        assertionChecker.checkOperation(item.getPermissionId() != null, "ApplicationResourceServiceImpl.ParamNonNull");
        assertionChecker.checkOperation(item.getResourceId() != null, "ApplicationResourceServiceImpl.ParamNonNull");
        assertionChecker.checkOperation(item.getScopeType() != null, "ApplicationResourceServiceImpl.ParamNonNull");
        PlatformPermission permission = permissionService.getById(item.getPermissionId());
        assertionChecker.checkOperation(permission != null, "BizPlatformAuthorityServiceImpl.NotExist");
        PlatformResource resource = resourceService.getById(item.getResourceId());
        assertionChecker.checkOperation(resource != null, "ApplicationResourceServiceImpl.ResourceNonExist");
        assertionChecker.checkOperation(permission.getResourceId() == null
                        || permission.getResourceId().equals(item.getResourceId()),
                "RoleDataRuleService.PermissionResourceMismatch");
        assertionChecker.checkOperation(resourceService.count(Wrappers.<PlatformResource>lambdaQuery()
                        .eq(PlatformResource::getId, item.getResourceId())
                        .eq(PlatformResource::getAppId, permission.getAppId())) == 1,
                "ApplicationResourceServiceImpl.ResourceAppMismatch");
    }

    private List<DataScopeGrant> toGrants(RoleDataRuleSetDTO dto) {
        return CollUtil.emptyIfNull(dto.getItems()).stream()
                .map(item -> DataScopeGrant.builder()
                        .permissionId(item.getPermissionId() == null ? 0L : item.getPermissionId())
                        .resourceId(item.getResourceId() == null ? 0L : item.getResourceId())
                        .scopeType(item.getScopeType())
                        .deptIds(item.getScopes())
                        .build())
                .toList();
    }
}
