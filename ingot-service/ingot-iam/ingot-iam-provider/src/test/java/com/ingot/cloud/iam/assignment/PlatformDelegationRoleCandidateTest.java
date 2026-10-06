package com.ingot.cloud.iam.assignment;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import com.ingot.cloud.iam.authorization.IamActionAuthorizer.Admission;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.DelegationRepository;
import com.ingot.cloud.iam.persistence.RoleRepository;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateSql;
import com.ingot.cloud.iam.persistence.projection.AuthorizationRoleRow;
import com.ingot.cloud.iam.role.RoleService;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.support.IamCapabilities;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.RoleKind;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * <p>委派角色树以委派治理资格独立开放，并保持层级分页参数。</p>
 * @author jy
 * @since 1.0.0
 */
class PlatformDelegationRoleCandidateTest {
    private final IamAccess access = mock(IamAccess.class);
    private final AuthorizationCandidateMapper candidates = mock(AuthorizationCandidateMapper.class);
    private final PlatformAuthorizationEditor editor = new PlatformAuthorizationEditor(access,
            mock(AuthorizationEvaluator.class), mock(AssignmentRepository.class), mock(DelegationRepository.class),
            mock(RoleRepository.class), mock(RoleService.class), candidates, new com.ingot.cloud.iam.extension.BuiltinResourceProviders(candidates).registry(List.of()));

    @Test
    void governanceCanQueryVersionWithoutAssignmentRead() {
        when(access.requireCurrent()).thenReturn(actor(AuthorizationDomain.PLATFORM));
        when(access.capabilities(any(), anyList())).thenReturn(new IamCapabilities(Map.of(
                IamAction.PLATFORM_DELEGATION_CREATE, new Admission(true))));
        when(candidates.rolePage(any())).thenReturn(List.of(new AuthorizationRoleRow(BigInteger.valueOf(501),
                BigInteger.valueOf(400), "平台治理", RoleKind.PLATFORM_CUSTOM, BigInteger.TWO)));
        when(candidates.roleCount(any())).thenReturn(1L);

        var result = editor.delegationRoleCandidates("400", "平台", List.of("501"), 1, 20);
        assertEquals("501", result.items().getFirst().id());
        assertEquals("400", result.items().getFirst().roleId());
        ArgumentCaptor<AuthorizationCandidateSql.RoleQuery> query =
                ArgumentCaptor.forClass(AuthorizationCandidateSql.RoleQuery.class);
        verify(candidates).rolePage(query.capture());
        assertEquals(BigInteger.valueOf(400), query.getValue().roleId());
        assertEquals(List.of(BigInteger.valueOf(501)), query.getValue().ids());
        assertNull(query.getValue().allowedRevisionIds());
        verify(access, never()).require(any(), any());
    }

    @Test
    void tenantOrNonGovernanceActorCannotReadDelegationCandidates() {
        when(access.requireCurrent()).thenReturn(actor(AuthorizationDomain.TENANT));
        assertThrows(BizException.class, () -> editor.delegationRoleCandidates(null, "", List.of(), 1, 20));
        when(access.requireCurrent()).thenReturn(actor(AuthorizationDomain.PLATFORM));
        when(access.capabilities(any(), anyList())).thenReturn(new IamCapabilities(Map.of()));
        assertThrows(BizException.class, () -> editor.delegationRoleCandidates(null, "", List.of(), 1, 20));
        verifyNoInteractions(candidates);
    }

    private static ActiveIdentity actor(AuthorizationDomain domain) {
        return new ActiveIdentity(new AuthorizationContext(domain, domain == AuthorizationDomain.TENANT ? "9" : null,
                "1", "5"), "0", "0", null);
    }
}
