package com.ingot.cloud.iam.fixture;

import com.ingot.cloud.iam.application.assignment.TenantAssignmentFacade;

import com.ingot.cloud.iam.application.authorization.scope.TenantScopeBindingService;
import com.ingot.cloud.iam.application.role.TenantRoleLockService;
import com.ingot.cloud.iam.application.role.TenantRoleDefinitionResolver;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.application.shared.TenantAuthorizationTransaction;
import com.ingot.cloud.iam.infrastructure.persistence.TenantRoleDefinitionRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantRoleWorkspaceMapper;
import java.time.*;
import java.util.*;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.application.role.TenantRoleDefinitionService;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.cloud.iam.application.assignment.*;

/**
 * <p>为原有assignment回归构造明确的应用服务图，不增加生产注入入口。</p>
 * @author jy
 * @since 1.0.0
 */
public final class TenantAssignmentFacadeFixture {
    private TenantAssignmentFacadeFixture() { }

    /** 使用既有测试依赖装配拆分后的用例。 */
    public static TenantAssignmentFacade create(TenantRoleDefinitionResolver roleDefinitions, TenantRoleLockService roleLocks, TenantScopeBindingService scopeBindings, IamAccess access, TenantAdministrationMapper live, TenantRoleWorkspaceMapper workspace, TenantRoleDefinitionRepository roles, TenantRoleDefinitionService roleService, ResourceScopeAccessService resources, TenantAuthorizationTransaction transactions, AuthorizationAuditWriter audits, AuthorizationChangeNotifier changes, com.ingot.cloud.iam.application.organization.OrganizationMemberDepartments memberDepartments) {
        TenantAssignmentOperations operations = new TenantAssignmentOperations(roleDefinitions, roleLocks, scopeBindings, access, live, workspace, roles, roleService, resources, transactions, audits, changes, memberDepartments);
        TenantAssignmentCommandService tenantAssignmentCommandService = new TenantAssignmentCommandService(operations);
        TenantAssignmentQueryService tenantAssignmentQueryService = new TenantAssignmentQueryService(operations);
        return new TenantAssignmentFacade(tenantAssignmentCommandService, tenantAssignmentQueryService);
    }
}
