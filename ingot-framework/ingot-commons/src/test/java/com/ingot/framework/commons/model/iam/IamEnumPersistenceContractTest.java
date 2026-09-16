package com.ingot.framework.commons.model.iam;

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
                AuditChangeType.class, DirectoryDefaultScope.class, MenuKind.class, ActionMatchMode.class,
                MenuAccessMode.class, ScopeKind.class, PolicyEffect.class, IamAction.class, IamReasonCode.class,
                MemberFieldKey.class, ExportTaskStatus.class, IamActionOperation.class,
                AccountLookupPurpose.class, SelectionPurpose.class
        }) {
            assertDoesNotThrow(() -> new CompositeEnumTypeHandler(type), type.getSimpleName());
        }
    }

    @Test
    void jsonStillUsesPublicLiteral() throws Exception {
        assertEquals("\"ACTIVE\"", mapper.writeValueAsString(MemberStatus.ACTIVE));
        assertEquals("\"PLATFORM\"", mapper.writeValueAsString(AuthorizationDomain.PLATFORM));
        assertEquals("\"ActionDenied\"", mapper.writeValueAsString(IamReasonCode.ACTION_DENIED));
        assertEquals("\"iam-tenant:directory:read\"", mapper.writeValueAsString(IamAction.TENANT_DIRECTORY_READ));
        assertEquals(IamAction.TENANT_DIRECTORY_READ, mapper.readValue("\"iam-tenant:directory:read\"", IamAction.class));
        assertEquals(MemberStatus.ACTIVE, MemberStatus.getEnum("ACTIVE"));
        assertEquals("displayName", mapper.readValue("\"displayName\"", MemberFieldKey.class).getValue());
    }
}
