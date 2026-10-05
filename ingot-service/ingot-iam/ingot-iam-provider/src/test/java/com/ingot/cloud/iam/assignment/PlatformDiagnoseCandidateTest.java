package com.ingot.cloud.iam.assignment;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.evaluation.ResolvedActionScope;
import com.ingot.cloud.iam.evaluation.ScopeClause;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.AssignmentRepository;
import com.ingot.cloud.iam.persistence.DelegationRepository;
import com.ingot.cloud.iam.persistence.RoleRepository;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateSql;
import com.ingot.cloud.iam.persistence.projection.AuthorizationCandidateRow;
import com.ingot.cloud.iam.role.RoleService;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.model.iam.AuthorizationCandidateKind;
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
    private final PlatformAuthorizationEditor editor = new PlatformAuthorizationEditor(access, evaluator,
            mock(AssignmentRepository.class), mock(DelegationRepository.class), mock(RoleRepository.class),
            mock(RoleService.class), candidates);

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
        when(evaluator.evaluate(ACTOR)).thenReturn(new AuthorizationEvaluator.AuthorizationView(codes, List.of(),
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
