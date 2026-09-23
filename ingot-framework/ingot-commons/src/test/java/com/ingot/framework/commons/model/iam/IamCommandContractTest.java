package com.ingot.framework.commons.model.iam;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证管理请求、角色发布与升级命令的结构边界，不替代服务端归属、冲突求解或目录生成。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class IamCommandContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void commandFixturesDeserializeAndValidate() throws Exception {
        var fixtures = java.util.Map.of("role-create", RoleCreateInput.class, "role-publish", RolePublishInput.class,
                "role-upgrade", UpgradeInput.class, "tenant-create", TenantCreateInput.class);
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            for (var entry : fixtures.entrySet()) {
                try (var input = getClass().getResourceAsStream("/iam/" + entry.getKey() + ".json")) {
                    assertNotNull(input);
                    var value = mapper.readValue(input, entry.getValue());
                    assertTrue(factory.getValidator().validate(value).isEmpty(), entry.getKey());
                }
            }
        }
    }

    @Test
    void systemRolesAndMixedDefinitionsCannotBeCreated() {
        var complete = new RoleDefinitionDraft(List.of(new ActionGrant("read", List.of())), List.of(), List.of(), null);
        var mixed = new RoleDefinitionDraft(List.of(new ActionGrant("read", List.of())),
                List.of(new RoleDelta("edit", RoleDeltaOperation.REMOVE, List.of())), List.of(), null);
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertFalse(validator.validate(new RoleCreateInput("code", "name", null, null,
                    RoleKind.SYSTEM, null, complete)).isEmpty());
            assertFalse(validator.validate(new RoleCreateInput("code", "name", null, null,
                    RoleKind.SHARED, "base", complete)).isEmpty());
            assertFalse(validator.validate(new RoleCreateInput("code", "name", null, null,
                    RoleKind.TENANT_CUSTOM, "base", complete)).isEmpty());
            assertFalse(validator.validate(new RoleCreateInput("code", "name", null, null,
                    RoleKind.PLATFORM_CUSTOM, null, mixed)).isEmpty());
            assertTrue(validator.validate(new RoleCreateInput("code", "name", null, null,
                    RoleKind.TENANT_CUSTOM, "base",
                    new RoleDefinitionDraft(List.of(), List.of(), List.of(), new RoleMetadataOverrides("n", null, null))))
                    .isEmpty());
        }
    }

    @Test
    void upgradeResolutionMustMatchChoiceAndKeysStayUnique() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertFalse(validator.validate(new UpgradeResolution("k", UpgradeResolutionChoice.ACCEPT_BASE,
                    List.of())).isEmpty());
            assertFalse(validator.validate(new UpgradeResolution("k", UpgradeResolutionChoice.REPLACE_SCOPE, null))
                    .isEmpty());
            assertTrue(validator.validate(new UpgradeResolution("k", UpgradeResolutionChoice.REPLACE_SCOPE, List.of()))
                    .isEmpty());
            var resolution = new UpgradeResolution("k", UpgradeResolutionChoice.ACCEPT_BASE, null);
            assertFalse(validator.validate(new UpgradeInput("1", "rev", List.of(resolution, resolution), List.of()))
                    .isEmpty());
        }
    }

    @Test
    void memberDepartmentsRejectDuplicatesAndMultiplePrimaries() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var primary = new MemberDepartmentBinding("11", true);
            assertFalse(validator.validate(new MemberCreateInput("1", "n", null, List.of(primary, primary))).isEmpty());
            assertFalse(validator.validate(new MemberDepartmentInput("1",
                    List.of(primary, new MemberDepartmentBinding("12", true)))).isEmpty());
            assertTrue(validator.validate(new MemberCreateInput("1", "n", null, List.of())).isEmpty());
        }
    }

    @Test
    void platformApplicationsCannotBeMarkedBaseline() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(new ApplicationDraft("iam", AuthorizationDomain.PLATFORM,
                    "IAM", null, null, 0, true)).isEmpty());
            assertTrue(factory.getValidator().validate(new ApplicationDraft("iam", AuthorizationDomain.TENANT,
                    "IAM", null, null, 0, true)).isEmpty());
        }
    }

    @Test
    void actionCodesRejectWildcardsAndHiddenValidatorsStayOffWire() throws Exception {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertFalse(factory.getValidator().validate(new ActionDraft("res", "iam:*:read", "读")).isEmpty());
        }
        var json = mapper.valueToTree(new UpgradeInput("1", "rev", List.of(), List.of()));
        assertFalse(json.has("unique"));
        assertFalse(mapper.valueToTree(new RoleDefinitionDraft(List.of(), List.of(), List.of(), null)).has("customized"));
    }
}
