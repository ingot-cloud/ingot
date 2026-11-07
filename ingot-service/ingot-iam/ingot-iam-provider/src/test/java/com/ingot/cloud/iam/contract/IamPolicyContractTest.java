package com.ingot.cloud.iam.contract;

import com.ingot.cloud.iam.application.catalog.model.AudienceDraft;
import com.ingot.cloud.iam.application.diagnosis.model.DiagnoseInput;
import com.ingot.cloud.iam.application.policy.model.FieldPolicyDraft;
import com.ingot.cloud.iam.application.policy.model.FieldPolicyInput;
import com.ingot.cloud.iam.application.policy.model.FieldRule;
import com.ingot.cloud.iam.domain.catalog.AudienceKind;
import com.ingot.cloud.iam.domain.organization.MemberStatus;
import com.ingot.cloud.iam.domain.organization.MemberStatusInput;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.Selection;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证字段策略草稿、版本命令与身份选择的结构边界，不替代服务端归属及授权求值。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class IamPolicyContractTest {
    private final Selection empty = new Selection(List.of(), List.of());

    @Test
    void policyFixturesDeserializeAndValidate() throws Exception {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            try (var input = getClass().getResourceAsStream("/iam/field-policy.json")) {
                assertNotNull(input);
                var value = new ObjectMapper().readValue(input, FieldPolicyInput.class);
                assertTrue(factory.getValidator().validate(value).isEmpty());
            }
        }
    }

    @Test
    void policyReplacementRequiresVersionAndNestedRules() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertFalse(validator.validate(new FieldPolicyInput("v1",
                    new FieldPolicyDraft("default-2", null))).isEmpty());
            assertFalse(validator.validate(new FieldPolicyInput("v1", new FieldPolicyDraft("default-2",
                    List.of(new FieldRule(" ", empty, List.of(),
                            java.util.Map.of(), FieldVisibility.MASKED))))).isEmpty());
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
    void fieldDraftCopiesRules() {
        var rules = new java.util.ArrayList<FieldRule>();
        var draft = new FieldPolicyDraft("default-2", rules);
        rules.add(new FieldRule("phone", empty, List.of(), java.util.Map.of(),
                FieldVisibility.MASKED));
        assertTrue(draft.rules().isEmpty());
        var json = new ObjectMapper().valueToTree(draft);
        assertFalse(json.has("expectedVersion"));
    }
}
