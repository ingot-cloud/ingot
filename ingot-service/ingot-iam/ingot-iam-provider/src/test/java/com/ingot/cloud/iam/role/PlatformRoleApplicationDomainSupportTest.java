package com.ingot.cloud.iam.role;

import java.math.BigInteger;

import com.ingot.cloud.iam.assignment.AssignmentService;
import com.ingot.cloud.iam.authorization.IamActionAuthorizer;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.identity.ActiveIdentityService;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.RoleRepository;
import com.ingot.cloud.iam.persistence.entity.IamRoleApplicationTaskEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleDefinitionEntity;
import com.ingot.cloud.iam.persistence.entity.IamRoleRevisionEntity;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamAdmission;
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
class PlatformRoleApplicationDomainSupportTest {
    private final IamAccess access = mock(IamAccess.class);
    private final RoleService roles = mock(RoleService.class);
    private final RoleRepository roleStore = mock(RoleRepository.class);
    private final AssignmentRepository assignmentStore = mock(AssignmentRepository.class);
    private final AssignmentService assignments = mock(AssignmentService.class);
    private final ResourceAccess resources = mock(ResourceAccess.class);
    private final ActiveIdentityService identities = mock(ActiveIdentityService.class);
    private final IamActionAuthorizer authorizer = mock(IamActionAuthorizer.class);
    private final PlatformRoleApplicationDomainSupport support = new PlatformRoleApplicationDomainSupport(
            access, roles, roleStore, assignmentStore, assignments, resources, identities, authorizer);
    private final AuthorizationContext context = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "2");
    private final ActiveIdentity actor = new ActiveIdentity(context, "1", "1", null);
    private final IamAdmission admission = new IamAdmission(actor, true);

    @Test
    void rejectsDelegatedAndForeignDomainAdmissionsBeforeBusinessReads() {
        when(access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_UPGRADE))
                .thenReturn(new IamAdmission(actor, false));
        denied(IamReasonCode.ACTION_DENIED, support::admit);
        var tenant = new IamAdmission(new ActiveIdentity(new AuthorizationContext(
                AuthorizationDomain.TENANT, "3", "1", "2"), "1", "1", "1"), true);
        when(access.admit(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_ASSIGNMENT_UPGRADE)).thenReturn(tenant);
        denied(IamReasonCode.ACTION_DENIED, support::admit);
        denied(IamReasonCode.ACTION_DENIED, () -> support.requireRole("10", tenant, true));
        denied(IamReasonCode.ACTION_DENIED, () -> support.scope(tenant, true));
        verifyNoInteractions(roleStore, assignmentStore, assignments, resources, identities, authorizer);
    }

    @Test
    void restoresSavedIdentityAndChecksCurrentGovernanceWithoutHttpAdmission() {
        var task = new IamRoleApplicationTaskEntity();
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
        var task = new IamRoleApplicationTaskEntity();
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
