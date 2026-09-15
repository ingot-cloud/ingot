package com.ingot.cloud.iam.identity;

import java.util.List;
import java.util.UUID;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>通过真实 MyBatis 事务验证最小初始化、固定引用与晚期失败的整体回滚。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class JdbcTenantInitializerTest {
    private JdbcTemplate jdbc;
    private TenantInitializer initializer;
    private final AuthorizationContext actor = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");

    @BeforeEach
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("identity/initialization.sql")).execute(dataSource);
        jdbc = new JdbcTemplate(dataSource);
        initializer = new TenantInitializer(new DataSourceTransactionManager(dataSource),
                new ActiveIdentityService(com.ingot.cloud.iam.persistence.IamMybatisTestAccess.identity(dataSource)),
                com.ingot.cloud.iam.persistence.IamMybatisTestAccess.tenantInit(dataSource),
                com.ingot.cloud.iam.persistence.IamMybatisTestAccess.audits(dataSource));
    }

    private TenantInitializationPlan plan(long governance, long directory, long application) {
        return new TenantInitializationPlan(10, "研发组织", 2, 101, "组织所有者", 111, "根部门",
                201, governance, directory, 22, List.of(new TenantInitializationPlan.Application(application, 301)), 401);
    }

    @Test
    void initializesOnlyOwnedRowsAndFixedReferences() {
        var result = initializer.initialize(actor, plan(11, 21, 1));
        assertEquals("10", result.id());
        assertEquals("0", result.version());
        assertEquals(101L, jdbc.queryForObject("SELECT owner_member_id FROM iam_tenant WHERE id=10", Long.class));
        assertEquals(2L, jdbc.queryForObject("SELECT account_id FROM iam_tenant_member WHERE id=101", Long.class));
        assertEquals(11L, jdbc.queryForObject("SELECT revision_id FROM iam_role_assignment", Long.class));
        assertEquals("INITIALIZATION", jdbc.queryForObject("SELECT source FROM iam_role_assignment", String.class));
        assertEquals("{}", jdbc.queryForObject("SELECT scope_bindings FROM iam_role_assignment", String.class));
        assertEquals(3, count("iam_role_definition"));
        assertEquals(3, count("iam_role_revision"));
        assertEquals(2, count("iam_default_policy_revision"));
        assertEquals(2, count("iam_account"));
        assertEquals(1, count("iam_member_department"));
        assertEquals("ALL", jdbc.queryForObject("SELECT audience_kind FROM iam_app_audience", String.class));
        assertEquals(1, count("iam_authorization_audit"));
        assertEquals("PLATFORM", jdbc.queryForObject("SELECT domain FROM iam_authorization_audit", String.class));
        var audit = jdbc.queryForObject("SELECT safe_after FROM iam_authorization_audit", String.class);
        assertFalse(audit.contains("组织所有者"));
        assertTrue(audit.contains("OWNER_MEMBER"));
    }

    @Test
    void auditFailureRollsBackAllPreviouslyInsertedRows() {
        jdbc.update("INSERT INTO iam_authorization_audit(id) VALUES (401)");
        assertThrows(DataAccessException.class, () -> initializer.initialize(actor, plan(11, 21, 1)));
        assertNoTenantRows();
        assertEquals(1, count("iam_authorization_audit"));
    }

    @Test
    void duplicateCreationDoesNotModifyExistingOrganization() {
        initializer.initialize(actor, plan(11, 21, 1));
        assertThrows(DataAccessException.class, () -> initializer.initialize(actor, plan(11, 21, 1)));
        assertEquals(1, count("iam_tenant"));
        assertEquals(1, count("iam_role_assignment"));
        assertEquals(1, count("iam_authorization_audit"));
    }

    @Test
    void sharedOrPlatformRoleCannotBecomeTenantGovernance() {
        for (long revision : List.of(12L, 13L)) {
            assertCode(IamReasonCode.ROLE_REVISION_UNAVAILABLE, plan(revision, 21, 1));
            assertNoTenantRows();
        }
    }

    @Test
    void wrongDefaultKindAndPlatformApplicationAreRejected() {
        assertCode(IamReasonCode.POLICY_CONFLICT, plan(11, 22, 1));
        assertCode(IamReasonCode.APPLICATION_UNAVAILABLE, plan(11, 21, 2));
        assertNoTenantRows();
    }

    @Test
    void disabledOwnerAccountOrActorCannotInitializeOrganization() {
        jdbc.update("UPDATE iam_account SET enabled=FALSE WHERE id=2");
        assertCode(IamReasonCode.IDENTITY_INVALID, plan(11, 21, 1));
        jdbc.update("UPDATE iam_account SET enabled=TRUE WHERE id=2");
        jdbc.update("UPDATE iam_platform_member SET status='SUSPENDED' WHERE id=1001");
        assertCode(IamReasonCode.IDENTITY_INVALID, plan(11, 21, 1));
        assertNoTenantRows();
    }

    private void assertCode(IamReasonCode code, TenantInitializationPlan plan) {
        assertEquals(code.getCode(), assertThrows(BizException.class, () -> initializer.initialize(actor, plan)).getCode());
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class);
    }

    private void assertNoTenantRows() {
        for (String table : List.of("iam_tenant", "iam_tenant_member", "iam_department", "iam_member_department",
                "iam_role_assignment", "iam_tenant_app_entitlement", "iam_app_audience", "iam_directory_policy", "iam_field_policy")) {
            assertEquals(0, count(table), table);
        }
    }
}
