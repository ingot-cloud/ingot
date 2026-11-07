package com.ingot.cloud.iam.application.roleapplication;

import com.ingot.cloud.iam.application.role.PlatformRoleFacade;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;

import java.math.BigInteger;

import com.ingot.cloud.iam.application.assignment.PlatformAssignmentFacade;
import com.ingot.cloud.iam.application.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.application.authorization.evaluation.ResourceScopeAccessService;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.application.identity.ActiveIdentityService;
import com.ingot.cloud.iam.infrastructure.persistence.AssignmentRepository;
import com.ingot.cloud.iam.infrastructure.persistence.RoleRepository;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleVersionApplicationTaskEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleDefinitionEntity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.domain.shared.AuthorizationAdmission;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证平台适配边界的身份、固定版本、fresh范围与锁顺序，不增加身份或角色查询。</p>
 * @author jy
 * @since 1.0.0
 */
class PlatformRoleVersionApplicationAdapterTest {
    private final IamAccess access = mock(IamAccess.class);
    private final PlatformRoleFacade roles = mock(PlatformRoleFacade.class);
    private final RoleRepository roleStore = mock(RoleRepository.class);
    private final AssignmentRepository assignmentStore = mock(AssignmentRepository.class);
    private final PlatformAssignmentFacade assignments = mock(PlatformAssignmentFacade.class);
    private final ResourceScopeAccessService resources = mock(ResourceScopeAccessService.class);
    private final ActiveIdentityService identities = mock(ActiveIdentityService.class);
    private final IamActionAuthorizer authorizer = mock(IamActionAuthorizer.class);
    private final PlatformRoleVersionApplicationAdapter support = new PlatformRoleVersionApplicationAdapter(
            access, roles, roleStore, assignmentStore, assignments, resources, identities, authorizer);
    private final AuthorizationContext context = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "2");
    private final ActiveIdentity actor = new ActiveIdentity(context, "1", "1", null);
    private final AuthorizationAdmission admission = new AuthorizationAdmission(actor, true);

    @Test
    void rejectsDelegatedAndForeignDomainAdmissionsBeforeBusinessReads() {
        when(access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_UPGRADE))
                .thenReturn(new AuthorizationAdmission(actor, false));
        denied(IamReasonCode.ACTION_DENIED, support::admit);
        var tenant = new AuthorizationAdmission(new ActiveIdentity(new AuthorizationContext(
                AuthorizationDomain.TENANT, "3", "1", "2"), "1", "1", "1"), true);
        when(access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_UPGRADE)).thenReturn(tenant);
        denied(IamReasonCode.ACTION_DENIED, support::admit);
        denied(IamReasonCode.ACTION_DENIED, () -> support.requireRole("10", tenant, true));
        denied(IamReasonCode.ACTION_DENIED, () -> support.scope(tenant, true));
        verifyNoInteractions(roleStore, assignmentStore, assignments, resources, identities, authorizer);
    }

    @Test
    void restoresSavedIdentityAndChecksCurrentGovernanceWithoutHttpAdmission() {
        var task = new IamRoleVersionApplicationTaskEntity();
        task.setActorAccountId(BigInteger.ONE); task.setActorMemberId(BigInteger.TWO);
        when(identities.requireActive(context)).thenReturn(actor);
        when(authorizer.admit(context, IamAction.PLATFORM_ASSIGNMENT_UPGRADE))
                .thenReturn(new IamActionAuthorizer.Admission(true), new IamActionAuthorizer.Admission(false));
        assertEquals(admission, support.restore(task));
        denied(IamReasonCode.ACTION_DENIED, () -> support.restore(task));
        verify(identities, times(2)).requireActive(context);
        verify(authorizer, times(2)).admit(context, IamAction.PLATFORM_ASSIGNMENT_UPGRADE);
        verifyNoInteractions(access, roleStore, assignmentStore, assignments, resources);
    }

    @Test
    void unavailableIdentityStopsBeforeAuthorizationOrBusinessAccess() {
        var task = new IamRoleVersionApplicationTaskEntity();
        task.setActorAccountId(BigInteger.ONE); task.setActorMemberId(BigInteger.TWO);
        when(identities.requireActive(context)).thenThrow(new BizException(IamReasonCode.ACTION_DENIED));
        denied(IamReasonCode.ACTION_DENIED, () -> support.restore(task));
        verifyNoInteractions(access, authorizer, roleStore, assignmentStore, assignments, resources);
    }

    @Test
    void preservesFreshScopeAndRoleReadBoundary() {
        when(resources.objects(context, IamAction.PLATFORM_ASSIGNMENT_UPGRADE)).thenReturn(ObjectScope.none());
        when(resources.objectsForExecution(context, IamAction.PLATFORM_ASSIGNMENT_UPGRADE)).thenReturn(ObjectScope.all());
        assertFalse(support.scope(admission, false).coversAll());
        assertTrue(support.scope(admission, true).coversAll());
        var role = new IamRoleDefinitionEntity(); role.setKind(RoleKind.PLATFORM_CUSTOM);
        when(roleStore.findDefinition(AuthorizationDomain.PLATFORM, false, null, 10L)).thenReturn(role);
        when(resources.objectsForExecution(context, IamAction.PLATFORM_ROLE_READ)).thenReturn(ObjectScope.none());
        denied(IamReasonCode.OBJECT_NOT_FOUND, () -> support.requireRole("10", admission, true));
        when(resources.objects(context, IamAction.PLATFORM_ROLE_READ)).thenReturn(ObjectScope.all());
        support.requireRole("10", admission, false);
        role.setKind(RoleKind.SYSTEM);
        denied(IamReasonCode.OBJECT_NOT_FOUND, () -> support.requireRole("10", admission, true));
        verify(roleStore, times(3)).findDefinition(AuthorizationDomain.PLATFORM, false, null, 10L);
        verifyNoInteractions(identities, authorizer);
    }

    @Test
    void rejectsForeignRoleOrUnsupportedRevisionWithOneExistingRead() {
        var revision = new IamRoleRevisionEntity();
        revision.setId(BigInteger.valueOf(20)); revision.setRoleId(BigInteger.valueOf(10));
        revision.setKind(RoleKind.PLATFORM_CUSTOM);
        when(roleStore.findRevision(20L)).thenReturn(revision);
        assertSame(revision, support.requireTarget("10", "20"));
        denied(IamReasonCode.ROLE_REVISION_UNAVAILABLE, () -> support.requireTarget("11", "20"));
        for (var kind : new RoleKind[]{RoleKind.SYSTEM, RoleKind.SHARED, RoleKind.TENANT_CUSTOM}) {
            revision.setKind(kind);
            denied(IamReasonCode.ROLE_REVISION_UNAVAILABLE, () -> support.requireTarget("10", "20"));
        }
        verify(roleStore, times(5)).findRevision(20L);
        verifyNoMoreInteractions(roleStore);
        verifyNoInteractions(identities, resources, assignments, assignmentStore);
    }

    @Test
    void locksAuthorizationBeforeRoleAndBuildsReferenceWithoutQueries() {
        support.lockRole(10L);
        var order = inOrder(assignmentStore, roleStore);
        order.verify(assignmentStore).lockAuthorization(AuthorizationDomain.PLATFORM);
        order.verify(roleStore).lockDefinition(AuthorizationDomain.PLATFORM, false, null, 10L);
        assertEquals(new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM, "20"), support.reference(BigInteger.valueOf(20)));
        verifyNoMoreInteractions(assignmentStore, roleStore);
        verifyNoInteractions(access, roles, assignments, resources, identities, authorizer);
    }

    private static void denied(IamReasonCode reason, org.junit.jupiter.api.function.Executable action) {
        assertEquals(reason.getCode(), assertThrows(BizException.class, action).getCode());
    }
}
