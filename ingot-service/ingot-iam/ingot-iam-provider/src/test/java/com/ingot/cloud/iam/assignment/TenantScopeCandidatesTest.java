package com.ingot.cloud.iam.assignment;

import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.IamRoleRevisionJoin;
import com.ingot.cloud.iam.persistence.RoleRepository;
import com.ingot.cloud.iam.persistence.entity.IamRoleParameterEntity;
import com.ingot.cloud.iam.persistence.mapper.TenantScopeCandidateMapper;
import com.ingot.cloud.iam.role.RoleService;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * <p>租户候选服务不借用普通人员权限，也不接受未知或跨资源对象。</p>
 * @author jy
 * @since 1.0.0
 */
class TenantScopeCandidatesTest {
    private final IamAccess access = mock(IamAccess.class);
    private final AssignmentRepository assignments = mock(AssignmentRepository.class);
    private final RoleRepository roleStore = mock(RoleRepository.class);
    private final RoleService roles = mock(RoleService.class);
    private final TenantScopeCandidateMapper mapper = mock(TenantScopeCandidateMapper.class);
    private final TenantScopeCandidates service = new TenantScopeCandidates(access, assignments, roleStore, roles, mapper);
    private final ActiveIdentity actor = new ActiveIdentity(
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "2"), "0", "0", "0");

    @BeforeEach
    void fixture() {
        when(access.requireCurrent()).thenReturn(actor);
        var revision = new IamRoleRevisionJoin();
        revision.setKind(RoleKind.SHARED);
        revision.setDomain(AuthorizationDomain.TENANT);
        revision.setEnabled(true);
        when(assignments.findRevision(50)).thenReturn(revision);
        var parameter = new IamRoleParameterEntity();
        parameter.setParameterKey("objects_20");
        parameter.setBindingKind(ScopeBindingKind.OBJECTS);
        when(roleStore.listParameters(50)).thenReturn(List.of(parameter));
        when(roles.synthesizedGrants(50)).thenReturn(List.of(new ActionGrant("30",
                List.of(new ScopeExpression(ScopeKind.OBJECT_SET, "objects_20", null)))));
    }

    @Test
    void candidatesRequireAssignmentQualification() {
        assertThrows(BizException.class, () -> service.list("50", "objects_20", "", List.of(), 1, 20));
        verifyNoInteractions(mapper);
    }

    @Test
    void unregisteredOrMixedResourcesNeverFallBackToAll() {
        when(access.allows(eq(AuthorizationDomain.TENANT), eq(IamAction.TENANT_ASSIGNMENT_CREATE), eq(false)))
                .thenReturn(true);
        when(mapper.actions(List.of(BigInteger.valueOf(30))))
                .thenReturn(List.of(new TenantScopeCandidateMapper.ActionResource(
                        BigInteger.valueOf(30), "other-app", "member", "成员")));
        var response = service.list("50", "objects_20", "", List.of(), 1, 20);
        assertFalse(response.supported());
        assertEquals(0, response.total());
        verify(mapper, never()).page(any());
    }

    @Test
    void oldSharedKeyMaySpanResourcesWithTheSameMemberTarget() {
        when(access.allows(eq(AuthorizationDomain.TENANT), eq(IamAction.TENANT_ASSIGNMENT_CREATE), eq(false)))
                .thenReturn(true);
        when(roles.synthesizedGrants(50)).thenReturn(List.of(
                new ActionGrant("30", List.of(new ScopeExpression(ScopeKind.OBJECT_SET, "objects_20", null))),
                new ActionGrant("31", List.of(new ScopeExpression(ScopeKind.OBJECT_SET, "objects_20", null)))));
        when(mapper.actions(List.of(BigInteger.valueOf(30), BigInteger.valueOf(31))))
                .thenReturn(List.of(
                        new TenantScopeCandidateMapper.ActionResource(BigInteger.valueOf(30), "iam-tenant", "member", "成员"),
                        new TenantScopeCandidateMapper.ActionResource(BigInteger.valueOf(31), "iam-tenant", "directory", "通讯录")));
        var response = service.list("50", "objects_20", "", List.of(), 1, 20);
        assertTrue(response.supported());
        assertEquals("成员 / 通讯录", response.contextLabel());
        verify(mapper).page(any());
    }

    @Test
    void submitRejectsObjectOutsideTheResolvedTenantResource() {
        when(mapper.actions(List.of(BigInteger.valueOf(30))))
                .thenReturn(List.of(new TenantScopeCandidateMapper.ActionResource(
                        BigInteger.valueOf(30), "iam-tenant", "member", "成员")));
        when(mapper.count(any())).thenReturn(0L);
        var input = new AssignmentInput(new SubjectRef(SubjectType.MEMBER, "2"),
                new RoleRevisionRef(RoleKind.SHARED, "50"),
                Map.of("objects_20", new ScopeBinding(ScopeBindingKind.OBJECTS, List.of("4"))),
                null, null, null);
        assertEquals(IamReasonCode.INVALID_ARGUMENT, service.validate(actor, input).getFirst().code());
    }
}
