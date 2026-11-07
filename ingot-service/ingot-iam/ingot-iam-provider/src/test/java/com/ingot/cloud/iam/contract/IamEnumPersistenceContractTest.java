package com.ingot.cloud.iam.contract;

import com.ingot.cloud.iam.domain.account.AccountLookupPurpose;
import com.ingot.cloud.iam.domain.assignment.AssignmentSource;
import com.ingot.cloud.iam.domain.assignment.GrantStatus;
import com.ingot.cloud.iam.domain.assignment.SubjectType;
import com.ingot.cloud.iam.domain.audit.AuditChangeType;
import com.ingot.cloud.iam.domain.catalog.ActionMatchMode;
import com.ingot.cloud.iam.domain.catalog.AudienceKind;
import com.ingot.cloud.iam.domain.catalog.EntitlementSource;
import com.ingot.cloud.iam.domain.catalog.MenuAccessMode;
import com.ingot.cloud.iam.domain.catalog.MenuKind;
import com.ingot.cloud.iam.domain.download.ExportTaskStatus;
import com.ingot.cloud.iam.domain.organization.MemberStatus;
import com.ingot.cloud.iam.domain.policy.MemberFieldKey;
import com.ingot.cloud.iam.domain.shared.DefaultPolicyKind;
import com.ingot.cloud.iam.domain.shared.SelectionPurpose;
import com.ingot.cloud.iam.domain.shared.SensitiveConfirmationKind;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamActionOperation;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.RoleKind;
import com.ingot.framework.commons.model.iam.ScopeKind;

import com.baomidou.mybatisplus.core.handlers.CompositeEnumTypeHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>验证 IAM 枚举可被生产配置的 CompositeEnumTypeHandler 构造，且 JSON 输出稳定字面量。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class IamEnumPersistenceContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void compositeHandlerDoesNotRecurse() {
        for (Class type : new Class[]{
                MemberStatus.class, AuthorizationDomain.class, RoleKind.class, SubjectType.class, GrantStatus.class,
                AssignmentSource.class, DefaultPolicyKind.class, AudienceKind.class, EntitlementSource.class,
                AuditChangeType.class, MenuKind.class, ActionMatchMode.class,
                MenuAccessMode.class, ScopeKind.class, IamAction.class, IamReasonCode.class,
                MemberFieldKey.class, ExportTaskStatus.class, IamActionOperation.class,
                AccountLookupPurpose.class, SelectionPurpose.class, SensitiveConfirmationKind.class
        }) {
            assertDoesNotThrow(() -> new CompositeEnumTypeHandler(type), type.getSimpleName());
        }
    }

    @Test
    void jsonStillUsesPublicLiteral() throws Exception {
        assertEquals("\"ACTIVE\"", mapper.writeValueAsString(MemberStatus.ACTIVE));
        assertEquals("\"PLATFORM\"", mapper.writeValueAsString(AuthorizationDomain.PLATFORM));
        assertEquals("\"ActionDenied\"", mapper.writeValueAsString(IamReasonCode.ACTION_DENIED));
        assertEquals("\"iam-tenant:field-policy:read\"", mapper.writeValueAsString(IamAction.TENANT_FIELD_POLICY_READ));
        assertEquals(IamAction.TENANT_FIELD_POLICY_READ, mapper.readValue("\"iam-tenant:field-policy:read\"", IamAction.class));
        assertEquals(MemberStatus.ACTIVE, MemberStatus.getEnum("ACTIVE"));
        assertEquals("displayName", mapper.readValue("\"displayName\"", MemberFieldKey.class).getValue());
    }
}
