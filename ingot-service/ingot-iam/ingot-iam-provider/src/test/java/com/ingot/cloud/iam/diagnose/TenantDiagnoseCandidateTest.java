package com.ingot.cloud.iam.diagnose;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResolvedActionScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.evaluation.ScopeClause;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.TenantLiveMapper;
import com.ingot.cloud.iam.persistence.mapper.TenantScopeCandidateMapper;
import com.ingot.cloud.iam.persistence.mapper.TenantScopeCandidateSql;
import com.ingot.cloud.iam.persistence.projection.TenantLiveRows;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationCandidateKind;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.TenantAuthorizationConstants;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>组织诊断候选与诊断校验同口径：成员按读取范围、应用按开通、对象按操作者对象范围。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class TenantDiagnoseCandidateTest {
    private static final AuthorizationContext ACTOR =
            new AuthorizationContext(AuthorizationDomain.TENANT, "9", "1", "5");
    private static final String GROUP_READ = "iam-tenant:group:read";
    private final IamAccess access = mock(IamAccess.class);
    private final AuthorizationEvaluator evaluator = mock(AuthorizationEvaluator.class);
    private final ResourceAccess scopes = mock(ResourceAccess.class);
    private final MemberQueryRepository members = mock(MemberQueryRepository.class);
    private final DepartmentQueryRepository departments = mock(DepartmentQueryRepository.class);
    private final TenantLiveMapper live = mock(TenantLiveMapper.class);
    private final TenantScopeCandidateMapper candidates = mock(TenantScopeCandidateMapper.class);
    private final TenantDiagnoseCandidates service = new TenantDiagnoseCandidates(access, evaluator, scopes,
            members, departments, live, candidates);

    @Test
    void memberCandidatesUseMemberReadScope() {
        setup(List.of(IamAction.TENANT_MEMBER_READ.getCode()), Map.of());
        ObjectScope scope = ObjectScope.all();
        when(scopes.memberRead(ACTOR, IamAction.TENANT_MEMBER_READ)).thenReturn(scope);
        IamTenantMemberEntity member = new IamTenantMemberEntity();
        member.setId(BigInteger.valueOf(7));
        member.setDisplayName("张三");
        Page<IamTenantMemberEntity> page = new Page<>(1, 20);
        page.setRecords(List.of(member));
        page.setTotal(1);
        when(members.pageTenantCandidates(eq(9L), eq(scope), eq("张"), eq(List.of()), eq(1), eq(20)))
                .thenReturn(page);
        var result = service.list(AuthorizationCandidateKind.MEMBER, null, null, " 张 ", List.of(), 1, 20);
        assertEquals("7", result.items().getFirst().id());
        assertEquals("张三", result.items().getFirst().name());
    }

    @Test
    void memberCandidatesOnlyContainSelfWithoutMemberRead() {
        setup(List.of(), Map.of());
        var result = service.list(AuthorizationCandidateKind.MEMBER, null, null, null, null, 1, 20);
        assertEquals(1, result.total());
        assertEquals(ACTOR.memberId(), result.items().getFirst().id());
        verify(members, never()).pageTenantCandidates(anyLong(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void applicationCandidatesOnlyContainOpenedApplications() {
        setup(List.of(), Map.of());
        when(live.applications(BigInteger.valueOf(9))).thenReturn(List.of(
                new TenantLiveRows.Application(BigInteger.ONE, "crm", "客户管理", null, 1, null),
                new TenantLiveRows.Application(BigInteger.TWO, "erp", "进销存", null, 2, null)));
        var result = service.list(AuthorizationCandidateKind.APPLICATION, null, null, "客户", List.of(), 1, 20);
        assertEquals(1, result.total());
        assertEquals("1", result.items().getFirst().id());
    }

    @Test
    void objectCandidatesFollowOperatorActionObjects() {
        setup(List.of(GROUP_READ), Map.of(GROUP_READ, objects("42")));
        openedAction(GROUP_READ);
        when(live.resource(BigInteger.TEN)).thenReturn(new TenantScopeCandidateMapper.ActionResource(BigInteger.TEN,
                TenantAuthorizationConstants.CORE_APPLICATION, "group", "用户组"));
        service.list(AuthorizationCandidateKind.OBJECT, null, "70", "", List.of(), 1, 20);
        ArgumentCaptor<TenantScopeCandidateSql.Query> query = ArgumentCaptor.forClass(TenantScopeCandidateSql.Query.class);
        verify(candidates).page(query.capture());
        assertEquals(List.of(BigInteger.valueOf(42)), query.getValue().ids());
    }

    @Test
    void objectCandidatesAreEmptyWhenOperatorLacksTheAction() {
        setup(List.of(), Map.of());
        openedAction(GROUP_READ);
        when(live.resource(BigInteger.TEN)).thenReturn(new TenantScopeCandidateMapper.ActionResource(BigInteger.TEN,
                TenantAuthorizationConstants.CORE_APPLICATION, "group", "用户组"));
        var result = service.list(AuthorizationCandidateKind.OBJECT, null, "70", "", List.of(), 1, 20);
        assertEquals(0, result.total());
        verify(candidates, never()).page(any());
    }

    @Test
    void businessResourceObjectsAreUnsupported() {
        setup(List.of(), Map.of());
        openedAction("crm:customer:read");
        when(live.resource(BigInteger.TEN)).thenReturn(new TenantScopeCandidateMapper.ActionResource(BigInteger.TEN,
                "crm", "customer", "客户"));
        var result = service.list(AuthorizationCandidateKind.OBJECT, null, "70", null, null, 1, 20);
        assertFalse(result.supported());
    }

    @Test
    void otherKindsAreRejected() {
        setup(List.of(), Map.of());
        assertThrows(BizException.class, () -> service.list(AuthorizationCandidateKind.GROUP, null, null, null,
                null, 1, 20));
        verify(live, never()).applications(isNull());
    }

    @Test
    void deniesWrongApplicationAndUnopenedActions() {
        setup(List.of(GROUP_READ), Map.of(GROUP_READ, objects("42")));
        openedAction(GROUP_READ);
        assertThrows(BizException.class,
                () -> service.list(AuthorizationCandidateKind.OBJECT, "2", "70", null, null, 1, 20));
        when(live.catalog(BigInteger.valueOf(9), BigInteger.ONE, null, List.of(BigInteger.valueOf(70))))
                .thenReturn(List.of());
        assertThrows(BizException.class,
                () -> service.list(AuthorizationCandidateKind.OBJECT, "1", "70", null, null, 1, 20));
        verify(candidates, never()).page(any());
    }

    private void openedAction(String code) {
        var rows = List.of(action(code));
        when(live.actions(List.of(BigInteger.valueOf(70)))).thenReturn(rows);
        when(live.catalog(BigInteger.valueOf(9), BigInteger.ONE, null, List.of(BigInteger.valueOf(70))))
                .thenReturn(rows);
    }

    private void setup(List<String> codes, Map<String, ResolvedActionScope> scopeMap) {
        when(access.require(AuthorizationDomain.TENANT, IamAction.TENANT_AUTHORIZATION_DIAGNOSE))
                .thenReturn(new ActiveIdentity(ACTOR, "0", "0", null));
        when(access.allows(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_READ, false))
                .thenReturn(codes.contains(IamAction.TENANT_MEMBER_READ.getCode()));
        when(evaluator.evaluate(ACTOR)).thenReturn(new AuthorizationEvaluator.AuthorizationView(codes, List.of(),
                scopeMap, "1", Instant.now().plusSeconds(30)));
    }

    private static TenantLiveRows.Action action(String code) {
        return new TenantLiveRows.Action(BigInteger.valueOf(70), code, "查看", BigInteger.ONE, "组织",
                BigInteger.TEN, "资源", true, "[]", BigInteger.ONE, BigInteger.ONE, BigInteger.ONE);
    }

    private static ResolvedActionScope objects(String id) {
        return new ResolvedActionScope(List.of(new ScopeClause(false, false, false, false,
                List.of(), false, List.of(id))));
    }
}
