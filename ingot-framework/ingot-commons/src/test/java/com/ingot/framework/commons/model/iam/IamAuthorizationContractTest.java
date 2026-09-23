package com.ingot.framework.commons.model.iam;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.ingot.framework.commons.model.support.R;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证角色、分配、委派及字段的可消费 JSON 契约，并输出真实类型生成的 schemas。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class IamAuthorizationContractTest {
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private <T> T fixture(String name, Class<T> type) throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/iam/" + name + ".json")) {
            assertNotNull(input);
            return mapper.readValue(input, type);
        }
    }

    @Test
    void examplesPassNestedBeanValidationAndRetainStringIds() throws Exception {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            for (Object value : List.of(fixture("role-shared", RoleRevision.class),
                    fixture("role-delta", RoleRevision.class), fixture("assignment", AssignmentInput.class),
                    fixture("delegation", DelegationInput.class), fixture("field-readonly", FieldRule.class))) {
                assertTrue(factory.getValidator().validate(value).isEmpty(), value.getClass().getSimpleName());
            }
            RoleRevision role = fixture("role-shared", RoleRevision.class);
            assertTrue(mapper.valueToTree(role).get("id").isTextual());
            assertTrue(mapper.valueToTree(role).get("revision").isTextual());
        }
    }

    @Test
    void rejectEmptyOrReverseAssignmentIntervals() throws Exception {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            AssignmentInput input = fixture("assignment", AssignmentInput.class);
            for (var until : List.of(input.validFrom(), input.validFrom().minusSeconds(1))) {
                assertFalse(factory.getValidator().validate(new AssignmentInput(input.subject(), input.roleRevisionRef(),
                        input.scopeBindings(), input.validFrom(), until, input.delegationGrantId())).isEmpty());
            }
        }
    }

    @Test
    void delegationDurationIsIsoStringAndMustBePositive() throws Exception {
        DelegationInput input = fixture("delegation", DelegationInput.class);
        assertEquals("PT24H", mapper.valueToTree(input).get("maxAssignmentDuration").asText());
        assertEquals("2026-09-13T00:00:00Z", mapper.valueToTree(input).get("validFrom").asText());
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            for (Duration duration : List.of(Duration.ZERO, Duration.ofSeconds(-1))) {
                assertFalse(factory.getValidator().validate(new DelegationInput(input.administratorMemberId(),
                        input.allowedRoleRevisionRefs(), input.recipientSelection(), input.actionScopeCeilings(),
                        input.validFrom(), input.validUntil(), duration)).isEmpty());
            }
        }
    }

    @Test
    void fieldsCannotBeEditableUnlessFull() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            for (FieldVisibility visibility : List.of(FieldVisibility.HIDDEN, FieldVisibility.MASKED)) {
                assertFalse(factory.getValidator().validate(new FieldAccess(visibility, true)).isEmpty());
            }
            assertTrue(factory.getValidator().validate(new FieldAccess(FieldVisibility.FULL, false)).isEmpty());
        }
    }

    @Test
    void scopeAndDeltaRejectContradictoryShapes() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            for (Object value : List.of(new ScopeExpression(ScopeKind.ALL, "unexpected", null),
                    new ScopeExpression(ScopeKind.OBJECT_SET, "objects", true),
                    new ScopeExpression(ScopeKind.MANAGED_DEPARTMENTS, null, true),
                    new RoleDelta("read", RoleDeltaOperation.ADD, null),
                    new RoleDelta("read", RoleDeltaOperation.REMOVE,
                            List.of(new ScopeExpression(ScopeKind.ALL, null, null))))) {
                assertFalse(factory.getValidator().validate(value).isEmpty());
            }
        }
    }

    @Test
    void customizedRevisionCannotPersistBaseGrantCopiesOrDuplicateDeltas() throws Exception {
        RoleRevision input = fixture("role-delta", RoleRevision.class);
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            RoleRevision duplicate = new RoleRevision(input.id(), input.roleId(), input.revision(), input.kind(),
                    input.baseRevisionId(), input.grants(), List.of(input.deltas().getFirst(), input.deltas().getFirst()),
                    input.parameterDefinitions(), input.metadataOverrides());
            assertFalse(factory.getValidator().validate(duplicate).isEmpty());
            RoleRevision copied = new RoleRevision(input.id(), input.roleId(), input.revision(), input.kind(),
                    input.baseRevisionId(), fixture("role-shared", RoleRevision.class).grants(), input.deltas(),
                    input.parameterDefinitions(), input.metadataOverrides());
            assertFalse(factory.getValidator().validate(copied).isEmpty());
        }
    }

    @Test
    void validationMethodsDoNotBecomeResponseProperties() throws Exception {
        ObjectNode node = mapper.valueToTree(fixture("delegation", DelegationInput.class));
        assertFalse(node.has("validPeriod"));
        assertFalse(node.has("positiveDuration"));
        assertEquals(7, node.size());
    }

    @Test
    void reasonCodesKeepStableWireValuesAndUnavailableStatus() throws Exception {
        for (IamReasonCode reason : IamReasonCode.values()) {
            assertEquals(reason, mapper.readValue(mapper.writeValueAsString(reason), IamReasonCode.class));
        }
        assertEquals("\"ActionDenied\"", mapper.writeValueAsString(IamReasonCode.ACTION_DENIED));
        assertEquals(503, IamReasonCode.AUTHORIZATION_UNAVAILABLE.getHttpStatus());
        assertEquals(409, IamReasonCode.REVISION_CONFLICT.getHttpStatus());
    }

    @Test
    void exportSchemasFromPublicTypesAndCheckRequiredFields() throws Exception {
        Map<String, Schema> schemas = new TreeMap<>();
        for (Class<?> type : List.of(AuthorizationContext.class, SubjectRef.class, Selection.class,
                RoleRevision.class, AssignmentBatchInput.class, DelegationInput.class,
                FieldAccess.class, FieldRule.class, DirectoryRule.class,
                CreatedResource.class, ObjectCapability.class,
                Bootstrap.class, CurrentCapabilities.class, MemberRecord.class, TenantRecord.class,
                DepartmentRecord.class, GroupRecord.class, ApplicationRecord.class, ResourceRecord.class,
                ActionRecord.class, MenuRecord.class, EffectiveRole.class, Decision.class, UpgradePreview.class,
                AuditEntry.class, AssignmentRecord.class, DelegationRecord.class, PlanRecord.class, EntitlementRecord.class,
                DirectoryPolicyInput.class, FieldPolicyInput.class, PolicyPreviewInput.class, PolicyPreviewResult.class,
                DiagnoseInput.class, ConfigurationStatusInput.class, MemberStatusInput.class, VersionInput.class,
                GroupDraft.class, GroupUpdateInput.class, AudienceUpdateInput.class, AssignmentUpdateInput.class,
                DelegationUpdateInput.class, AssignmentPreviewResult.class, OwnerTransferInput.class,
                RoleCreateInput.class, RoleUpdateInput.class, RolePublishInput.class, RoleDefinitionDraft.class,
                UpgradePreviewInput.class, UpgradeInput.class, MemberCreateInput.class, MemberProfileInput.class,
                MemberDepartmentInput.class, TenantCreateInput.class, TenantUpdateInput.class, TenantSettingsInput.class,
                DepartmentDraft.class, DepartmentUpdateInput.class, ApplicationDraft.class, ApplicationUpdateInput.class,
                ResourceDraft.class, ResourceUpdateInput.class, ActionDraft.class, ActionUpdateInput.class,
                MenuDraft.class, MenuUpdateInput.class, PlanDraft.class, PlanUpdateInput.class,
                EntitlementReplaceInput.class, TenantPreviewResult.class, EntitlementPreviewResult.class,
                ReferenceImpactPreview.class, RoleSummary.class, ApplicationSummary.class,
                AccountCreateInput.class, AccountUpdateInput.class, AccountLockInput.class,
                AccountLookupInput.class, AccountRecord.class, AccountSecret.class,
                AccountSelfProfile.class, AccountSelfProfileInput.class, CurrentPasswordInput.class,
                ExportTask.class, SelectionPurpose.class, AccountLookupPurpose.class)) {
            schemas.putAll(ModelConverters.getInstance().readAll(type));
        }
        for (java.lang.reflect.Type type : List.of(
                new com.fasterxml.jackson.core.type.TypeReference<ResourceDetail<MemberRecord>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<PageResponse<ResourceDetail<MemberRecord>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<Preview<EffectiveRole>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<Bootstrap>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<Preview<PolicyPreviewResult>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<Preview<AssignmentPreviewResult>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<DirectoryPolicyDraft>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<FieldPolicyDraft>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<CreatedResource>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<CurrentCapabilities>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<Decision>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<MemberRecord>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<MemberRecord>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<Preview<EffectiveRole>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<Preview<UpgradePreview>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<Preview<TenantPreviewResult>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<Preview<EntitlementPreviewResult>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<Preview<ReferenceImpactPreview>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<TenantRecord>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<GroupRecord>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<DepartmentRecord>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<ApplicationRecord>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<ResourceRecord>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<ActionRecord>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<MenuRecord>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<PlanRecord>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<EntitlementRecord>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<RoleSummary>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<RoleRevision>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<AssignmentRecord>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<DelegationRecord>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<AudienceDraft>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<TenantRecord>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<GroupRecord>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<DepartmentRecord>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<ApplicationRecord>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<ResourceRecord>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<ActionRecord>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<MenuRecord>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<PlanRecord>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<EntitlementRecord>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<RoleSummary>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<RoleRevision>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<AssignmentRecord>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<DelegationRecord>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<AuditEntry>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ExportTask>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<AccountSelfProfile>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<AccountSecret>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<ResourceDetail<AccountRecord>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<PageResponse<ResourceDetail<AccountRecord>>>>() {}.getType(),
                new com.fasterxml.jackson.core.type.TypeReference<R<Void>>() {}.getType())) {
            schemas.putAll(ModelConverters.getInstance().resolveAsResolvedSchema(
                    new io.swagger.v3.core.converter.AnnotatedType(type)).referencedSchemas);
        }
        Schema<?> page = schemas.get("PageResponseResourceDetailMemberRecord");
        assertTrue(page.getRequired().containsAll(List.of("items", "total", "page", "pageSize")));
        assertEquals("integer", ((Schema<?>) page.getProperties().get("total")).getType());
        assertEquals("#/components/schemas/ResourceDetailMemberRecord",
                ((Schema<?>) page.getProperties().get("items")).getItems().get$ref());
        assertEquals("#/components/schemas/MemberRecord",
                ((Schema<?>) schemas.get("ResourceDetailMemberRecord").getProperties().get("record")).get$ref());
        assertEquals("#/components/schemas/EffectiveRole",
                ((Schema<?>) schemas.get("PreviewEffectiveRole").getProperties().get("effectiveResult")).get$ref());
        assertFalse(schemas.get("PreviewEffectiveRole").getProperties().containsKey("consistent"));
        Schema<?> assignment = schemas.get("AssignmentInput");
        assertNotNull(assignment);
        assertTrue(assignment.getRequired().containsAll(List.of("subject", "roleRevisionRef", "scopeBindings")));
        assertFalse(schemas.get("DelegationInput").getProperties().containsKey("positiveDuration"));
        Schema<?> duration = (Schema<?>) schemas.get("DelegationInput").getProperties().get("maxAssignmentDuration");
        assertEquals("string", duration.getType());
        assertEquals("duration", duration.getFormat());
        assertTrue(schemas.get("RoleCreateInput").getRequired().containsAll(List.of("code", "name", "kind", "definition")));
        assertTrue(schemas.get("UpgradeInput").getRequired().containsAll(
                List.of("expectedVersion", "newBaseRevisionId", "resolutions", "assignmentIds")));
        assertTrue(schemas.get("TenantCreateInput").getRequired().containsAll(List.of("name", "ownerAccountId")));
        assertFalse(schemas.get("RoleDefinitionDraft").getProperties().containsKey("customized"));
        assertNotNull(schemas.get("RPreviewTenantPreviewResult"));
        assertNotNull(schemas.get("RPreviewUpgradePreview"));
        assertTrue(schemas.get("ExportTask").getRequired().containsAll(List.of("id", "status", "version", "expiresAt")));
        assertFalse(schemas.get("ExportTask").getRequired().contains("failureCode"));
        assertTrue(schemas.get("AccountLookupInput").getRequired().contains("purpose"));
        assertFalse(schemas.get("AccountLookupInput").getProperties().containsKey("exactlyOneCriterion"));
        assertFalse(schemas.get("AccountLookupInput").getProperties().containsKey("isExactlyOneCriterion"));
        assertNotNull(schemas.get("RExportTask"));
        assertNotNull(schemas.get("RAccountSelfProfile"));
        assertNotNull(schemas.get("RAccountSecret"));
        assertNotNull(schemas.get("RPageResponseResourceDetailAccountRecord"));
        Schema<?> exportStatus = (Schema<?>) schemas.get("ExportTask").getProperties().get("status");
        assertEquals(List.of("PENDING", "RUNNING", "SUCCEEDED", "FAILED", "EXPIRED"), exportStatus.getEnum());
        Schema<?> lookupPurpose = (Schema<?>) schemas.get("AccountLookupInput").getProperties().get("purpose");
        assertEquals(List.of("MEMBER_CREATE", "ACCOUNT_MANAGE"), lookupPurpose.getEnum());
        for (var reference : Json.mapper().valueToTree(schemas).findValues("$ref")) {
            String prefix = "#/components/schemas/";
            assertTrue(reference.asText().startsWith(prefix));
            assertTrue(schemas.containsKey(reference.asText().substring(prefix.length())), reference.asText());
        }
        Path output = Path.of("build", "iam-contract", "schemas.json");
        Files.createDirectories(output.getParent());
        Files.writeString(output, Json.pretty(Map.of("components", Map.of("schemas", schemas))));
    }
}
