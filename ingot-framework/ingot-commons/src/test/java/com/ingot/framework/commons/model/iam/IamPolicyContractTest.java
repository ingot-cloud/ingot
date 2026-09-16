package com.ingot.framework.commons.model.iam;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证策略草稿、版本命令与身份选择的结构边界，不替代服务端归属及授权求值。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class IamPolicyContractTest {
    private final Selection empty = new Selection(List.of(), List.of());

    @Test
    void policyFixturesDeserializeAndValidate() throws Exception {
        var fixtures = java.util.Map.of("directory-policy", DirectoryPolicyInput.class,
                "field-policy", FieldPolicyInput.class, "policy-preview", PolicyPreviewInput.class);
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            for (var entry : fixtures.entrySet()) {
                try (var input = getClass().getResourceAsStream("/iam/" + entry.getKey() + ".json")) {
                    assertNotNull(input);
                    var value = new ObjectMapper().readValue(input, entry.getValue());
                    assertTrue(factory.getValidator().validate(value).isEmpty());
                }
            }
        }
    }

    @Test
    void defaultSelectionOnlyBelongsToSelectedScope() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (DirectoryDefaultScope scope : DirectoryDefaultScope.values()) {
                assertTrue(validator.validate(new DirectoryDefault(scope,
                        scope == DirectoryDefaultScope.SELECTED ? empty : null)).isEmpty());
                assertFalse(validator.validate(new DirectoryDefault(scope,
                        scope == DirectoryDefaultScope.SELECTED ? null : empty)).isEmpty());
            }
        }
    }

    @Test
    void policyKindMustSelectExactlyOneDraft() {
        var directory = new DirectoryPolicyDraft("default-1", null, List.of());
        var field = new FieldPolicyDraft("default-2", List.of());
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new PolicyDraft(DefaultPolicyKind.DIRECTORY, directory, null)).isEmpty());
            assertTrue(validator.validate(new PolicyDraft(DefaultPolicyKind.FIELD, null, field)).isEmpty());
            for (var kind : DefaultPolicyKind.values()) {
                assertFalse(validator.validate(new PolicyDraft(kind, directory, field)).isEmpty());
                assertFalse(validator.validate(new PolicyDraft(kind, null, null)).isEmpty());
            }
        }
    }

    @Test
    void policyReplacementRequiresVersionAndNestedRules() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new DirectoryPolicyInput("v1",
                    new DirectoryPolicyDraft("default-1", null, List.of()))).isEmpty());
            assertFalse(validator.validate(new DirectoryPolicyInput(" ",
                    new DirectoryPolicyDraft("default-1", null, List.of()))).isEmpty());
            assertFalse(validator.validate(new FieldPolicyInput("v1",
                    new FieldPolicyDraft("default-2", null))).isEmpty());
            assertFalse(validator.validate(new FieldPolicyInput("v1", new FieldPolicyDraft("default-2",
                    List.of(new FieldRule(PolicyScenario.MANAGEMENT, "phone", empty, List.of(),
                            java.util.Map.of(), FieldVisibility.MASKED, true))))).isEmpty());
        }
    }

    @Test
    void diagnosisRejectsAmbiguousAndEmptyIdentities() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (var input : List.of(new DiagnoseInput("m1", null, "app", "action", null),
                    new DiagnoseInput(null, "a1", "app", "action", null))) {
                assertTrue(validator.validate(input).isEmpty());
            }
            for (var input : List.of(new DiagnoseInput("m1", "a1", "app", "action", null),
                    new DiagnoseInput(null, null, "app", "action", null),
                    new DiagnoseInput(" ", null, "app", "action", null))) {
                assertFalse(validator.validate(input).isEmpty());
            }
        }
    }

    @Test
    void statusPatchCannotRemoveMember() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            for (MemberStatus status : MemberStatus.values()) {
                assertEquals(status != MemberStatus.REMOVED,
                        factory.getValidator().validate(new MemberStatusInput(status, "v1")).isEmpty());
            }
        }
    }

    @Test
    void allAudienceRejectsHiddenSelectionAndGroups() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new AudienceDraft(AudienceKind.ALL, null, List.of())).isEmpty());
            assertFalse(validator.validate(new AudienceDraft(AudienceKind.ALL, empty, List.of())).isEmpty());
            assertFalse(validator.validate(new AudienceDraft(AudienceKind.ALL, null, List.of("group"))).isEmpty());
            assertTrue(validator.validate(new AudienceDraft(AudienceKind.SELECTED, empty, List.of("group"))).isEmpty());
        }
    }

    @Test
    void draftCopiesCollectionsAndDoesNotExposeValidatorProperties() {
        var rules = new java.util.ArrayList<DirectoryRule>();
        var draft = new DirectoryPolicyDraft("default-1", null, rules);
        rules.add(new DirectoryRule(PolicyEffect.DENY, empty, empty));
        assertTrue(draft.rules().isEmpty());
        var json = new ObjectMapper().valueToTree(new PolicyDraft(DefaultPolicyKind.DIRECTORY, draft, null));
        assertFalse(json.has("kindCompatible"));
        assertFalse(json.get("directory").has("expectedVersion"));
    }
}
