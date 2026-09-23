package com.ingot.cloud.iam.policy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldPolicyDraft;
import com.ingot.framework.commons.model.iam.FieldProjection;
import com.ingot.framework.commons.model.iam.FieldRule;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberFieldKey;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.PolicyScenario;
import com.ingot.framework.commons.model.iam.ScopeExpression;
import com.ingot.framework.commons.model.iam.ScopeKind;
import com.ingot.framework.commons.model.iam.Selection;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证默认脱敏、更严规则合并、写入拒绝与原值筛选拒绝。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FieldAccessEvaluatorTest {
    private JdbcTemplate jdbc;
    private FieldAccessEvaluator evaluator;

    @BeforeAll
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY, tenant_id BIGINT, parent_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, member_id BIGINT, department_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_policy_selector_member(tenant_id BIGINT, selector_id BIGINT, member_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE iam_policy_selector_department(tenant_id BIGINT, selector_id BIGINT,
                  department_id BIGINT, include_descendants BOOLEAN)
                """);
        jdbc.execute("""
                CREATE TABLE iam_field_rule(id BIGINT, tenant_id BIGINT, scenario VARCHAR(32), field_key VARCHAR(64),
                  viewer_selector_id BIGINT, target_scope VARCHAR(512), scope_bindings VARCHAR(512),
                  visibility VARCHAR(16), editable BOOLEAN)
                """);
        jdbc.execute("CREATE TABLE iam_default_policy_revision(id BIGINT PRIMARY KEY, kind VARCHAR(16),"
                + " revision BIGINT, definition VARCHAR(1024))");
        jdbc.execute("CREATE TABLE iam_field_policy(tenant_id BIGINT PRIMARY KEY, default_revision_id BIGINT,"
                + " default_kind VARCHAR(16), version BIGINT)");
        jdbc.execute("CREATE TABLE iam_directory_policy(tenant_id BIGINT PRIMARY KEY, default_revision_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_directory_rule(id BIGINT)");
        jdbc.execute("CREATE TABLE iam_policy_selector(id BIGINT PRIMARY KEY, tenant_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, tenant_id BIGINT, status VARCHAR(16))");
        jdbc.update("""
                INSERT INTO iam_default_policy_revision VALUES (22,'FIELD',1,
                  '{"fields":{"displayName":{"visibility":"FULL","editable":true},"avatar":{"visibility":"FULL","editable":true},"phone":{"visibility":"MASKED","editable":false},"email":{"visibility":"MASKED","editable":false}}}')
                """);
        jdbc.update("INSERT INTO iam_department VALUES (11,10,NULL),(12,10,11)");
        jdbc.update("INSERT INTO iam_member_department VALUES (10,101,11),(10,102,12)");
        evaluator = com.ingot.cloud.iam.persistence.IamMybatisTestAccess.fields(dataSource);
    }

    @AfterEach
    void clearRules() {
        jdbc.update("DELETE FROM iam_field_rule");
        jdbc.update("DELETE FROM iam_policy_selector_member");
        jdbc.update("DELETE FROM iam_default_policy_revision WHERE id <> 22");
    }

    @Test
    void phoneAndEmailDefaultToMaskedAndNotEditable() {
        FieldAccess phone = evaluator.access(10, 101, 102, PolicyScenario.MANAGEMENT, MemberFieldKey.VALUE_PHONE);
        assertEquals(FieldVisibility.MASKED, phone.visibility());
        assertFalse(phone.editable());
        MemberRecord projected = evaluator.project(new MemberRecord("102", "成员", "a", "13800000000", "a@b.c",
                null, MemberStatus.ACTIVE, List.of()), evaluator.memberAccess(10, 101, 102, PolicyScenario.MANAGEMENT));
        assertEquals(FieldProjection.MASKED_PLACEHOLDER, projected.phone());
        assertEquals(FieldProjection.MASKED_PLACEHOLDER, projected.email());
        assertEquals("成员", projected.displayName());
    }

    @Test
    void hiddenRuleWinsAndRejectsWriteAndOriginalLookup() {
        insertRule(1, 101, "[{\"kind\":\"ALL\"}]", FieldVisibility.HIDDEN, false);
        insertRule(2, 101, "[{\"kind\":\"ALL\"}]", FieldVisibility.MASKED, false);
        FieldAccess phone = evaluator.access(10, 101, 102, PolicyScenario.MANAGEMENT, MemberFieldKey.VALUE_PHONE);
        assertEquals(FieldVisibility.HIDDEN, phone.visibility());
        MemberRecord projected = evaluator.project(new MemberRecord("102", "成员", null, "13800000000", null,
                null, MemberStatus.ACTIVE, List.of()), Map.of(MemberFieldKey.VALUE_PHONE, phone,
                MemberFieldKey.VALUE_DISPLAY_NAME, new FieldAccess(FieldVisibility.FULL, true),
                MemberFieldKey.VALUE_AVATAR, new FieldAccess(FieldVisibility.FULL, true),
                MemberFieldKey.VALUE_EMAIL, new FieldAccess(FieldVisibility.MASKED, false)));
        assertNull(projected.phone());
        BizException write = assertThrows(BizException.class,
                () -> evaluator.requireWritable(10, 101, 102, MemberFieldKey.VALUE_PHONE, "13900000000"));
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(), write.getCode());
        BizException lookup = assertThrows(BizException.class,
                () -> evaluator.requireOriginalLookup(10, 101, MemberFieldKey.VALUE_PHONE, "13800000000"));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), lookup.getCode());
    }

    @Test
    void fullEditableRuleAllowsWriteAndOriginalLookupWhenCeilingOpen() {
        insertRule(3, 101, "[{\"kind\":\"ALL\"}]", FieldVisibility.FULL, true);
        FieldAccess phone = evaluator.access(10, 101, 102, PolicyScenario.MANAGEMENT, MemberFieldKey.VALUE_PHONE);
        assertEquals(FieldVisibility.FULL, phone.visibility());
        assertTrue(phone.editable());
        evaluator.requireWritable(10, 101, 102, MemberFieldKey.VALUE_PHONE, "13900000000");
        evaluator.requireOriginalLookup(10, 101, MemberFieldKey.VALUE_PHONE, "13800000000");
        BizException placeholder = assertThrows(BizException.class,
                () -> evaluator.requireWritable(10, 101, 102, MemberFieldKey.VALUE_PHONE,
                        FieldProjection.MASKED_PLACEHOLDER));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), placeholder.getCode());
    }

    @Test
    void originalLookupRejectsWhenOnlySelfIsFullyVisible() {
        insertRule(5, 101, "[{\"kind\":\"SELF\"}]", FieldVisibility.FULL, true);
        FieldAccess self = evaluator.access(10, 101, 101, PolicyScenario.MANAGEMENT, MemberFieldKey.VALUE_PHONE);
        assertEquals(FieldVisibility.FULL, self.visibility());
        FieldAccess other = evaluator.access(10, 101, 102, PolicyScenario.MANAGEMENT, MemberFieldKey.VALUE_PHONE);
        assertEquals(FieldVisibility.MASKED, other.visibility());
        BizException lookup = assertThrows(BizException.class,
                () -> evaluator.requireOriginalLookup(10, 101, MemberFieldKey.VALUE_PHONE, "13800000000"));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), lookup.getCode());
    }

    @Test
    void platformCeilingCapsTenantFullRule() {
        jdbc.update("""
                INSERT INTO iam_default_policy_revision VALUES (23,'FIELD',2,
                  '{"fields":{"phone":{"visibility":"MASKED","editable":false}},"ceiling":{"phone":{"visibility":"MASKED","editable":false}}}')
                """);
        insertRule(6, 101, "[{\"kind\":\"ALL\"}]", FieldVisibility.FULL, true);
        FieldAccess phone = evaluator.access(10, 101, 102, PolicyScenario.MANAGEMENT, MemberFieldKey.VALUE_PHONE);
        assertEquals(FieldVisibility.MASKED, phone.visibility());
        assertFalse(phone.editable());
        BizException lookup = assertThrows(BizException.class,
                () -> evaluator.requireOriginalLookup(10, 101, MemberFieldKey.VALUE_PHONE, "13800000000"));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), lookup.getCode());
    }

    @Test
    void draftSnapshotParticipatesInPreviewWithoutWritingRules() {
        FieldPolicyDraft draft = new FieldPolicyDraft("22", List.of(
                new FieldRule(PolicyScenario.DIRECTORY, MemberFieldKey.VALUE_PHONE,
                        new Selection(List.of("101"), List.of()),
                        List.of(new ScopeExpression(ScopeKind.ALL, null, null)), Map.of(),
                        FieldVisibility.HIDDEN, false)));
        FieldPolicySnapshot snapshot = evaluator.snapshot(10, PolicyScenario.DIRECTORY, draft);
        assertEquals(FieldVisibility.HIDDEN,
                evaluator.access(snapshot, 101, 102, MemberFieldKey.VALUE_PHONE).visibility());
        assertEquals(FieldVisibility.MASKED,
                evaluator.access(10, 101, 102, PolicyScenario.DIRECTORY, MemberFieldKey.VALUE_PHONE).visibility());
    }

    @Test
    void unmatchedViewerDoesNotApplyRule() {
        insertRule(4, 999, "[{\"kind\":\"ALL\"}]", FieldVisibility.FULL, true);
        FieldAccess phone = evaluator.access(10, 101, 102, PolicyScenario.MANAGEMENT, MemberFieldKey.VALUE_PHONE);
        assertEquals(FieldVisibility.MASKED, phone.visibility());
        assertFalse(phone.editable());
    }

    private void insertRule(long id, long viewerMemberId, String targetScope, FieldVisibility visibility,
                            boolean editable) {
        jdbc.update("INSERT INTO iam_policy_selector_member VALUES (10,?,?)", id, viewerMemberId);
        jdbc.update("""
                INSERT INTO iam_field_rule VALUES (?,?,?,?,?,?,'{}',?,?)
                """, id, 10, PolicyScenario.MANAGEMENT.name(), MemberFieldKey.VALUE_PHONE, id, targetScope,
                visibility.name(), editable);
    }
}
