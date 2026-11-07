package com.ingot.cloud.iam.fixture;

import com.ingot.cloud.iam.application.delegation.TenantDelegationFacade;

import com.ingot.cloud.iam.application.authorization.scope.TenantScopeBindingService;
import com.ingot.cloud.iam.application.role.TenantRoleLockService;
import com.ingot.cloud.iam.application.role.TenantRoleDefinitionResolver;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.shared.TenantAuthorizationTransaction;
import java.time.*;
import java.util.*;
import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.infrastructure.persistence.TenantRoleDefinitionRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper;
import com.ingot.framework.commons.model.iam.*;
import com.ingot.cloud.iam.application.delegation.*;

/**
 * <p>为原有delegation回归构造明确的应用服务图，不增加生产注入入口。</p>
 * @author jy
 * @since 1.0.0
 */
public final class TenantDelegationFacadeFixture {
    private TenantDelegationFacadeFixture() { }

    /** 使用既有测试依赖装配拆分后的用例。 */
    public static TenantDelegationFacade create(IamAccess access, TenantAdministrationMapper live, TenantRoleDefinitionRepository roles, TenantRoleDefinitionResolver roleDefinitions, TenantRoleLockService roleLocks, TenantScopeBindingService scopeBindings, ResourceScopeAccessService resources, TenantAuthorizationTransaction transactions, AuthorizationAuditWriter audits, AuthorizationChangeNotifier changes) {
        TenantDelegationOperations operations = new TenantDelegationOperations(access, live, roles, roleDefinitions, roleLocks, scopeBindings, resources, transactions, audits, changes);
        TenantDelegationCommandService tenantDelegationCommandService = new TenantDelegationCommandService(operations);
        TenantDelegationQueryService tenantDelegationQueryService = new TenantDelegationQueryService(operations);
        return new TenantDelegationFacade(tenantDelegationCommandService, tenantDelegationQueryService);
    }
}
