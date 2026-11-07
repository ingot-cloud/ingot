package com.ingot.cloud.iam.application.assignment;

import com.ingot.cloud.iam.application.authorization.candidate.PlatformAuthorizationCandidateQueryService;

import com.ingot.cloud.iam.domain.authorization.AuthorizationView;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.application.authorization.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.domain.authorization.ResolvedActionScope;
import com.ingot.cloud.iam.domain.authorization.ScopeClause;
import com.ingot.cloud.iam.domain.identity.ActiveIdentity;
import com.ingot.cloud.iam.infrastructure.persistence.AssignmentRepository;
import com.ingot.cloud.iam.infrastructure.persistence.DelegationRepository;
import com.ingot.cloud.iam.infrastructure.persistence.RoleRepository;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.AuthorizationCandidateSql;
import com.ingot.cloud.iam.infrastructure.persistence.projection.AuthorizationCandidateRow;
import com.ingot.cloud.iam.application.role.PlatformRoleFacade;
import com.ingot.cloud.iam.application.authorization.IamAccess;
import com.ingot.cloud.iam.domain.shared.AuthorizationCandidateKind;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>诊断成员与操作目标分别使用诊断范围和资源读取范围，不混用不同资源的 ID。</p>
 * @author jy
 * @since 1.0.0
 */
class PlatformDiagnoseCandidateTest {
    private static final AuthorizationContext ACTOR =
            new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "5");
    private final IamAccess access = mock(IamAccess.class);
    private final AuthorizationEvaluator evaluator = mock(AuthorizationEvaluator.class);
    private final AuthorizationCandidateMapper candidates = mock(AuthorizationCandidateMapper.class);
    private final PlatformAuthorizationCandidateQueryService editor = com.ingot.cloud.iam.fixture.PlatformAuthorizationCandidateFixture.create(access, evaluator,
            mock(AssignmentRepository.class), mock(DelegationRepository.class), mock(RoleRepository.class),
            mock(PlatformRoleFacade.class), candidates, new com.ingot.cloud.iam.application.authorization.extension.BuiltinResourceProviders(candidates).registry(List.of()));

    @Test
    void targetCandidatesUseResourceReadScopeInsteadOfDiagnosableMemberIds() {
        setup(List.of(IamAction.VALUE_PLATFORM_AUTHORIZATION_DIAGNOSE,
                IamAction.VALUE_PLATFORM_APPLICATION_READ));
        editor.diagnose(AuthorizationCandidateKind.OBJECT, "70", "10", "", List.of(), 1, 20);
        ArgumentCaptor<AuthorizationCandidateSql.Query> query = ArgumentCaptor.forClass(AuthorizationCandidateSql.Query.class);
        verify(candidates).page(query.capture());
        assertEquals(List.of(BigInteger.valueOf(42)), query.getValue().allowedIds());
        assertEquals("application", query.getValue().objectResource());
    }

    @Test
    void targetCandidatesWithoutResourceReadPermissionAreEmpty() {
        setup(List.of(IamAction.VALUE_PLATFORM_AUTHORIZATION_DIAGNOSE));
        editor.diagnose(AuthorizationCandidateKind.OBJECT, "70", "10", "", List.of(), 1, 20);
        ArgumentCaptor<AuthorizationCandidateSql.Query> query = ArgumentCaptor.forClass(AuthorizationCandidateSql.Query.class);
        verify(candidates).page(query.capture());
        assertEquals(List.of(), query.getValue().allowedIds());
    }

    @Test
    void actionCandidateKeepsItsNameAndIdAndExposesTheResourceSummary() {
        setup(List.of(IamAction.VALUE_PLATFORM_AUTHORIZATION_DIAGNOSE));
        when(candidates.page(any(AuthorizationCandidateSql.Query.class))).thenReturn(List.of(
                new AuthorizationCandidateRow(BigInteger.valueOf(70), "创建", null, null, "成员管理")));
        var page = editor.diagnose(AuthorizationCandidateKind.ACTION, null, "10", "创建", List.of(), 1, 20);
        var option = page.items().getFirst();
        assertEquals("70", option.id());
        assertEquals("创建", option.name());
        assertEquals("成员管理", option.summary());
    }

    private void setup(List<String> codes) {
        when(access.require(eq(AuthorizationDomain.PLATFORM), eq(IamAction.PLATFORM_AUTHORIZATION_DIAGNOSE)))
                .thenReturn(new ActiveIdentity(ACTOR, "0", "0", null));
        when(evaluator.evaluate(ACTOR)).thenReturn(new AuthorizationView(codes, List.of(),
                Map.of(IamAction.VALUE_PLATFORM_AUTHORIZATION_DIAGNOSE, scope("5"),
                        IamAction.VALUE_PLATFORM_APPLICATION_READ, scope("42")),
                "1", Instant.now().plusSeconds(30)));
        when(candidates.actions(List.of(BigInteger.valueOf(70)))).thenReturn(List.of(
                new AuthorizationCandidateMapper.ActionRow(BigInteger.valueOf(70), "查看应用",
                        IamAction.VALUE_PLATFORM_APPLICATION_READ, BigInteger.TEN, "平台",
                        "iam-platform", BigInteger.ONE, "应用", "application", "[]")));
        when(candidates.page(any(AuthorizationCandidateSql.Query.class))).thenReturn(List.of());
    }

    private static ResolvedActionScope scope(String id) {
        return new ResolvedActionScope(List.of(new ScopeClause(false, false, false, false,
                List.of(), false, List.of(id))));
    }
}
