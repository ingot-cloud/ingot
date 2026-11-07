package com.ingot.cloud.iam.application.assignment;

import com.ingot.cloud.iam.application.authorization.candidate.PlatformAuthorizationCandidateQueryService;

import com.ingot.cloud.iam.infrastructure.persistence.AssignmentRepository;
import com.ingot.cloud.iam.infrastructure.persistence.DelegationRepository;
import com.ingot.cloud.iam.infrastructure.persistence.IamRoleRevisionJoin;
import com.ingot.cloud.iam.infrastructure.persistence.RoleRepository;
import com.ingot.framework.commons.model.iam.ActionGrant;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.RoleRevisionRef;
import com.ingot.framework.commons.model.iam.ScopeBinding;
import com.ingot.framework.commons.model.iam.ScopeBindingKind;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.ScopeKind;

import com.ingot.cloud.iam.application.assignment.model.AssignmentInput;
import com.ingot.cloud.iam.application.assignment.model.SubjectRef;
import com.ingot.cloud.iam.domain.assignment.GrantStatus;
import com.ingot.cloud.iam.domain.assignment.SubjectType;
import com.ingot.cloud.iam.domain.shared.AuthorizationCandidateKind;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import com.ingot.cloud.iam.application.authorization.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.entity.IamDelegationGrantEntity;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.AuthorizationCandidateSql;
import com.ingot.cloud.iam.application.role.PlatformRoleFacade;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>分配记录的已选回显仅使用可信记录关系，不要求通用创建资格或客户端完整 ID 集合。</p>
 * @author jy
 * @since 1.0.0
 */
class PlatformAssignmentSelectionTest {
    private final IamAccess access = mock(IamAccess.class);
    private final AssignmentRepository assignments = mock(AssignmentRepository.class);
    private final DelegationRepository delegations = mock(DelegationRepository.class);
    private final PlatformRoleFacade roles = mock(PlatformRoleFacade.class);
    private final AuthorizationCandidateMapper candidates = mock(AuthorizationCandidateMapper.class);
    private final PlatformAuthorizationCandidateQueryService editor = com.ingot.cloud.iam.fixture.PlatformAuthorizationCandidateFixture.create(access,
            mock(AuthorizationEvaluator.class), assignments, delegations, mock(RoleRepository.class), roles, candidates, new com.ingot.cloud.iam.application.authorization.extension.BuiltinResourceProviders(candidates).registry(List.of()));
    private final ActiveIdentity actor = new ActiveIdentity(
            new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "99"), "0", "0", "0");

    @Test
    void fixedRoleReplayUsesStoredAssignmentFilterWithoutCreatingQualification() {
        editor.selectedAssignmentCandidates(actor, "90", input(null), AuthorizationCandidateKind.ROLE_REVISION, null, 2, 20);
        var query = ArgumentCaptor.forClass(AuthorizationCandidateSql.Query.class);
        verify(candidates).page(query.capture());
        assertEquals(BigInteger.valueOf(90), query.getValue().selectedAssignmentId());
        assertEquals(20, query.getValue().offset());
        assertTrue(query.getValue().ids().isEmpty());
        verifyNoInteractions(access);
    }

    @Test
    void objectReplayKeepsParameterAndResourceContextAndDoesNotUseIdsFilter() {
        var revision = new IamRoleRevisionJoin();
        revision.setDomain(AuthorizationDomain.PLATFORM); revision.setEnabled(true);
        when(assignments.findRevision(31)).thenReturn(revision);
        when(roles.synthesizedGrants(31)).thenReturn(List.of(new ActionGrant("331",
                List.of(new ScopeExpression(ScopeKind.OBJECT_SET, "objects", false)))));
        when(candidates.actions(anyList())).thenReturn(List.of(new AuthorizationCandidateMapper.ActionRow(
                BigInteger.valueOf(331), "查看", "iam-platform:application:read", BigInteger.ONE, "平台",
                "iam-platform", BigInteger.valueOf(330), "应用", "application", "[\"ALL\",\"OBJECT_SET\"]")));
        editor.selectedAssignmentCandidates(actor, "90", input(null), AuthorizationCandidateKind.OBJECT, "objects", 1, 20);
        var query = ArgumentCaptor.forClass(AuthorizationCandidateSql.Query.class);
        verify(candidates).page(query.capture());
        assertEquals("objects", query.getValue().selectedParameterKey());
        assertEquals("application", query.getValue().objectResource());
        assertEquals(BigInteger.valueOf(90), query.getValue().selectedAssignmentId());
        assertTrue(query.getValue().ids().isEmpty());
    }

    @Test
    void revokedSourceAndInvalidKindsCannotFallBackToUnrestrictedCandidates() {
        var source = new IamDelegationGrantEntity(); source.setId(BigInteger.valueOf(60));
        source.setPlatformAdministratorId(BigInteger.valueOf(99)); source.setStatus(GrantStatus.REVOKED);
        when(delegations.find(AuthorizationDomain.PLATFORM, null, 60)).thenReturn(source);
        var unavailable = editor.selectedAssignmentCandidates(actor, "90", input("60"), AuthorizationCandidateKind.OBJECT, "objects", 1, 20);
        assertFalse(unavailable.supported());
        assertTrue(unavailable.items().isEmpty());
        assertThrows(BizException.class, () -> editor.selectedAssignmentCandidates(actor, "90", input(null), AuthorizationCandidateKind.MEMBER, null, 1, 20));
        assertThrows(BizException.class, () -> editor.selectedAssignmentCandidates(actor, "90", input(null), AuthorizationCandidateKind.OBJECT, "other", 1, 20));
        verifyNoInteractions(candidates);
    }

    private static AssignmentInput input(String source) {
        return new AssignmentInput(new SubjectRef(SubjectType.MEMBER, "1"),
                new RoleRevisionRef(RoleKind.PLATFORM_CUSTOM, "31"),
                Map.of("objects", new ScopeBinding(ScopeBindingKind.OBJECTS, List.of("100"))), null, null, source);
    }
}
