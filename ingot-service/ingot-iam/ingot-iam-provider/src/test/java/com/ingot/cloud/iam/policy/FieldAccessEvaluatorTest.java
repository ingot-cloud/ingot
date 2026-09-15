package com.ingot.cloud.iam.policy;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.FieldAccess;
import com.ingot.framework.commons.model.iam.FieldProjection;
import com.ingot.framework.commons.model.iam.FieldVisibility;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberFieldKey;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.PolicyScenario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class FieldAccessEvaluatorTest {
    private JdbcTemplate jdbc;
    private FieldAccessEvaluator evaluator;

    @BeforeEach
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
        jdbc.update("INSERT INTO iam_department VALUES (11,10,NULL),(12,10,11)");
        jdbc.update("INSERT INTO iam_member_department VALUES (10,101,11),(10,102,12)");
        evaluator = new FieldAccessEvaluator(dataSource);
    }

    @Test
    void phoneAndEmailDefaultToMaskedAndNotEditable() {
        FieldAccess phone = evaluator.access(10, 101, 102, PolicyScenario.MANAGEMENT, MemberFieldKey.VALUE_PHONE);
        assertEquals(FieldVisibility.MASKED, phone.visibility());
        assertFalse(phone.editable());
        MemberRecord projected = evaluator.project(new MemberRecord("102", "成员", "a", "13800000000", "a@b.c",
                MemberStatus.ACTIVE, List.of()), evaluator.memberAccess(10, 101, 102, PolicyScenario.MANAGEMENT));
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
                MemberStatus.ACTIVE, List.of()), Map.of(MemberFieldKey.VALUE_PHONE, phone,
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
    void fullEditableRuleAllowsWriteAndRejectsMaskedPlaceholder() {
        insertRule(3, 101, "[{\"kind\":\"ALL\"}]", FieldVisibility.FULL, true);
        FieldAccess phone = evaluator.access(10, 101, 102, PolicyScenario.MANAGEMENT, MemberFieldKey.VALUE_PHONE);
        assertEquals(FieldVisibility.FULL, phone.visibility());
        assertTrue(phone.editable());
        evaluator.requireWritable(10, 101, 102, MemberFieldKey.VALUE_PHONE, "13900000000");
        BizException placeholder = assertThrows(BizException.class,
                () -> evaluator.requireWritable(10, 101, 102, MemberFieldKey.VALUE_PHONE,
                        FieldProjection.MASKED_PLACEHOLDER));
        assertEquals(IamReasonCode.INVALID_ARGUMENT.getCode(), placeholder.getCode());
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
