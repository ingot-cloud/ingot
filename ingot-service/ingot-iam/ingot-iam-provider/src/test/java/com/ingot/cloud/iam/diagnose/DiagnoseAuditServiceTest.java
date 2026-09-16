package com.ingot.cloud.iam.diagnose;

import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluationRepository;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.persistence.entity.IamAuthorizationAuditEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.persistence.mapper.IamAuthorizationAuditMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.policy.FieldPolicySnapshot;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuditChangeType;
import com.ingot.framework.commons.model.iam.AuditEntry;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.Decision;
import com.ingot.framework.commons.model.iam.DiagnoseInput;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberFieldKey;
import com.ingot.framework.commons.model.iam.PolicyScenario;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>验证诊断必须核对应用与操作关联及目标对象范围，不能只看操作码集合。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class DiagnoseAuditServiceTest {
    private static final AuthorizationContext ACTOR =
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");
    private static final ActiveIdentity IDENTITY = new ActiveIdentity(ACTOR, "0", "0", null);

    @Test
    void actionFromAnotherApplicationIsNotFound() {
        DiagnoseAuditService service = service();
        when(evaluations.listActions(BigInteger.valueOf(12)))
                .thenReturn(List.of(new AuthorizationEvalRows.Action(IamAction.VALUE_TENANT_MEMBER_UPDATE, true,
                        AuthorizationDomain.TENANT, true, BigInteger.valueOf(99))));
        BizException exception = assertThrows(BizException.class,
                () -> service.diagnose(AuthorizationDomain.TENANT, input("102", "2", "12", null)));
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void actionPresentWithoutObjectCoverageIsDataScopeDenied() {
        DiagnoseAuditService service = service();
        when(evaluations.listActions(BigInteger.valueOf(12)))
                .thenReturn(List.of(new AuthorizationEvalRows.Action(IamAction.VALUE_TENANT_MEMBER_UPDATE, true,
                        AuthorizationDomain.TENANT, true, BigInteger.valueOf(2))));
        when(evaluations.entitlement(any(), any(), any()))
                .thenReturn(new AuthorizationEvalRows.Entitlement(1, null));
        AuthorizationEvaluator.AuthorizationView view = new AuthorizationEvaluator.AuthorizationView(
                List.of(IamAction.VALUE_TENANT_MEMBER_UPDATE), List.of(), Map.of(), "31",
                Instant.now().plusSeconds(30));
        when(evaluator.evaluate(any())).thenReturn(view);
        when(scopes.targetAllowed(any(), eq(view), eq(IamAction.VALUE_TENANT_MEMBER_UPDATE), eq("888")))
                .thenReturn(false);
        Decision decision = service.diagnose(AuthorizationDomain.TENANT, input("102", "2", "12", "888"));
        assertFalse(decision.allowed());
        assertEquals(IamReasonCode.DATA_SCOPE_DENIED, decision.reasonCode());
        assertTrue(decision.sources().isEmpty());
    }

    @Test
    void operatorWithoutTargetMemberSeesNotFound() {
        DiagnoseAuditService service = service();
        doThrow(new BizException(IamReasonCode.OBJECT_NOT_FOUND))
                .when(scopes).requireVisibleMember(eq(ACTOR), eq(IamAction.TENANT_MEMBER_READ), eq(102L));
        BizException exception = assertThrows(BizException.class,
                () -> service.diagnose(AuthorizationDomain.TENANT, input("102", "2", "12", null)));
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), exception.getCode());
    }

    @Test
    void listAuditsReturnsDelegationAssignmentAndTrace() {
        DiagnoseAuditService service = service();
        when(access.require(eq(AuthorizationDomain.TENANT), eq(IamAction.TENANT_AUDIT_READ))).thenReturn(IDENTITY);
        IamAuthorizationAuditEntity row = new IamAuthorizationAuditEntity();
        row.setId(BigInteger.valueOf(9));
        row.setActorAccountId(BigInteger.ONE);
        row.setActorMemberId(BigInteger.valueOf(101));
        row.setDomain(AuthorizationDomain.TENANT);
        row.setTenantId(BigInteger.TEN);
        row.setTargetType("assignment");
        row.setTargetId("88");
        row.setChangeType(AuditChangeType.CREATE);
        row.setSafeBefore("{}");
        row.setSafeAfter("{}");
        row.setRevisions("{}");
        row.setDelegationId(BigInteger.valueOf(7));
        row.setAssignmentId(BigInteger.valueOf(88));
        row.setTraceId("trace-1");
        row.setOccurredAt(LocalDateTime.now(ZoneOffset.UTC));
        Page<IamAuthorizationAuditEntity> page = new Page<>(1, 20);
        page.setRecords(List.of(row));
        page.setTotal(1);
        when(audits.selectPage(any(Page.class), any())).thenReturn(page);
        when(evaluator.evaluate(any())).thenReturn(new AuthorizationEvaluator.AuthorizationView(
                List.of(IamAction.VALUE_TENANT_MEMBER_READ), List.of(), Map.of(), "1", Instant.now().plusSeconds(30)));
        when(scopes.memberRead(eq(ACTOR), eq(IamAction.TENANT_MEMBER_READ))).thenReturn(ObjectScope.all());
        IamTenantMemberEntity member = new IamTenantMemberEntity();
        member.setId(BigInteger.valueOf(101));
        member.setDisplayName("管理者");
        when(tenantMembers.selectList(any())).thenReturn(List.of(member));
        FieldPolicySnapshot snapshot = mock(FieldPolicySnapshot.class);
        when(fieldAccess.snapshot(eq(10L), eq(PolicyScenario.MANAGEMENT))).thenReturn(snapshot);
        when(fieldAccess.access(eq(snapshot), eq(101L), eq(101L), eq(MemberFieldKey.VALUE_DISPLAY_NAME)))
                .thenReturn(new FieldAccess(FieldVisibility.FULL, true));
        AuditEntry entry = service.listAudits(AuthorizationDomain.TENANT, 1, 20).items().getFirst().record();
        assertEquals("7", entry.delegationId());
        assertEquals("88", entry.assignmentId());
        assertEquals("trace-1", entry.traceId());
        assertEquals("管理者", entry.actor().displayName());
    }

    @Test
    void listAuditsOmitsActorNameWithoutMemberRead() {
        DiagnoseAuditService service = service();
        when(access.require(eq(AuthorizationDomain.TENANT), eq(IamAction.TENANT_AUDIT_READ))).thenReturn(IDENTITY);
        IamAuthorizationAuditEntity row = new IamAuthorizationAuditEntity();
        row.setId(BigInteger.valueOf(9));
        row.setActorAccountId(BigInteger.ONE);
        row.setActorMemberId(BigInteger.valueOf(101));
        row.setDomain(AuthorizationDomain.TENANT);
        row.setTenantId(BigInteger.TEN);
        row.setTargetType("member");
        row.setTargetId("102");
        row.setChangeType(AuditChangeType.UPDATE);
        row.setSafeBefore("{}");
        row.setSafeAfter("{}");
        row.setRevisions("{}");
        row.setOccurredAt(LocalDateTime.now(ZoneOffset.UTC));
        Page<IamAuthorizationAuditEntity> page = new Page<>(1, 20);
        page.setRecords(List.of(row));
        page.setTotal(1);
        when(audits.selectPage(any(Page.class), any())).thenReturn(page);
        when(evaluator.evaluate(any())).thenReturn(new AuthorizationEvaluator.AuthorizationView(
                List.of(), List.of(), Map.of(), "1", Instant.now().plusSeconds(30)));
        AuditEntry entry = service.listAudits(AuthorizationDomain.TENANT, 1, 20).items().getFirst().record();
        assertNull(entry.actor().displayName());
        assertNull(entry.delegationId());
    }

    private DiagnoseInput input(String memberId, String applicationId, String actionId, String targetId) {
        return new DiagnoseInput(memberId, null, applicationId, actionId, targetId);
    }

    private IamAccess access;
    private AuthorizationEvaluator evaluator;
    private AuthorizationEvaluationRepository evaluations;
    private ResourceAccess scopes;
    private FieldAccessEvaluator fieldAccess;
    private IamAuthorizationAuditMapper audits;
    private IamTenantMemberMapper tenantMembers;
    private IamApplicationMapper applications;

    private DiagnoseAuditService service() {
        access = mock(IamAccess.class);
        evaluator = mock(AuthorizationEvaluator.class);
        evaluations = mock(AuthorizationEvaluationRepository.class);
        scopes = mock(ResourceAccess.class);
        fieldAccess = mock(FieldAccessEvaluator.class);
        audits = mock(IamAuthorizationAuditMapper.class);
        IamPlatformMemberMapper platformMembers = mock(IamPlatformMemberMapper.class);
        tenantMembers = mock(IamTenantMemberMapper.class);
        applications = mock(IamApplicationMapper.class);
        when(access.require(eq(AuthorizationDomain.TENANT), eq(IamAction.TENANT_AUTHORIZATION_DIAGNOSE)))
                .thenReturn(IDENTITY);
        when(tenantMembers.selectCount(any(Wrapper.class))).thenReturn(1L);
        IamApplicationEntity application = new IamApplicationEntity();
        application.setId(BigInteger.valueOf(2));
        application.setDomain(AuthorizationDomain.TENANT);
        application.setEnabled(true);
        when(applications.selectOne(any(Wrapper.class))).thenReturn(application);
        when(fieldAccess.memberAccess(eq(10L), eq(101L), eq(888L), any())).thenReturn(Map.of());
        return new DiagnoseAuditService(access, evaluator, evaluations, scopes, fieldAccess, audits,
                platformMembers, tenantMembers, applications);
    }
}
