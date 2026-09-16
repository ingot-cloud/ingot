package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证平台与租户身份结构隔离及平台选择器边界，不替代真实成员归属鉴权测试。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class IamIdentityContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void oneAccountCanHavePlatformAndMultipleTenantIdentities() throws Exception {
        AuthorizationContext platform = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "account", "pm");
        AuthorizationContext tenantA = new AuthorizationContext(AuthorizationDomain.TENANT, "a", "account", "ma");
        AuthorizationContext tenantB = new AuthorizationContext(AuthorizationDomain.TENANT, "b", "account", "mb");
        assertEquals(platform.accountId(), tenantA.accountId());
        assertEquals(platform.accountId(), tenantB.accountId());
        assertEquals(3, java.util.Set.of(platform, tenantA, tenantB).size());
        for (AuthorizationContext context : List.of(platform, tenantA, tenantB)) {
            assertEquals(context, mapper.readValue(mapper.writeValueAsString(context), AuthorizationContext.class));
        }
    }

    @Test
    void platformCannotCarryEvenAnEmptyTenantId() {
        for (String tenantId : List.of("tenant", "", " ")) {
            assertThrows(IllegalArgumentException.class,
                    () -> new AuthorizationContext(AuthorizationDomain.PLATFORM, tenantId, "account", "member"));
        }
    }

    @Test
    void requiresTenantAndMemberForTenantIdentityAndMemberForPlatformIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationContext(AuthorizationDomain.TENANT, null, "account", "member"));
        assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationContext(AuthorizationDomain.TENANT, " ", "account", "member"));
        for (AuthorizationDomain domain : AuthorizationDomain.values()) {
            String tenantId = domain == AuthorizationDomain.TENANT ? "tenant" : null;
            assertThrows(IllegalArgumentException.class,
                    () -> new AuthorizationContext(domain, tenantId, "account", null));
            assertThrows(IllegalArgumentException.class,
                    () -> new AuthorizationContext(domain, tenantId, "", "member"));
        }
        assertThrows(IllegalArgumentException.class,
                () -> new AuthorizationContext(null, null, "account", "member"));
    }

    @Test
    void rejectsAccountAsAssignmentSubject() {
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,
                () -> mapper.readValue("{\"type\":\"ACCOUNT\",\"id\":\"account\"}", SubjectRef.class));
    }

    @Test
    void platformSelectionCannotReferenceDepartments() {
        Selection selection = new Selection(List.of("member"), List.of(new DepartmentSelection("dept", true)));
        assertThrows(IllegalArgumentException.class,
                () -> selection.requireCompatibleDomain(AuthorizationDomain.PLATFORM));
        assertDoesNotThrow(() -> selection.requireCompatibleDomain(AuthorizationDomain.TENANT));
        assertThrows(IllegalArgumentException.class, () -> selection.requireCompatibleDomain(null));
    }

    @Test
    void normalizesEmptySelectionAndPreservesImmutableInput() throws Exception {
        Selection empty = mapper.readValue("{}", Selection.class);
        assertEquals("{\"members\":[],\"departments\":[]}", mapper.writeValueAsString(empty));
        assertDoesNotThrow(() -> empty.requireCompatibleDomain(AuthorizationDomain.PLATFORM));
        List<String> members = new ArrayList<>(List.of("member"));
        Selection selection = new Selection(members, null);
        members.clear();
        assertEquals(List.of("member"), selection.members());
        assertThrows(UnsupportedOperationException.class, () -> selection.members().clear());
    }

    @Test
    void validatesNestedSelectionIds() throws Exception {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Selection selection = mapper.readValue(
                    "{\"members\":[\"\"],\"departments\":[{\"id\":\"\"},null]}", Selection.class);
            assertEquals(3, factory.getValidator().validate(selection).size());
        }
    }
}
