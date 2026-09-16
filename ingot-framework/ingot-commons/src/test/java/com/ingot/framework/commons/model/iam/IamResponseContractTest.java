package com.ingot.framework.commons.model.iam;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ingot.framework.commons.jackson.InModule;
import com.ingot.framework.commons.model.support.R;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>使用应用的 Jackson 模块验证响应字段投影、数字以及受限解释的可消费契约。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class IamResponseContractTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
            .registerModule(new InModule());

    private <T> T fixture(String name, TypeReference<T> type) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/iam/" + name + ".json")) {
            assertNotNull(input);
            return mapper.readValue(input, type);
        }
    }

    @Test
    void responseFixturesPassNestedValidation() throws Exception {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            for (Object value : List.of(fixture("bootstrap", new TypeReference<Bootstrap>() {}),
                    fixture("member-detail", new TypeReference<ResourceDetail<MemberRecord>>() {}),
                    fixture("member-page", new TypeReference<PageResponse<ResourceDetail<MemberRecord>>>() {}),
                    fixture("decision-restricted", new TypeReference<Decision>() {}),
                    fixture("preview-invalid", new TypeReference<Preview<EffectiveRole>>() {}),
                    fixture("upgrade-conflict", new TypeReference<UpgradePreview>() {}),
                    fixture("audit", new TypeReference<AuditEntry>() {}))) {
                assertTrue(factory.getValidator().validate(value).isEmpty(), value.getClass().getSimpleName());
            }
        }
    }

    @Test
    void hiddenFieldsAreOmittedAndMaskedValueIsRetained() throws Exception {
        var detail = fixture("member-detail", new TypeReference<ResourceDetail<MemberRecord>>() {});
        var json = mapper.valueToTree(detail);
        assertFalse(json.get("record").has("email"));
        assertEquals("138****0000", json.at("/record/phone").asText());
        assertEquals("HIDDEN", json.at("/fieldAccess/email/visibility").asText());
        assertTrue(json.get("version").isTextual());
        assertEquals("9007199254740993", json.at("/record/id").asText());
    }

    @Test
    void actualModuleKeepsCountsNumericWithoutChangingIdentifiers() throws Exception {
        var page = mapper.valueToTree(fixture("member-page", new TypeReference<PageResponse<ResourceDetail<MemberRecord>>>() {}));
        assertTrue(page.get("total").isIntegralNumber());
        assertTrue(page.get("page").isIntegralNumber());
        assertTrue(page.at("/items/0/record/id").isTextual());
        assertTrue(mapper.valueToTree(new UsageSummary(1L, 2L, false)).get("assignments").isIntegralNumber());
        assertTrue(mapper.valueToTree(new ImpactSummary(1L, 2L, 3L, false)).get("affectedMembers").isIntegralNumber());
        assertTrue(mapper.valueToTree(new GroupRecord("g", "组", null,
                new Selection(List.of(), List.of()), 1L)).get("visibleMemberCount").isIntegralNumber());
    }

    @Test
    void restrictedResponsesDoNotInventZeroCountsOrSourceIds() throws Exception {
        var decision = mapper.valueToTree(fixture("decision-restricted", new TypeReference<Decision>() {}));
        assertTrue(decision.get("sources").isEmpty());
        assertEquals("ActionDenied", decision.get("reasonCode").asText());
        var preview = mapper.valueToTree(fixture("preview-invalid", new TypeReference<Preview<EffectiveRole>>() {}));
        assertFalse(preview.has("effectiveResult"));
        assertFalse(preview.at("/impactSummary").has("affectedMembers"));
        assertTrue(preview.at("/impactSummary/restricted").asBoolean());
        assertFalse(preview.has("consistent"));
    }

    @Test
    void previewRejectsContradictorySuccessAndPageRejectsNegativeCounts() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var error = new ValidationIssue("role", IamReasonCode.INVALID_ARGUMENT, "无效角色");
            assertFalse(factory.getValidator().validate(new Preview<>("v1", true, List.of(error), List.of(),
                    new ImpactSummary(null, null, null, true), null)).isEmpty());
            assertFalse(factory.getValidator().validate(new PageResponse<>(List.of(), -1L, 0, 0)).isEmpty());
        }
    }

    @Test
    void existingEnvelopePreservesTypedDataAndStableErrorCodes() throws Exception {
        var bootstrap = fixture("bootstrap", new TypeReference<Bootstrap>() {});
        var success = mapper.valueToTree(new R<Bootstrap>().data(bootstrap));
        assertTrue(success.get("success").asBoolean());
        assertEquals("PLATFORM", success.at("/data/context/domain").asText());
        assertTrue(success.at("/data/actionCodes").isArray());
        assertTrue(success.at("/data/version").isTextual());
        var denied = mapper.valueToTree(new R<>(IamReasonCode.ACTION_DENIED));
        assertEquals("ActionDenied", denied.get("code").asText());
        assertFalse(denied.get("success").asBoolean());
        assertTrue(denied.get("data").isNull());
    }

    @Test
    void auditAcceptsOnlyDeclaredSafeDiffKeys() throws Exception {
        var audit = fixture("audit", new TypeReference<AuditEntry>() {});
        assertEquals(Map.of(AuditField.ROLE_REVISION, "rev-1"), audit.before());
        var json = mapper.valueToTree(audit);
        assertEquals("2026-09-13T00:00:00Z", json.get("timestamp").asText());
        assertThrows(com.fasterxml.jackson.databind.JsonMappingException.class,
                () -> mapper.readValue(mapper.writeValueAsString(audit).replace("ROLE_REVISION", "password"),
                        AuditEntry.class));
    }
}
