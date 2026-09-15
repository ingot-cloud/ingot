package com.ingot.cloud.iam.evaluation;

import java.util.UUID;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证组展开并入有效授权，以及租户域缺少开通时失败关闭。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class JdbcAuthorizationEvaluatorTest {
    private JdbcTemplate jdbc;
    private JdbcAuthorizationEvaluator evaluator;
    private static final AuthorizationContext PLATFORM = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");
    private static final AuthorizationContext TENANT = new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");

    @BeforeEach
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE iam_application(id BIGINT PRIMARY KEY, domain VARCHAR(16), enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_action(id BIGINT PRIMARY KEY, application_id BIGINT, code VARCHAR(192), enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_role_definition(id BIGINT PRIMARY KEY, enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_role_revision(id BIGINT PRIMARY KEY, role_id BIGINT, base_revision_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_role_grant(revision_id BIGINT, action_id BIGINT, scopes VARCHAR(128))");
        jdbc.execute("CREATE TABLE iam_role_delta(revision_id BIGINT, action_id BIGINT, operation VARCHAR(32), scopes VARCHAR(128))");
        jdbc.execute("""
                CREATE TABLE iam_role_assignment(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,
                  subject_type VARCHAR(16), platform_member_id BIGINT, platform_group_id BIGINT,
                  tenant_member_id BIGINT, tenant_group_id BIGINT, revision_id BIGINT, status VARCHAR(16),
                  valid_from TIMESTAMP, valid_until TIMESTAMP, delegation_grant_id BIGINT, scope_bindings VARCHAR(512))
                """);
        jdbc.execute("CREATE TABLE iam_platform_group_member(group_id BIGINT, member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_group_member(tenant_id BIGINT, group_id BIGINT, member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_delegation_grant(id BIGINT PRIMARY KEY, status VARCHAR(16), valid_from TIMESTAMP, valid_until TIMESTAMP)");
        jdbc.execute("""
                CREATE TABLE iam_tenant_app_entitlement(tenant_id BIGINT, application_id BIGINT, enabled BOOLEAN,
                  valid_from TIMESTAMP, valid_until TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE iam_app_audience(tenant_id BIGINT, application_id BIGINT, enabled BOOLEAN, audience_kind VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_audience_member(tenant_id BIGINT, application_id BIGINT, member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_audience_group(tenant_id BIGINT, application_id BIGINT, group_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_audience_department(tenant_id BIGINT, application_id BIGINT, department_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, department_id BIGINT, member_id BIGINT)");
        jdbc.update("INSERT INTO iam_application VALUES (1,'PLATFORM',TRUE),(2,'TENANT',TRUE)");
        jdbc.update("INSERT INTO iam_action VALUES (11,1,?,TRUE),(12,2,?,TRUE)",
                IamAction.VALUE_PLATFORM_TENANT_CREATE, IamAction.VALUE_TENANT_MEMBER_STATUS);
        jdbc.update("INSERT INTO iam_role_definition VALUES (21,TRUE),(22,TRUE)");
        jdbc.update("INSERT INTO iam_role_revision VALUES (31,21,NULL),(32,22,NULL)");
        jdbc.update("INSERT INTO iam_role_grant VALUES (31,11,'[]'),(32,12,'[]')");
        jdbc.update("""
                INSERT INTO iam_role_assignment VALUES
                (41,'PLATFORM',NULL,'GROUP',NULL,501,NULL,NULL,31,'ACTIVE',TIMESTAMP '2000-01-01 00:00:00',NULL,NULL,'{}'),
                (42,'TENANT',10,'MEMBER',NULL,NULL,101,NULL,32,'ACTIVE',TIMESTAMP '2000-01-01 00:00:00',NULL,NULL,'{}')
                """);
        jdbc.update("INSERT INTO iam_platform_group_member VALUES (501,1001)");
        evaluator = new JdbcAuthorizationEvaluator(dataSource);
    }

    @Test
    void groupAssignmentExpandsToMember() {
        evaluator.require(PLATFORM, IamAction.PLATFORM_TENANT_CREATE);
        assertTrue(evaluator.evaluate(PLATFORM).actionCodes().contains(IamAction.VALUE_PLATFORM_TENANT_CREATE));
    }

    @Test
    void tenantActionWithoutEntitlementIsDenied() {
        BizException failure = assertThrows(BizException.class,
                () -> evaluator.require(TENANT, IamAction.TENANT_MEMBER_STATUS));
        assertEquals(IamReasonCode.ACTION_DENIED.getCode(), failure.getCode());
        assertFalse(evaluator.evaluate(TENANT).actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));
    }

    @Test
    void tenantEntitlementAndAudienceAllowAction() {
        jdbc.update("INSERT INTO iam_tenant_app_entitlement VALUES (10,2,TRUE,NULL,NULL)");
        jdbc.update("INSERT INTO iam_app_audience VALUES (10,2,TRUE,'ALL')");
        evaluator.require(TENANT, IamAction.TENANT_MEMBER_STATUS);
    }

    @Test
    void memberDepartmentsScopeIsBoundFromGrant() {
        jdbc.update("INSERT INTO iam_tenant_app_entitlement VALUES (10,2,TRUE,NULL,NULL)");
        jdbc.update("INSERT INTO iam_app_audience VALUES (10,2,TRUE,'ALL')");
        jdbc.update("UPDATE iam_role_grant SET scopes=? WHERE revision_id=32",
                "[{\"kind\":\"MEMBER_DEPARTMENTS\",\"includeDescendants\":true}]");
        var view = evaluator.evaluate(TENANT);
        assertTrue(view.actionCodes().contains(IamAction.VALUE_TENANT_MEMBER_STATUS));
        assertTrue(view.scope(IamAction.VALUE_TENANT_MEMBER_STATUS).clauses().stream()
                .anyMatch(clause -> clause.memberDepartments() && clause.memberDepartmentDescendants()));
    }
}
