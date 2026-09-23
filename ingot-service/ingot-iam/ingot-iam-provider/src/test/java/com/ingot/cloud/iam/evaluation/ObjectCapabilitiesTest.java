package com.ingot.cloud.iam.evaluation;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.iam.organization.MemberMutationGuard;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.ObjectCapability;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * <p>验证对象能力按操作有无与对象范围分别给出原因，空映射不再充当已实现。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class ObjectCapabilitiesTest {
    private static final AuthorizationContext ACTOR =
            new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");

    @Test
    void missingActionIsDeniedAndOutOfScopeUsesDataScope() {
        AuthorizationEvaluator evaluator = mock(AuthorizationEvaluator.class);
        ResourceAccess scopes = mock(ResourceAccess.class);
        AuthorizationEvaluator.AuthorizationView view = new AuthorizationEvaluator.AuthorizationView(
                List.of(IamAction.VALUE_TENANT_MEMBER_UPDATE, IamAction.VALUE_TENANT_MEMBER_STATUS), List.of(),
                Map.of(), "31", Instant.now().plusSeconds(30));
        when(evaluator.evaluate(ACTOR)).thenReturn(view);
        when(scopes.memberVisible(eq(ACTOR), eq(view), eq(IamAction.TENANT_MEMBER_UPDATE), eq(102L)))
                .thenReturn(true);
        when(scopes.memberWriteAllowed(eq(ACTOR), eq(view), eq(IamAction.TENANT_MEMBER_STATUS), eq("102"),
                eq(MemberMutationGuard.Operation.CHANGE_STATUS), any(), any())).thenReturn(false);
        ObjectCapabilities capabilities = new ObjectCapabilities(evaluator, scopes);
        ObjectCapabilities.Snapshot snapshot = capabilities.snapshot(ACTOR);
        Map<String, ObjectCapability> result = capabilities.tenantMember(snapshot,
                new MemberRecord("102", "成员", null, null, null, null, MemberStatus.ACTIVE, List.of()));
        assertTrue(result.get(IamAction.VALUE_TENANT_MEMBER_UPDATE).allowed());
        assertNull(result.get(IamAction.VALUE_TENANT_MEMBER_UPDATE).reasonCode());
        assertFalse(result.get(IamAction.VALUE_TENANT_MEMBER_STATUS).allowed());
        assertEquals(IamReasonCode.DATA_SCOPE_DENIED, result.get(IamAction.VALUE_TENANT_MEMBER_STATUS).reasonCode());
        assertFalse(result.get(IamAction.VALUE_TENANT_MEMBER_REMOVE).allowed());
        assertEquals(IamReasonCode.ACTION_DENIED, result.get(IamAction.VALUE_TENANT_MEMBER_REMOVE).reasonCode());
        assertFalse(result.get(IamAction.VALUE_TENANT_MEMBER_DEPARTMENTS).allowed());
        assertEquals(IamReasonCode.ACTION_DENIED, result.get(IamAction.VALUE_TENANT_MEMBER_DEPARTMENTS).reasonCode());
    }
}
