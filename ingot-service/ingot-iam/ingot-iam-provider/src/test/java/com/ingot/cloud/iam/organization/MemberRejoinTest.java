package com.ingot.cloud.iam.organization;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.cloud.iam.assignment.AssignmentService;
import com.ingot.cloud.iam.authorization.snapshot.AuthorizationChangeNotifier;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.extension.BuiltinResourceProviders;
import com.ingot.cloud.iam.identity.ActiveIdentity;
import com.ingot.cloud.iam.persistence.DepartmentLeaderRepository;
import com.ingot.cloud.iam.support.IamAccess;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentLeaderMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.framework.authorization.field.FieldProjectionEngine;
import com.ingot.framework.authorization.field.FieldWriteExecutor;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.iam.MemberDepartmentBinding;
import com.ingot.framework.commons.model.iam.MemberStatus;
import com.ingot.framework.commons.model.iam.TenantMemberCreateInput;
import com.ingot.framework.commons.model.iam.TenantMemberRejoinInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>验证已移出成员重新加入会清掉旧关系、恢复在职并在失败时回滚。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class MemberRejoinTest {
    private static final AuthorizationContext TENANT = new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1",
            "101");
    private JdbcTemplate jdbc;
    private MemberRejoin service;
    private FieldProjectionEngine projection;
    private final AtomicInteger invalidations = new AtomicInteger();

    @BeforeEach
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa",
                "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE iam_account(id BIGINT PRIMARY KEY, username VARCHAR(64), password_hash VARCHAR(128),
                  phone VARCHAR(32), email VARCHAR(128), enabled BOOLEAN, must_change_password BOOLEAN,
                  password_changed_at TIMESTAMP, last_login_at TIMESTAMP, deleted_at TIMESTAMP, version BIGINT,
                  created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE iam_tenant(id BIGINT PRIMARY KEY, name VARCHAR(64), avatar VARCHAR(128),
                  owner_member_id BIGINT, enabled BOOLEAN, deleted_at TIMESTAMP, version BIGINT)
                """);
        jdbc.execute("""
                CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, tenant_id BIGINT, account_id BIGINT,
                  display_name VARCHAR(128), avatar VARCHAR(128), phone VARCHAR(32), email VARCHAR(128),
                  status VARCHAR(16), version BIGINT, created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE iam_department(id BIGINT PRIMARY KEY, tenant_id BIGINT, parent_id BIGINT,
                  name VARCHAR(64), sort_order INT, version BIGINT)
                """);
        jdbc.execute("""
                CREATE TABLE iam_member_department(tenant_id BIGINT, member_id BIGINT, department_id BIGINT,
                  is_primary BOOLEAN, PRIMARY KEY(tenant_id, member_id, department_id))
                """);
        jdbc.execute("""
                CREATE TABLE iam_department_leader(tenant_id BIGINT, department_id BIGINT, member_id BIGINT,
                  is_primary BOOLEAN, PRIMARY KEY(tenant_id, department_id, member_id))
                """);
        jdbc.execute("""
                CREATE TABLE iam_role_assignment(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,
                  subject_type VARCHAR(16), platform_member_id BIGINT, platform_group_id BIGINT, tenant_member_id BIGINT,
                  tenant_group_id BIGINT, revision_id BIGINT, revision_kind VARCHAR(16), scope_bindings VARCHAR(128),
                  delegation_grant_id BIGINT, valid_from TIMESTAMP, valid_until TIMESTAMP, status VARCHAR(16),
                  source VARCHAR(32), version BIGINT, created_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE iam_delegation_grant(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,
                  platform_administrator_id BIGINT, tenant_administrator_id BIGINT, valid_from TIMESTAMP,
                  valid_until TIMESTAMP, assignment_duration_mode VARCHAR(16), max_assignment_duration_seconds BIGINT,
                  max_assignment_duration_nanos INT, status VARCHAR(16), version BIGINT, created_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE iam_delegation_recipient_member(id BIGINT PRIMARY KEY, delegation_id BIGINT,
                  domain VARCHAR(16), tenant_id BIGINT, platform_member_id BIGINT, tenant_member_id BIGINT)
                """);
        jdbc.execute("""
                CREATE TABLE iam_tenant_group(id BIGINT PRIMARY KEY, tenant_id BIGINT, name VARCHAR(64),
                  description VARCHAR(128), version BIGINT, created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE iam_tenant_group_member(tenant_id BIGINT, group_id BIGINT, member_id BIGINT,
                  PRIMARY KEY(tenant_id, group_id, member_id))
                """);
        jdbc.execute("""
                CREATE TABLE iam_app_audience(tenant_id BIGINT, application_id BIGINT, enabled BOOLEAN,
                  audience_kind VARCHAR(16), version BIGINT, PRIMARY KEY(tenant_id, application_id))
                """);
        jdbc.execute("""
                CREATE TABLE iam_audience_member(tenant_id BIGINT, application_id BIGINT, member_id BIGINT,
                  PRIMARY KEY(tenant_id, application_id, member_id))
                """);
        jdbc.execute("""
                CREATE TABLE iam_policy_selector_member(tenant_id BIGINT, selector_id BIGINT, member_id BIGINT,
                  PRIMARY KEY(tenant_id, selector_id, member_id))
                """);
        jdbc.execute("""
                CREATE TABLE iam_authorization_audit(id BIGINT PRIMARY KEY, event_id VARCHAR(64),
                  actor_account_id BIGINT, actor_member_id BIGINT, domain VARCHAR(16), tenant_id BIGINT,
                  target_type VARCHAR(64), target_id VARCHAR(128), change_type VARCHAR(64), safe_before VARCHAR(4096),
                  safe_after VARCHAR(4096), revisions VARCHAR(4096), delegation_id BIGINT, assignment_id BIGINT,
                  trace_id VARCHAR(128), occurred_at TIMESTAMP)
                """);
        jdbc.update("INSERT INTO iam_account(id, username, enabled, deleted_at, version) VALUES (2, '13800138000', TRUE, NULL, 0)");
        jdbc.update("INSERT INTO iam_tenant(id, owner_member_id, enabled, deleted_at, version) VALUES (10, 101, TRUE, NULL, 0)");
        jdbc.update("""
                INSERT INTO iam_tenant_member(id, tenant_id, account_id, display_name, phone, email, status, version)
                VALUES (102, 10, 2, '旧名', '13800138000', 'old@example.com', 'REMOVED', 3)
                """);
        jdbc.update("INSERT INTO iam_department(id, tenant_id, name, sort_order, version) VALUES (11, 10, '产品', 1, 1), (12, 10, '研发', 2, 4)");
        jdbc.update("INSERT INTO iam_member_department VALUES (10, 102, 12, TRUE)");
        jdbc.update("INSERT INTO iam_department_leader VALUES (10, 12, 102, TRUE)");
        jdbc.update("""
                INSERT INTO iam_role_assignment(id, domain, tenant_id, subject_type, tenant_member_id, status, source, version)
                VALUES (801, 'TENANT', 10, 'MEMBER', 102, 'ACTIVE', 'MANUAL', 0)
                """);
        jdbc.update("""
                INSERT INTO iam_delegation_grant(id, domain, tenant_id, tenant_administrator_id, status, version)
                VALUES (701, 'TENANT', 10, 102, 'ACTIVE', 0)
                """);
        jdbc.update("""
                INSERT INTO iam_delegation_recipient_member(id, delegation_id, domain, tenant_id, tenant_member_id)
                VALUES (901, 701, 'TENANT', 10, 102)
                """);
        jdbc.update("INSERT INTO iam_tenant_group(id, tenant_id, name, version) VALUES (501, 10, '研发', 2)");
        jdbc.update("INSERT INTO iam_tenant_group_member VALUES (10, 501, 102)");
        jdbc.update("INSERT INTO iam_app_audience VALUES (10, 41, TRUE, 'SELECTED', 0)");
        jdbc.update("INSERT INTO iam_audience_member VALUES (10, 41, 102)");
        jdbc.update("INSERT INTO iam_policy_selector_member VALUES (10, 61, 102)");
        service = createService(dataSource);
    }

    @Test
    void rejoinsRemovedMemberAndClearsRelations() {
        var result = service.rejoin("102", input("3", "11"));
        assertEquals("102", result.id());
        assertEquals("4", result.version());
        assertTrue(result.rejoined());
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM iam_tenant_member WHERE id=102", String.class));
        assertEquals("新名", jdbc.queryForObject("SELECT display_name FROM iam_tenant_member WHERE id=102", String.class));
        assertEquals("13800138001", jdbc.queryForObject("SELECT phone FROM iam_tenant_member WHERE id=102", String.class));
        assertEquals(1, jdbc.queryForObject(
                "SELECT COUNT(*) FROM iam_member_department WHERE member_id=102 AND department_id=11 AND is_primary=TRUE",
                Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_member_department WHERE department_id=12", Integer.class));
        assertEquals("REVOKED", jdbc.queryForObject("SELECT status FROM iam_role_assignment WHERE id=801", String.class));
        assertEquals("REVOKED", jdbc.queryForObject("SELECT status FROM iam_delegation_grant WHERE id=701", String.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_delegation_recipient_member", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_tenant_group_member", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT version FROM iam_tenant_group WHERE id=501", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_department_leader", Integer.class));
        assertEquals(5, jdbc.queryForObject("SELECT version FROM iam_department WHERE id=12", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_audience_member", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT version FROM iam_app_audience WHERE application_id=41", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_policy_selector_member", Integer.class));
        assertEquals(1, invalidations.get());
        verify(projection).submitted(any(TenantMemberCreateInput.class),
                argThat((Set<String> fields) -> fields.contains("displayName") && fields.contains("phone")
                        && fields.contains("email") && !fields.contains("expectedVersion")),
                eq(BuiltinResourceProviders.TENANT_MEMBER_RESOURCE));
    }

    @Test
    void rejectsStaleVersion() {
        BizException failure = assertThrows(BizException.class, () -> service.rejoin("102", input("0", "11")));
        assertEquals(IamReasonCode.REVISION_CONFLICT.getCode(), failure.getCode());
        assertEquals("REMOVED", jdbc.queryForObject("SELECT status FROM iam_tenant_member WHERE id=102", String.class));
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM iam_role_assignment WHERE id=801", String.class));
        assertEquals(0, invalidations.get());
    }

    @Test
    void missingDepartmentRollsBackRelationCleanup() {
        BizException failure = assertThrows(BizException.class, () -> service.rejoin("102", input("3", "99")));
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), failure.getCode());
        assertEquals("REMOVED", jdbc.queryForObject("SELECT status FROM iam_tenant_member WHERE id=102", String.class));
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM iam_role_assignment WHERE id=801", String.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_tenant_group_member", Integer.class));
        assertEquals(0, invalidations.get());
    }

    private MemberRejoin createService(DriverManagerDataSource dataSource) {
        IamAccess access = mock(IamAccess.class);
        AtomicLong ids = new AtomicLong(5000);
        when(access.require(AuthorizationDomain.TENANT, IamAction.TENANT_MEMBER_CREATE))
                .thenReturn(new ActiveIdentity(TENANT, "0", "0", "0"));
        when(access.nextId()).thenAnswer(invocation -> ids.getAndIncrement());
        ResourceAccess scopes = mock(ResourceAccess.class);
        AssignmentService locks = mock(AssignmentService.class);
        projection = mock(FieldProjectionEngine.class);
        FieldWriteExecutor fieldWrites = mock(FieldWriteExecutor.class);
        when(projection.submitted(any(), any(), eq(BuiltinResourceProviders.TENANT_MEMBER_RESOURCE)))
                .thenReturn(Map.of());
        return new MemberRejoin(access, scopes, locks, IamMybatisTestAccess.memberQueries(dataSource),
                IamMybatisTestAccess.members(dataSource), IamMybatisTestAccess.accountQueries(dataSource),
                IamMybatisTestAccess.assignments(dataSource), IamMybatisTestAccess.delegations(dataSource),
                IamMybatisTestAccess.groups(dataSource),
                new DepartmentLeaderRepository(IamMybatisTestAccess.mapper(dataSource, IamDepartmentLeaderMapper.class),
                        IamMybatisTestAccess.mapper(dataSource, IamTenantMemberMapper.class)),
                IamMybatisTestAccess.entitlements(dataSource), IamMybatisTestAccess.policies(dataSource), projection,
                fieldWrites, IamMybatisTestAccess.audits(dataSource),
                new AuthorizationChangeNotifier(event -> invalidations.incrementAndGet()),
                new DataSourceTransactionManager(dataSource));
    }

    private static TenantMemberRejoinInput input(String version, String departmentId) {
        return new TenantMemberRejoinInput(version, "新名", "13800138001", "new@example.com",
                List.of(new MemberDepartmentBinding(departmentId, true)));
    }
}
