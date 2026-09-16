package com.ingot.framework.commons.model.iam;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证 IAM 范围公共契约的 JSON 类型、输入校验与集合稳定性。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class IamScopeContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void roundTripsStringIdsBeyondJavascriptIntegerPrecision() throws Exception {
        String json = """
                {"actionId":"9007199254740993","scopes":[
                  {"kind":"MANAGED_DEPARTMENTS","parameterKey":"managedDepartments","includeDescendants":true}
                ]}
                """;
        ActionGrant grant = mapper.readValue(json, ActionGrant.class);
        assertEquals("9007199254740993", grant.actionId());
        assertEquals(ScopeKind.MANAGED_DEPARTMENTS, grant.scopes().getFirst().kind());
        assertEquals(mapper.readTree(json), mapper.valueToTree(grant));
    }

    @Test
    void rejectsUnknownScopeVocabulary() {
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,
                () -> mapper.readValue("{\"kind\":\"CUSTOM_SCRIPT\"}", ScopeExpression.class));
    }

    @Test
    void keepsMissingAndEmptyRequiredScopesDistinct() throws Exception {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ActionGrant missing = mapper.readValue("{\"actionId\":\"read\"}", ActionGrant.class);
            ActionGrant empty = mapper.readValue("{\"actionId\":\"read\",\"scopes\":[]}", ActionGrant.class);
            assertFalse(factory.getValidator().validate(missing).isEmpty());
            assertTrue(factory.getValidator().validate(empty).isEmpty());
            assertEquals("[]", mapper.valueToTree(empty).get("scopes").toString());
        }
    }

    @Test
    void validatesNestedScopeAndCollectionElements() throws Exception {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            ActionGrant invalid = mapper.readValue(
                    "{\"actionId\":\"read\",\"scopes\":[{},null]}", ActionGrant.class);
            assertEquals(2, factory.getValidator().validate(invalid).size());
            ScopeBinding binding = mapper.readValue(
                    "{\"kind\":\"DEPARTMENTS\",\"ids\":[\"\",null]}", ScopeBinding.class);
            assertEquals(2, factory.getValidator().validate(binding).size());
        }
    }

    @Test
    void normalizesOptionalRemoveScopesToArray() throws Exception {
        RoleDelta delta = mapper.readValue(
                "{\"actionId\":\"edit\",\"operation\":\"REMOVE\"}", RoleDelta.class);
        assertEquals(RoleDeltaOperation.REMOVE, delta.operation());
        assertEquals(List.of(), delta.scopes());
        assertTrue(mapper.valueToTree(delta).get("scopes").isArray());
    }

    @Test
    void doesNotAllowCallerToMutateValidatedCollections() {
        List<ScopeExpression> scopes = new ArrayList<>();
        scopes.add(new ScopeExpression(ScopeKind.SELF, null, null));
        ActionGrant grant = new ActionGrant("read", scopes);
        RoleDelta delta = new RoleDelta("read", RoleDeltaOperation.ADD, scopes);
        scopes.clear();
        assertEquals(1, grant.scopes().size());
        assertEquals(1, delta.scopes().size());
        assertThrows(UnsupportedOperationException.class, () -> grant.scopes().clear());
        assertThrows(UnsupportedOperationException.class, () -> delta.scopes().clear());

        List<String> ids = new ArrayList<>(List.of("9007199254740993"));
        ScopeBinding binding = new ScopeBinding(ScopeBindingKind.OBJECTS, ids);
        ids.clear();
        assertEquals(1, binding.ids().size());
        assertThrows(UnsupportedOperationException.class, () -> binding.ids().clear());
    }
}
