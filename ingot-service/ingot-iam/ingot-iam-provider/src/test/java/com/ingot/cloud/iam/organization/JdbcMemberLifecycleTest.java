package com.ingot.cloud.iam.organization;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.ingot.cloud.iam.identity.ActiveIdentityService;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证成员生命周期的事务、所有者保护、全关系覆盖及跨域隔离。</p>
 * @author jy
 * @since 1.0.0
 */
class JdbcMemberLifecycleTest {
    private JdbcTemplate jdbc;
    private MemberLifecycle service;
    private static final AuthorizationContext TENANT = new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "101");
    private static final AuthorizationContext PLATFORM = new AuthorizationContext(AuthorizationDomain.PLATFORM, null, "1", "1001");
    // 仅本地持久化夹具使用；运行时必须接真实授权引擎，禁止复用此空实现。
    private static final MemberMutationGuard ALLOW = (actor, operation, member, before, after) -> { };

    @BeforeEach
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("CREATE TABLE iam_account(id BIGINT PRIMARY KEY, enabled BOOLEAN, deleted_at TIMESTAMP, version BIGINT)");
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY, account_id BIGINT, status VARCHAR(16), version BIGINT, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_tenant(id BIGINT PRIMARY KEY, owner_member_id BIGINT, enabled BOOLEAN, deleted_at TIMESTAMP, version BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, account_id BIGINT, tenant_id BIGINT, status VARCHAR(16), version BIGINT, updated_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY, tenant_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, member_id BIGINT, department_id BIGINT, is_primary BOOLEAN, PRIMARY KEY(tenant_id,member_id,department_id))");
        jdbc.execute("CREATE TABLE iam_authorization_audit(id BIGINT PRIMARY KEY,event_id VARCHAR(64),actor_account_id BIGINT,actor_member_id BIGINT,domain VARCHAR(16),tenant_id BIGINT,target_type VARCHAR(64),target_id VARCHAR(128),change_type VARCHAR(64),safe_before VARCHAR(4096),safe_after VARCHAR(4096),revisions VARCHAR(4096),occurred_at TIMESTAMP)");
        jdbc.update("INSERT INTO iam_account VALUES (1,TRUE,NULL,0),(2,TRUE,NULL,0)");
        jdbc.update("INSERT INTO iam_platform_member VALUES (1001,1,'ACTIVE',0,NULL),(1002,2,'ACTIVE',0,NULL)");
        jdbc.update("INSERT INTO iam_tenant VALUES (10,101,TRUE,NULL,0),(20,201,TRUE,NULL,0)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES (101,1,10,'ACTIVE',0,NULL),(102,2,10,'ACTIVE',0,NULL),(201,1,20,'ACTIVE',0,NULL),(202,2,20,'ACTIVE',0,NULL)");
        jdbc.update("INSERT INTO iam_department VALUES (11,10),(12,10),(13,10),(21,20)");
        jdbc.update("INSERT INTO iam_member_department VALUES (10,102,11,TRUE),(10,102,12,FALSE)");
        service = new MemberLifecycle(new DataSourceTransactionManager(dataSource),
                new ActiveIdentityService(com.ingot.cloud.iam.persistence.IamMybatisTestAccess.identity(dataSource)),
                com.ingot.cloud.iam.persistence.IamMybatisTestAccess.members(dataSource),
                com.ingot.cloud.iam.persistence.IamMybatisTestAccess.audits(dataSource));
    }

    @Test
    void removingTenantMemberPreservesGlobalAccountAndOtherDomainMembers() {
        assertEquals("1", service.remove(TENANT, "102", new VersionInput("0"), 5001, ALLOW));
        assertEquals(MemberStatus.REMOVED.name(), state("iam_tenant_member", 102));
        assertEquals(MemberStatus.ACTIVE.name(), state("iam_tenant_member", 202));
        assertEquals(MemberStatus.ACTIVE.name(), state("iam_platform_member", 1002));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM iam_account", Integer.class));
        assertThrows(BizException.class, () -> service.changeStatus(TENANT, "102", new MemberStatusInput(MemberStatus.ACTIVE, "1"), 5002, ALLOW));
    }

    @Test
    void suspendingPlatformMemberDoesNotTouchTenantIdentity() {
        assertEquals("1", service.changeStatus(PLATFORM, "1002", new MemberStatusInput(MemberStatus.SUSPENDED, "0"), 5001, ALLOW));
        assertEquals(MemberStatus.ACTIVE.name(), state("iam_tenant_member", 102));
        assertThrows(BizException.class, () -> service.remove(PLATFORM, "102", new VersionInput("0"), 5002, ALLOW));
    }

    @Test
    void lifecycleGuardReceivesAllDepartmentsAndFailureHasNoWrites() {
        MemberMutationGuard reject = (actor, operation, member, before, after) -> {
            assertEquals(Set.of("11", "12"), before);
            assertEquals(MemberMutationGuard.Operation.CHANGE_STATUS, operation);
            throw new BizException(IamReasonCode.DATA_SCOPE_DENIED);
        };
        assertThrows(BizException.class, () -> service.changeStatus(TENANT, "102", new MemberStatusInput(MemberStatus.SUSPENDED, "0"), 5001, reject));
        assertEquals(MemberStatus.ACTIVE.name(), state("iam_tenant_member", 102));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_authorization_audit", Integer.class));
    }

    @Test
    void ownerCannotBeSuspendedOrRemoved() {
        assertThrows(BizException.class, () -> service.remove(TENANT, "101", new VersionInput("0"), 5001, ALLOW));
        assertThrows(BizException.class, () -> service.changeStatus(TENANT, "101", new MemberStatusInput(MemberStatus.SUSPENDED, "0"), 5002, ALLOW));
    }

    @Test
    void staleVersionAndCrossTenantTargetReject() {
        assertThrows(BizException.class, () -> service.remove(TENANT, "102", new VersionInput("7"), 5001, ALLOW));
        assertThrows(BizException.class, () -> service.remove(TENANT, "202", new VersionInput("0"), 5002, ALLOW));
        assertEquals(MemberStatus.ACTIVE.name(), state("iam_tenant_member", 102));
    }

    @Test
    void auditFailureRollsBackMembership() {
        jdbc.update("INSERT INTO iam_authorization_audit(id) VALUES (5001)");
        assertThrows(DataAccessException.class, () -> service.remove(TENANT, "102", new VersionInput("0"), 5001, ALLOW));
        assertEquals(MemberStatus.ACTIVE.name(), state("iam_tenant_member", 102));
        assertEquals(0L, jdbc.queryForObject("SELECT version FROM iam_tenant_member WHERE id=102", Long.class));
    }

    @Test
    void departmentReplacementChecksOnlyAffectedOldAndNewRelations() {
        AtomicReference<Set<String>> oldChecked = new AtomicReference<>();
        AtomicReference<Set<String>> newChecked = new AtomicReference<>();
        var plan = new MemberDepartmentPlan("0", List.of(new MemberDepartmentPlan.Department("11", true), new MemberDepartmentPlan.Department("13", false)));
        assertEquals("1", service.replaceDepartments(TENANT, "102", plan, 5001, (actor, operation, member, before, after) -> {
            oldChecked.set(before); newChecked.set(after);
        }));
        assertEquals(Set.of("12"), oldChecked.get());
        assertEquals(Set.of("13"), newChecked.get());
        assertEquals(List.of(11L, 13L), jdbc.queryForList("SELECT department_id FROM iam_member_department WHERE member_id=102 ORDER BY department_id", Long.class));
    }

    @Test
    void primaryChangeRequiresBothEndsAndAuditFailureRestoresRelations() {
        var plan = new MemberDepartmentPlan("0", List.of(new MemberDepartmentPlan.Department("11", false), new MemberDepartmentPlan.Department("12", true)));
        jdbc.update("INSERT INTO iam_authorization_audit(id) VALUES (5001)");
        assertThrows(DataAccessException.class, () -> service.replaceDepartments(TENANT, "102", plan, 5001, (actor, operation, member, before, after) -> {
            assertEquals(Set.of("11", "12"), before);
            assertEquals(Set.of("11", "12"), after);
        }));
        assertTrue(jdbc.queryForObject("SELECT is_primary FROM iam_member_department WHERE member_id=102 AND department_id=11", Boolean.class));
    }

    @Test
    void crossTenantDepartmentsAndPlatformDepartmentChangesReject() {
        var plan = new MemberDepartmentPlan("0", List.of(new MemberDepartmentPlan.Department("21", true)));
        assertThrows(BizException.class, () -> service.replaceDepartments(TENANT, "102", plan, 5001, ALLOW));
        assertThrows(BizException.class, () -> service.replaceDepartments(PLATFORM, "1002", plan, 5002, ALLOW));
        assertEquals(2, jdbc.queryForObject("SELECT COUNT(*) FROM iam_member_department", Integer.class));
    }

    private String state(String table, long id) {
        return jdbc.queryForObject("SELECT status FROM " + table + " WHERE id=?", String.class, id);
    }
}
