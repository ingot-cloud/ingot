package com.ingot.cloud.iam.fixture;

import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.application.shared.TenantAuthorizationTransaction;
import com.ingot.cloud.iam.infrastructure.persistence.DepartmentClosure;
import com.ingot.cloud.iam.infrastructure.persistence.TenantRoleDefinitionRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantRoleWorkspaceMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantScopeCandidateMapper;
import java.time.*;
import java.util.*;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.application.role.TenantRoleDefinitionService;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.cloud.iam.application.assignment.TenantAssignmentFacade;
import com.ingot.cloud.iam.application.role.TenantRoleDefinitionResolver;
import com.ingot.cloud.iam.application.role.TenantRoleLockService;
import com.ingot.cloud.iam.application.authorization.scope.TenantScopeBindingService;

/**
 * <p>使用现有数据库夹具装配租户分配与共享规则，不增加生产构造入口。</p>
 * @author jy
 * @since 1.0.0
 */
public final class TenantAssignmentFixture {
    private TenantAssignmentFixture() { }
    /** 装配与原测试相同的必需依赖。 */
    public static TenantAssignmentFacade create(IamAccess access, TenantAdministrationMapper live, TenantRoleWorkspaceMapper workspace, TenantRoleDefinitionRepository roles, TenantRoleDefinitionService roleService, ResourceScopeAccessService resources, TenantAuthorizationTransaction transactions, AuthorizationAuditWriter audits, AuthorizationChangeNotifier changes, TenantScopeCandidateMapper candidates, DepartmentClosure departments, com.ingot.cloud.iam.application.organization.OrganizationMemberDepartments memberDepartments) {
        TenantRoleDefinitionResolver roleDefinitions = new TenantRoleDefinitionResolver(live, roles);
        TenantRoleLockService roleLocks = new TenantRoleLockService(live);
        TenantScopeBindingService scopeBindings = new TenantScopeBindingService(live, candidates, departments);
        return com.ingot.cloud.iam.fixture.TenantAssignmentFacadeFixture.create(roleDefinitions, roleLocks, scopeBindings, access, live, workspace, roles, roleService, resources, transactions, audits, changes, memberDepartments);
    }
}
