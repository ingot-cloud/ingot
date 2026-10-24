package com.ingot.cloud.iam.diagnose;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluationRepository;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.entity.IamApplicationEntity;
import com.ingot.cloud.iam.persistence.entity.IamAuthorizationAuditEntity;
import com.ingot.cloud.iam.persistence.entity.IamTenantMemberEntity;
import com.ingot.cloud.iam.persistence.mapper.IamApplicationMapper;
import com.ingot.cloud.iam.persistence.mapper.IamPlatformMemberMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.cloud.iam.persistence.projection.AuthorizationEvalRows;
import com.ingot.cloud.iam.policy.FieldAccessEvaluator;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.Decision;
import com.ingot.framework.commons.model.iam.DiagnoseInput;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
    void platformTargetRequiresTheSelectedResourceReadScope() {
        DiagnoseAuditService service = service();
        AuthorizationContext platform = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "101");
        when(access.require(AuthorizationDomain.PLATFORM, IamAction.PLATFORM_AUTHORIZATION_DIAGNOSE))
                .thenReturn(new ActiveIdentity(platform, "0", "0", null));
        when(platformMembers.selectCount(any(Wrapper.class))).thenReturn(1L);
        IamApplicationEntity application = new IamApplicationEntity();
        application.setId(BigInteger.valueOf(2));
        application.setDomain(AuthorizationDomain.PLATFORM);
        application.setEnabled(true);
        when(applications.selectOne(any(Wrapper.class))).thenReturn(application);
        when(evaluations.listActions(BigInteger.valueOf(12))).thenReturn(List.of(
                new AuthorizationEvalRows.Action(IamAction.VALUE_PLATFORM_APPLICATION_READ, true,
                        AuthorizationDomain.PLATFORM, true, BigInteger.valueOf(2))));
        AuthorizationEvaluator.AuthorizationView view = new AuthorizationEvaluator.AuthorizationView(
                List.of(IamAction.VALUE_PLATFORM_AUTHORIZATION_DIAGNOSE), List.of(), Map.of(),
                "1", Instant.now().plusSeconds(30));
        when(evaluator.evaluate(any())).thenReturn(view);
        when(scopes.targetAllowed(platform, view, IamAction.VALUE_PLATFORM_AUTHORIZATION_DIAGNOSE, "102"))
                .thenReturn(true);
        BizException exception = assertThrows(BizException.class, () -> service.diagnose(
                AuthorizationDomain.PLATFORM, input("102", "2", "12", "888")));
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), exception.getCode());
        verify(scopes).targetAllowed(platform, view, IamAction.VALUE_PLATFORM_APPLICATION_READ, "888");
    }

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

    private DiagnoseInput input(String memberId, String applicationId, String actionId, String targetId) {
        return new DiagnoseInput(memberId, null, applicationId, actionId, targetId);
    }

    private IamAccess access;
    private AuthorizationEvaluator evaluator;
    private AuthorizationEvaluationRepository evaluations;
    private ResourceAccess scopes;
    private FieldAccessEvaluator fieldAccess;
    private IamTenantMemberMapper tenantMembers;
    private IamPlatformMemberMapper platformMembers;
    private IamApplicationMapper applications;

    private DiagnoseAuditService service() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(), "test"),
                IamAuthorizationAuditEntity.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
                new org.apache.ibatis.builder.MapperBuilderAssistant(new org.apache.ibatis.session.Configuration(), "test-member"),
                IamTenantMemberEntity.class);

        access = mock(IamAccess.class);
        evaluator = mock(AuthorizationEvaluator.class);
        evaluations = mock(AuthorizationEvaluationRepository.class);
        scopes = mock(ResourceAccess.class);
        fieldAccess = mock(FieldAccessEvaluator.class);
        platformMembers = mock(IamPlatformMemberMapper.class);
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
        return new DiagnoseAuditService(access, evaluator, evaluations, scopes, fieldAccess,
                platformMembers, tenantMembers, applications);
    }
}
