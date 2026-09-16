package com.ingot.cloud.iam.policy;

import java.util.List;
import java.util.UUID;

import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.framework.commons.model.iam.DirectoryDefault;
import com.ingot.framework.commons.model.iam.DirectoryDefaultScope;
import com.ingot.framework.commons.model.iam.DirectoryPolicyDraft;
import com.ingot.framework.commons.model.iam.DirectoryRule;
import com.ingot.framework.commons.model.iam.PolicyEffect;
import com.ingot.framework.commons.model.iam.Selection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证通讯录允许并集替代默认、禁止后恢复本人，以及部门树不泄露隐藏分支。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DirectoryVisibilityEvaluatorTest {
    private JdbcTemplate jdbc;
    private DirectoryVisibilityEvaluator evaluator;
    private PolicyWriteRepository policies;

    @BeforeAll
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, tenant_id BIGINT, display_name VARCHAR(32),"
                + " avatar VARCHAR(32), phone VARCHAR(32), email VARCHAR(64), status VARCHAR(16), version BIGINT)");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY, tenant_id BIGINT, parent_id BIGINT,"
                + " name VARCHAR(32), sort_order INT, version BIGINT)");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, member_id BIGINT, department_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_default_policy_revision(id BIGINT PRIMARY KEY, kind VARCHAR(16),"
                + " revision BIGINT, definition VARCHAR(512))");
        jdbc.execute("CREATE TABLE iam_directory_policy(tenant_id BIGINT PRIMARY KEY, default_revision_id BIGINT,"
                + " default_kind VARCHAR(16), default_scope VARCHAR(16), default_selector_id BIGINT, version BIGINT)");
        jdbc.execute("CREATE TABLE iam_directory_rule(id BIGINT, tenant_id BIGINT, effect VARCHAR(16),"
                + " viewer_selector_id BIGINT, target_selector_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_policy_selector(id BIGINT PRIMARY KEY, tenant_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_policy_selector_member(tenant_id BIGINT, selector_id BIGINT, member_id BIGINT)");
        jdbc.execute("""
                CREATE TABLE iam_policy_selector_department(tenant_id BIGINT, selector_id BIGINT,
                  department_id BIGINT, include_descendants BOOLEAN)
                """);
        jdbc.execute("CREATE TABLE iam_field_policy(tenant_id BIGINT PRIMARY KEY, default_revision_id BIGINT,"
                + " default_kind VARCHAR(16), version BIGINT)");
        jdbc.execute("""
                CREATE TABLE iam_field_rule(id BIGINT, tenant_id BIGINT, scenario VARCHAR(32), field_key VARCHAR(64),
                  viewer_selector_id BIGINT, target_scope VARCHAR(512), scope_bindings VARCHAR(512),
                  visibility VARCHAR(16), editable BOOLEAN)
                """);
        jdbc.update("INSERT INTO iam_default_policy_revision VALUES (21,'DIRECTORY',1,'{\"scope\":\"ALL\"}')");
        jdbc.update("INSERT INTO iam_tenant_member VALUES (101,10,'查看者',NULL,NULL,NULL,'ACTIVE',0),"
                + "(102,10,'可见',NULL,NULL,NULL,'ACTIVE',0),(103,10,'隐藏',NULL,NULL,NULL,'ACTIVE',0),"
                + "(104,10,'已移出',NULL,NULL,NULL,'REMOVED',0)");
        jdbc.update("INSERT INTO iam_department VALUES (11,10,NULL,'总部',1,0),(12,10,11,'可见组',2,0),"
                + "(13,10,11,'隐藏组',3,0)");
        jdbc.update("INSERT INTO iam_member_department VALUES (10,101,12),(10,102,12),(10,103,13)");
        evaluator = IamMybatisTestAccess.directory(dataSource);
        policies = IamMybatisTestAccess.policies(dataSource);
    }

    @Test
    void defaultRevisionIsWholeOrganizationNotSelf() {
        DirectoryVisibility visibility = evaluator.evaluate(10, 101, draft(null, List.of()));
        assertTrue(visibility.coversAll());
        assertTrue(visibility.contains(102));
        assertTrue(visibility.contains(103));
        var page = policies.pageVisibleMembers(10, List.of(visibility), null, null, 1, 20);
        assertEquals(3, page.getTotal());
    }

    @Test
    void matchingAllowReplacesDefaultInsteadOfAppending() {
        DirectoryVisibility visibility = evaluator.evaluate(10, 101, draft(null, List.of(
                rule(PolicyEffect.ALLOW, 101, 102))));
        assertFalse(visibility.coversAll());
        assertTrue(visibility.contains(101));
        assertTrue(visibility.contains(102));
        assertFalse(visibility.contains(103));
        var page = policies.pageVisibleMembers(10, List.of(visibility), null, null, 1, 20);
        assertEquals(2, page.getTotal());
        assertEquals(101L, page.getRecords().getFirst().getId().longValueExact());
        assertEquals(102L, page.getRecords().get(1).getId().longValueExact());
    }

    @Test
    void denySubtractsAfterAllowRegardlessOfRuleOrderAndRestoresSelf() {
        DirectoryVisibility denyFirst = evaluator.evaluate(10, 101, draft(null, List.of(
                rule(PolicyEffect.DENY, 101, 102), rule(PolicyEffect.ALLOW, 101, 102, 103))));
        DirectoryVisibility allowFirst = evaluator.evaluate(10, 101, draft(null, List.of(
                rule(PolicyEffect.ALLOW, 101, 102, 103), rule(PolicyEffect.DENY, 101, 102))));
        assertEquals(denyFirst.included(), allowFirst.included());
        assertTrue(denyFirst.contains(101));
        assertFalse(denyFirst.contains(102));
        assertTrue(denyFirst.contains(103));
        DirectoryVisibility deniedSelf = evaluator.evaluate(10, 101, draft(
                new DirectoryDefault(DirectoryDefaultScope.ALL, null),
                List.of(rule(PolicyEffect.DENY, 101, 101, 102))));
        assertTrue(deniedSelf.coversAll());
        assertTrue(deniedSelf.contains(101));
        assertFalse(deniedSelf.contains(102));
        assertTrue(policies.visibleMember(10, deniedSelf, 101));
        assertFalse(policies.visibleMember(10, deniedSelf, 102));
    }

    @Test
    void departmentTreeKeepsAncestorsAsNavigationOnly() {
        DirectoryVisibility visibility = evaluator.evaluate(10, 101, draft(null, List.of(
                rule(PolicyEffect.ALLOW, 101, 102))));
        var page = policies.pageVisibleDepartments(10, List.of(visibility), 1, 20);
        assertEquals(2, page.getTotal());
        DirectoryDepartmentNode root = page.getRecords().getFirst();
        DirectoryDepartmentNode leaf = page.getRecords().get(1);
        assertEquals(11L, root.row().getId().longValueExact());
        assertTrue(root.navigationOnly());
        assertEquals(12L, leaf.row().getId().longValueExact());
        assertFalse(leaf.navigationOnly());
    }

    private DirectoryPolicyDraft draft(DirectoryDefault override, List<DirectoryRule> rules) {
        return new DirectoryPolicyDraft("21", override, rules);
    }

    private DirectoryRule rule(PolicyEffect effect, long viewerId, long... targetIds) {
        List<String> targets = new java.util.ArrayList<>();
        for (long targetId : targetIds) {
            targets.add(Long.toString(targetId));
        }
        return new DirectoryRule(effect, new Selection(List.of(Long.toString(viewerId)), List.of()),
                new Selection(targets, List.of()));
    }
}
