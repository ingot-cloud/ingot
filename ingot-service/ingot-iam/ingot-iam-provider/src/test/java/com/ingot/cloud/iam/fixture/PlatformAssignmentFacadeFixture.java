package com.ingot.cloud.iam.fixture;

import com.ingot.cloud.iam.application.authorization.candidate.PlatformAuthorizationCandidateQueryService;
import com.ingot.cloud.iam.application.authorization.scope.TenantScopeCandidates;

import com.ingot.cloud.iam.application.assignment.PlatformAssignmentFacade;

import com.ingot.cloud.iam.application.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.infrastructure.persistence.AssignmentRepository;
import com.ingot.cloud.iam.application.delegation.DelegationAdmission;
import com.ingot.cloud.iam.infrastructure.persistence.RoleRepository;
import com.ingot.cloud.iam.application.role.PlatformRoleFacade;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.application.audit.AuthorizationAuditWriter;
import org.springframework.transaction.PlatformTransactionManager;
import com.ingot.cloud.iam.application.assignment.*;

/**
 * <p>为原有assignment回归构造明确的应用服务图，不增加生产注入入口。</p>
 * @author jy
 * @since 1.0.0
 */
public final class PlatformAssignmentFacadeFixture {
    private PlatformAssignmentFacadeFixture() { }

    /** 使用既有测试依赖装配拆分后的用例。 */
    public static PlatformAssignmentFacade create(IamAccess access, AuthorizationAuditWriter audits, AuthorizationChangeNotifier changes,
                                 PlatformRoleFacade roles, RoleRepository roleStore, AssignmentRepository assignments,
                                 DelegationAdmission delegations,
                                 com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService resourceAccess, PlatformAuthorizationCandidateQueryService editor, com.ingot.cloud.iam.application.authorization.candidate.PlatformScopeBindingValidator bindingValidator,
                                 TenantScopeCandidates tenantScopeCandidates,
                                 PlatformTransactionManager transactionManager) {
        PlatformAssignmentOperations operations = new PlatformAssignmentOperations(access, audits, changes, roles, roleStore, assignments, delegations, resourceAccess, bindingValidator, editor, tenantScopeCandidates, transactionManager);
        PlatformAssignmentPreviewService platformAssignmentPreviewService = new PlatformAssignmentPreviewService(operations);
        PlatformAssignmentQueryService platformAssignmentQueryService = new PlatformAssignmentQueryService(operations);
        PlatformAssignmentVersionService platformAssignmentVersionService = new PlatformAssignmentVersionService(operations);
        PlatformAssignmentCommandService platformAssignmentCommandService = new PlatformAssignmentCommandService(operations, platformAssignmentPreviewService);
        return new PlatformAssignmentFacade(platformAssignmentCommandService, platformAssignmentPreviewService, platformAssignmentQueryService, platformAssignmentVersionService);
    }
}
