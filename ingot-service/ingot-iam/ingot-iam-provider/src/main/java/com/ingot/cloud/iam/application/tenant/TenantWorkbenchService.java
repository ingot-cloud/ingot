package com.ingot.cloud.iam.application.tenant;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.infrastructure.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.infrastructure.persistence.GroupRepository;
import com.ingot.cloud.iam.infrastructure.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.infrastructure.persistence.TenantRepository;
import com.ingot.cloud.iam.application.session.SessionService;
import com.ingot.cloud.iam.domain.shared.IamIds;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.cloud.iam.domain.catalog.MenuKind;
import com.ingot.cloud.iam.application.catalog.model.MenuNode;
import com.ingot.cloud.iam.application.tenant.model.TenantWorkbenchStatistics;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * <p>聚合当前租户的工作台统计，复用会话授权与列表范围，不向无读取权限的成员披露数量。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class TenantWorkbenchService {
    private final SessionService sessions;
    private final ResourceScopeAccessService scopes;
    private final TenantRepository tenants;
    private final MemberQueryRepository members;
    private final DepartmentQueryRepository departments;
    private final GroupRepository groups;

    /**
     * 读取当前组织最小资料与可见统计；依赖失败传播，无权限指标省略而非返回零。
     *
     * @return 绑定当前身份与各资源读取范围的汇总
     */
    public TenantWorkbenchStatistics statistics() {
        var bootstrap = sessions.bootstrap();
        var actor = bootstrap.context();
        Set<String> actions = Set.copyOf(bootstrap.actionCodes());
        if (actor.domain() != AuthorizationDomain.TENANT
                || !actions.contains(IamAction.VALUE_TENANT_WORKBENCH_READ)) {
            throw new BizException(IamReasonCode.ACTION_DENIED);
        }
        long tenantId = IamIds.require(actor.tenantId());
        var tenant = tenants.findActive(tenantId);
        if (tenant == null) {
            throw new BizException(IamReasonCode.IDENTITY_INVALID);
        }
        Long memberCount = actions.contains(IamAction.VALUE_TENANT_MEMBER_READ)
                ? members.countTenant(tenantId, scopes.memberRead(actor, IamAction.TENANT_MEMBER_READ)) : null;
        Long departmentCount = actions.contains(IamAction.VALUE_TENANT_DEPARTMENT_READ)
                ? departments.countDepartments(tenantId,
                        scopes.departmentRead(actor, IamAction.TENANT_DEPARTMENT_READ)) : null;
        Long groupCount = actions.contains(IamAction.VALUE_TENANT_GROUP_READ)
                ? groups.countTenant(tenantId, scopes.objects(actor, IamAction.TENANT_GROUP_READ)) : null;
        Set<String> navigableApplications = new HashSet<>();
        collectApplications(bootstrap.menus(), navigableApplications);
        long applicationCount = bootstrap.applications().stream().map(app -> app.id()).distinct()
                .filter(navigableApplications::contains).count();
        return new TenantWorkbenchStatistics(actor.tenantId(), tenant.getName(), tenant.getAvatar(),
                memberCount, departmentCount, groupCount, applicationCount);
    }

    private static void collectApplications(List<MenuNode> menus, Set<String> result) {
        for (MenuNode menu : menus) {
            if (menu.hidden()) {
                continue;
            }
            if (menu.kind() == MenuKind.PAGE && menu.path() != null && !menu.path().isBlank()) {
                result.add(menu.applicationId());
            }
            collectApplications(menu.children(), result);
        }
    }
}
