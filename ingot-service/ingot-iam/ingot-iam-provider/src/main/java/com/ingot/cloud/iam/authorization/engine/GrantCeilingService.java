package com.ingot.cloud.iam.authorization.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import com.ingot.cloud.iam.api.model.domain.PlatformResource;
import com.ingot.cloud.iam.api.model.domain.PlatformRoleDataRule;
import com.ingot.cloud.iam.api.model.domain.TenantDept;
import com.ingot.cloud.iam.api.model.domain.TenantRoleDataRulePrivate;
import com.ingot.cloud.iam.service.domain.PlatformResourceService;
import com.ingot.cloud.iam.service.domain.PlatformRoleDataRuleService;
import com.ingot.cloud.iam.service.domain.TenantDeptService;
import com.ingot.cloud.iam.service.domain.TenantRoleDataRulePrivateService;
import com.ingot.cloud.iam.service.domain.TenantUserDeptPrivateService;
import com.ingot.framework.core.utils.validation.AssertionChecker;
import com.ingot.framework.data.mybatis.common.model.DataScopeTypeEnum;
import com.ingot.framework.security.core.context.SecurityAuthContext;
import com.ingot.framework.security.core.userdetails.InUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * <p>授权写入口的委派上限校验：拟授予功能与数据范围必须能证明为操作者自身范围的子集。</p>
 *
 * <p>平台超级管理员跳过；无登录上下文时跳过（系统引导）。无法证明则拒绝，叶子全集不能委派未来通配。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class GrantCeilingService {

    private final ObjectProvider<EffectiveAuthorizationService> effectiveAuthorizationService;
    private final TenantDeptService tenantDeptService;
    private final TenantUserDeptPrivateService tenantUserDeptPrivateService;
    private final PlatformRoleDataRuleService platformRoleDataRuleService;
    private final TenantRoleDataRulePrivateService tenantRoleDataRuleService;
    private final PlatformResourceService platformResourceService;
    private final AssertionChecker assertionChecker;

    /**
     * 校验拟授予功能编码不超过操作者自身范围。
     *
     * @param proposedCodes 变更后角色将持有的功能编码
     */
    public void assertCanGrantCodes(Collection<String> proposedCodes) {
        EffectiveAuthorization grantor = currentGrantor();
        if (grantor == null) {
            return;
        }
        for (String code : proposedCodes == null ? List.<String>of() : proposedCodes) {
            if (GrantCeiling.canGrantCode(grantor, code)) {
                continue;
            }
            assertionChecker.checkOperation(false,
                    GrantCeiling.deniedAsFutureWildcard(grantor, code)
                            ? "GrantCeilingService.WildcardNotDelegable"
                            : "GrantCeilingService.ExceedsGrantor");
        }
    }

    /**
     * 校验把角色绑定到指定用户/部门后，该角色数据范围仍不超过操作者。
     *
     * @param roleId       目标角色 ID
     * @param platformRole 是否平台预设角色
     * @param deptId       拟绑定部门，非部门角色可空
     * @param userIds      接收用户
     */
    public void assertCanBindRole(long roleId, boolean platformRole, Long deptId, Collection<Long> userIds) {
        EffectiveAuthorization grantor = currentGrantor();
        if (grantor == null) {
            return;
        }
        List<DataScopeGrant> rules = loadRoleRules(roleId, platformRole);
        assertDataRules(grantor, rules, deptId, userIds);
    }

    /**
     * 校验角色数据规则变更后的结果不超过操作者。
     *
     * @param proposed 变更后该来源层规则
     */
    public void assertCanReplaceDataRules(List<DataScopeGrant> proposed) {
        EffectiveAuthorization grantor = currentGrantor();
        if (grantor == null) {
            return;
        }
        assertDataRules(grantor, proposed, null, List.of());
    }

    private void assertDataRules(EffectiveAuthorization grantor,
                                 List<DataScopeGrant> proposed,
                                 Long bindDeptId,
                                 Collection<Long> recipientUserIds) {
        InUser user = SecurityAuthContext.getUser();
        long grantorUserId = user.getId();
        List<Long> bindDeptAndChild = bindDeptId == null
                ? List.of()
                : tenantDeptService.getDescendantList(bindDeptId, true).stream()
                .map(TenantDept::getId)
                .toList();
        for (DataScopeGrant rule : CollUtil.emptyIfNull(proposed)) {
            DataScopeCapability capability = capabilityOf(grantor, grantorUserId,
                    rule.getPermissionId(), rule.getResourceId());
            boolean covered = capability.covers(rule.getScopeType(), rule.safeDeptIds(),
                    bindDeptId, bindDeptAndChild, grantorUserId, recipientUserIds);
            if (covered) {
                continue;
            }
            assertionChecker.checkOperation(false,
                    rule.getScopeType() == DataScopeTypeEnum.SELF
                            ? "GrantCeilingService.SelfScopeNotTransferable"
                            : "GrantCeilingService.DataScopeExceedsGrantor");
        }
    }

    private DataScopeCapability capabilityOf(EffectiveAuthorization grantor,
                                             long grantorUserId,
                                             long permissionId,
                                             long resourceId) {
        DataScopeCapability capability = new DataScopeCapability();
        PlatformResource resource = platformResourceService.getById(resourceId);
        if (grantor.isOrgAdmin() && resource != null
                && grantor.safeAccessibleAppIds().contains(resource.getAppId())) {
            capability.mergeAll();
            return capability;
        }
        for (RoleBinding binding : grantor.safeRoleBindings()) {
            for (DataScopeGrant rule : loadRoleRules(binding.getRoleId(), binding.isPlatformRole())) {
                if (rule.getPermissionId() != permissionId || rule.getResourceId() != resourceId) {
                    continue;
                }
                merge(capability, rule, binding, grantorUserId);
            }
        }
        return capability;
    }

    private void merge(DataScopeCapability capability,
                       DataScopeGrant rule,
                       RoleBinding binding,
                       long grantorUserId) {
        if (rule.getScopeType() == null) {
            return;
        }
        switch (rule.getScopeType()) {
            case ALL -> capability.mergeAll();
            case SELF -> capability.mergeSelf();
            case DEPT -> capability.mergeDepts(resolveDeptRoots(binding, grantorUserId));
            case DEPT_AND_CHILD -> {
                for (Long root : resolveDeptRoots(binding, grantorUserId)) {
                    capability.mergeDepts(tenantDeptService.getDescendantList(root, true).stream()
                            .map(TenantDept::getId)
                            .toList());
                }
            }
            case CUSTOM -> capability.mergeDepts(rule.safeDeptIds());
        }
    }

    private List<Long> resolveDeptRoots(RoleBinding binding, long grantorUserId) {
        if (binding.getDeptId() != null) {
            return List.of(binding.getDeptId());
        }
        return CollUtil.emptyIfNull(tenantUserDeptPrivateService.getUserDepartmentIds(grantorUserId));
    }

    private List<DataScopeGrant> loadRoleRules(long roleId, boolean platformRole) {
        List<DataScopeGrant> rules = new ArrayList<>();
        if (platformRole) {
            for (PlatformRoleDataRule rule : platformRoleDataRuleService.listByRoleId(roleId)) {
                rules.add(toGrant(rule.getPermissionId(), rule.getResourceId(),
                        rule.getScopeType(), rule.getScopes()));
            }
        }
        for (TenantRoleDataRulePrivate rule : tenantRoleDataRuleService.listByRole(roleId, platformRole)) {
            rules.add(toGrant(rule.getPermissionId(), rule.getResourceId(),
                    rule.getScopeType(), rule.getScopes()));
        }
        return rules;
    }

    private static DataScopeGrant toGrant(Long permissionId,
                                          Long resourceId,
                                          DataScopeTypeEnum scopeType,
                                          List<Long> deptIds) {
        return DataScopeGrant.builder()
                .permissionId(permissionId == null ? 0L : permissionId)
                .resourceId(resourceId == null ? 0L : resourceId)
                .scopeType(scopeType)
                .deptIds(deptIds)
                .build();
    }

    private EffectiveAuthorization currentGrantor() {
        if (SecurityAuthContext.isAdmin()) {
            return null;
        }
        InUser user = SecurityAuthContext.getUser();
        if (user == null || user.getId() == null) {
            return null;
        }
        return effectiveAuthorizationService.getObject().resolve(user.getId());
    }
}
