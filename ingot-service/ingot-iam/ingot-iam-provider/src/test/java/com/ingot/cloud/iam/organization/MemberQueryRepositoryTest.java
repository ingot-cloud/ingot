package com.ingot.cloud.iam.organization;

import java.util.stream.LongStream;
import java.util.Map;
import java.time.Instant;
import com.ingot.cloud.iam.evaluation.ObjectCapabilities;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.evaluation.ResourceAccess;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.evaluation.DepartmentClosure;
import com.ingot.cloud.iam.evaluation.ObjectScopeCompiler;
import com.ingot.cloud.iam.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.evaluation.ScopeClause;
import com.ingot.cloud.iam.evaluation.ResolvedActionScope;
import com.ingot.framework.commons.model.iam.IamAction;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.cloud.iam.persistence.QueryCountingDataSource;
import java.util.List;
import java.math.BigInteger;
import java.util.UUID;

import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.framework.commons.model.iam.MemberDepartmentView;
import com.ingot.framework.commons.model.iam.MemberRecord;
import com.ingot.framework.commons.model.iam.MemberStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <p>验证平台成员按 ID 精确回显，不把大页码当全量。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class MemberQueryRepositoryTest {
    private MemberQueryRepository members;
    private JdbcTemplate jdbc;
    private QueryCountingDataSource counting;

    @BeforeEach
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        counting = new QueryCountingDataSource(dataSource);
        jdbc = new JdbcTemplate(counting);
        jdbc.execute("""
                CREATE TABLE iam_account(id BIGINT PRIMARY KEY, username VARCHAR(64), phone VARCHAR(32),
                  email VARCHAR(128), deleted_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY, account_id BIGINT, display_name VARCHAR(128),
                  avatar VARCHAR(256), phone VARCHAR(32), email VARCHAR(128), status VARCHAR(16), version BIGINT DEFAULT 0, created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, tenant_id BIGINT, account_id BIGINT,
                  display_name VARCHAR(128), avatar VARCHAR(256), phone VARCHAR(32), email VARCHAR(128),
                  status VARCHAR(16), version BIGINT DEFAULT 0, created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY, tenant_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, member_id BIGINT, department_id BIGINT)");
        jdbc.update("INSERT INTO iam_tenant_member(id,tenant_id,status) VALUES (2001,10,'ACTIVE'),(2002,10,'ACTIVE'),"
                + "(2003,10,'SUSPENDED'),(2004,10,'REMOVED')");
        jdbc.update("INSERT INTO iam_member_department VALUES (10,2001,1001103),(10,2002,1001104),(10,2003,1001103)");
        jdbc.execute("CREATE TABLE iam_platform_group_member(group_id BIGINT, member_id BIGINT)");
        jdbc.update("INSERT INTO iam_account VALUES (1,'alice',NULL,NULL,NULL),(2,'bob',NULL,NULL,NULL)");
        jdbc.update("INSERT INTO iam_platform_member(id,account_id,display_name,avatar,status,version,created_at,updated_at) VALUES (1001,1,'池鑫',NULL,'ACTIVE',0,NULL,NULL),"
                + "(1002,2,'杨紫微',NULL,'ACTIVE',0,NULL,NULL)");
        jdbc.update("INSERT INTO iam_platform_group_member VALUES (9,1002),(9,1001)");
        members = IamMybatisTestAccess.memberQueries(counting);
    }

    @Test
    void pageTenantFiltersDirectDepartmentAndStatus() {
        var all = members.pageTenant(10, ObjectScope.all(), null, null, 1, 20);
        assertEquals(3, all.getTotal());
        var inDept = members.pageTenant(10, ObjectScope.all(), null, null, 1001103L, null, 1, 20);
        assertEquals(2, inDept.getTotal());
        assertEquals(List.of(2001L, 2003L), inDept.getRecords().stream().map(row -> row.getId().longValue()).toList());
        var activeInDept = members.pageTenant(10, ObjectScope.all(), null, null, 1001103L, MemberStatus.ACTIVE, 1, 20);
        assertEquals(1, activeInDept.getTotal());
        assertEquals(2001L, activeInDept.getRecords().getFirst().getId().longValue());
        var removed = members.pageTenant(10, ObjectScope.all(), null, null, null, MemberStatus.REMOVED, 1, 20);
        assertEquals(1, removed.getTotal());
        assertEquals(2004L, removed.getRecords().getFirst().getId().longValue());
        var empty = members.pageTenant(10, ObjectScope.all(), null, null, 999L, null, 1, 20);
        assertEquals(0, empty.getTotal());
    }

    @Test
    void pagePlatformFiltersByIds() {
        var page = members.pagePlatform(ObjectScope.all(), 1, 20, null, null, List.of(1002L));
        assertEquals(1, page.getTotal());
        assertEquals(1002L, page.getRecords().getFirst().getId().longValue());
        assertEquals("杨紫微", page.getRecords().getFirst().getDisplayName());
    }

    @Test
    void pagePlatformWithoutIdsKeepsNameFilter() {
        var all = members.pagePlatform(ObjectScope.all(), 1, 20, null, MemberStatus.ACTIVE, List.of());
        assertEquals(2, all.getTotal());
        var byName = members.pagePlatform(ObjectScope.all(), 1, 20, "池", null, List.of());
        assertEquals(1, byName.getTotal());
        assertEquals("池鑫", byName.getRecords().getFirst().getDisplayName());
    }

    @Test
    void pagePlatformByGroupFiltersMembershipAndName() {
        var page = members.pagePlatformByGroup(ObjectScope.all(), 9, 1, 20, null);
        assertEquals(2, page.getTotal());
        var byName = members.pagePlatformByGroup(ObjectScope.all(), 9, 1, 20, "杨");
        assertEquals(1, byName.getTotal());
        assertEquals(1002L, byName.getRecords().getFirst().getId().longValue());
        var empty = members.pagePlatformByGroup(ObjectScope.all(), 8, 1, 20, null);
        assertEquals(0, empty.getTotal());
    }

    @Test
    void platformContactWritesDoNotModifyLoginContacts() {
        jdbc.update("UPDATE iam_account SET phone='13800000001',email='login@example.com' WHERE id=1");
        assertEquals(1, members.updatePlatform(1001, null, null, "13900000001", "contact@example.com", BigInteger.ZERO, java.util.Set.of("phone", "email")));
        assertEquals("13900000001", members.findPlatform(1001).getPhone());
        assertEquals("contact@example.com", members.findPlatform(1001).getEmail());
        assertEquals("13800000001", jdbc.queryForObject("SELECT phone FROM iam_account WHERE id=1", String.class));
        assertEquals("login@example.com", jdbc.queryForObject("SELECT email FROM iam_account WHERE id=1", String.class));
    }

    @Test
    void newMemberCopiesContactsOnceAndDoesNotFollowAccountChanges() {
        jdbc.update("UPDATE iam_account SET phone='13800000001',email='login@example.com' WHERE id=1");
        members.insertPlatform(1003, 1, "新成员", null);
        jdbc.update("UPDATE iam_account SET phone='13800000002',email='changed@example.com' WHERE id=1");
        assertEquals("13800000001", members.findPlatform(1003).getPhone());
        assertEquals("login@example.com", members.findPlatform(1003).getEmail());
    }

    @Test
    void nullKeepsContactsBlankClearsAndStaleVersionDoesNotWrite() {
        members.updatePlatform(1001, null, null, "13900000001", "contact@example.com", BigInteger.ZERO, java.util.Set.of("phone", "email"));
        assertEquals(1, members.updatePlatform(1001, "新显示名", null, null, null, BigInteger.ONE, java.util.Set.of("displayName")));
        assertEquals("13900000001", members.findPlatform(1001).getPhone());
        assertEquals(0, members.updatePlatform(1001, null, null, "other", "other", BigInteger.ZERO, java.util.Set.of("phone", "email")));
        assertEquals("contact@example.com", members.findPlatform(1001).getEmail());
        assertEquals(1, members.updatePlatform(1001, null, null, "", " ", BigInteger.TWO, java.util.Set.of("phone", "email")));
        assertNull(members.findPlatform(1001).getPhone());
        assertNull(members.findPlatform(1001).getEmail());
    }
    @Test
    void batchCapabilitiesMatchSingleObjectChecksAndRemainConstantAtTwentyRows() {
        for (long id = 1003; id <= 1020; id++) jdbc.update("INSERT INTO iam_platform_member(id,account_id,status) VALUES (?,1,'ACTIVE')", id);
        jdbc.update("UPDATE iam_platform_member SET status='REMOVED' WHERE id=1020");
        var actor = new AuthorizationContext(
                AuthorizationDomain.PLATFORM, null, "1", "1001");
        var update = IamAction.PLATFORM_MEMBER_UPDATE;
        var status = IamAction.PLATFORM_MEMBER_STATUS;
        var remove = IamAction.PLATFORM_MEMBER_REMOVE;
        var restricted = new ResolvedActionScope(List.of(
                new ScopeClause(false, false, false, false, List.of(), false, List.of("1001", "1002"))));
        var view = new AuthorizationEvaluator.AuthorizationView(
                List.of(update.getCode(), status.getCode(), remove.getCode()), List.of(), Map.of(
                    update.getCode(), restricted,
                    status.getCode(), new ResolvedActionScope(List.of(ScopeClause.universe())),
                    remove.getCode(), new ResolvedActionScope(List.of())),
                "test", Instant.now().plusSeconds(30));
        var evaluator = mock(AuthorizationEvaluator.class);
        var compiler = new ObjectScopeCompiler(
                mock(DepartmentClosure.class),
                mock(IamMemberDepartmentMapper.class));
        var resources = new ResourceAccess(evaluator, compiler, members,
                mock(DepartmentQueryRepository.class));
        var capabilities = new ObjectCapabilities(evaluator, resources);
        var snapshot = new ObjectCapabilities.Snapshot(actor, view);
        var ids = LongStream.rangeClosed(1001, 1020).mapToObj(BigInteger::valueOf).toList();
        counting.reset();
        capabilities.platformMembers(snapshot, ids.subList(0, 1));
        int one = counting.count();
        counting.reset();
        var batch = capabilities.platformMembers(snapshot, ids);
        assertEquals(one, counting.count());
        org.junit.jupiter.api.Assertions.assertTrue(counting.count() <= 3);
        for (var id : ids) assertEquals(capabilities.platformMember(snapshot, id.toString()), batch.get(id.toString()));
        org.junit.jupiter.api.Assertions.assertFalse(batch.get("1020").get(status.getCode()).allowed());
        org.junit.jupiter.api.Assertions.assertFalse(batch.get("1003").get(update.getCode()).allowed());
        org.junit.jupiter.api.Assertions.assertFalse(batch.get("1001").get(remove.getCode()).allowed());
    }

    @Test
    void tenantBatchCapabilitiesMatchSingleObjectChecksAndRemainConstantAtTwentyRows() {
        for (long id = 2101; id <= 2116; id++) {
            jdbc.update("INSERT INTO iam_tenant_member(id,tenant_id,status) VALUES (?,10,'ACTIVE')", id);
        }
        jdbc.update("INSERT INTO iam_department(id,tenant_id) VALUES (1001103,10),(1001104,10)");
        var actor = new AuthorizationContext(AuthorizationDomain.TENANT, "10", "1", "2001");
        var update = IamAction.TENANT_MEMBER_UPDATE;
        var status = IamAction.TENANT_MEMBER_STATUS;
        var remove = IamAction.TENANT_MEMBER_REMOVE;
        var assignDepartments = IamAction.TENANT_MEMBER_DEPARTMENTS;
        var departmentUpdate = IamAction.TENANT_DEPARTMENT_UPDATE;
        var departmentDelete = IamAction.TENANT_DEPARTMENT_DELETE;
        var restrictedMembers = new ResolvedActionScope(List.of(
                new ScopeClause(false, false, false, false, List.of(), false, List.of("2001", "2002"))));
        var managedDepartments = new ResolvedActionScope(List.of(
                new ScopeClause(false, false, false, false, List.of("1001103"), false, List.of())));
        var view = new AuthorizationEvaluator.AuthorizationView(List.of(update.getCode(), status.getCode(),
                remove.getCode(), assignDepartments.getCode(), departmentUpdate.getCode(), departmentDelete.getCode()),
                List.of(), Map.of(
                    update.getCode(), restrictedMembers,
                    status.getCode(), new ResolvedActionScope(List.of(ScopeClause.universe())),
                    remove.getCode(), new ResolvedActionScope(List.of()),
                    assignDepartments.getCode(), managedDepartments,
                    departmentUpdate.getCode(), new ResolvedActionScope(List.of(ScopeClause.universe())),
                    departmentDelete.getCode(), managedDepartments),
                "test", Instant.now().plusSeconds(30));
        var departmentRepository = new DepartmentQueryRepository(
                IamMybatisTestAccess.mapper(counting, IamDepartmentMapper.class),
                IamMybatisTestAccess.mapper(counting, IamMemberDepartmentMapper.class),
                IamMybatisTestAccess.mapper(counting, IamTenantMemberMapper.class));
        var resources = new ResourceAccess(mock(AuthorizationEvaluator.class),
                new ObjectScopeCompiler(mock(DepartmentClosure.class), mock(IamMemberDepartmentMapper.class)),
                members, departmentRepository);
        var capabilities = new ObjectCapabilities(mock(AuthorizationEvaluator.class), resources);
        var snapshot = new ObjectCapabilities.Snapshot(actor, view);
        var records = new java.util.ArrayList<MemberRecord>();
        records.add(tenantMember("2001", MemberStatus.ACTIVE, "1001103"));
        records.add(tenantMember("2002", MemberStatus.ACTIVE, "1001104"));
        records.add(tenantMember("2003", MemberStatus.SUSPENDED, "1001103"));
        records.add(tenantMember("2004", MemberStatus.REMOVED, "1001103"));
        for (long id = 2101; id <= 2116; id++) {
            records.add(tenantMember(Long.toString(id), MemberStatus.ACTIVE, "1001103"));
        }
        counting.reset();
        capabilities.tenantMembers(snapshot, records.subList(0, 1));
        int one = counting.count();
        counting.reset();
        var batch = capabilities.tenantMembers(snapshot, records);
        assertEquals(one, counting.count());
        org.junit.jupiter.api.Assertions.assertTrue(counting.count() <= 4);
        for (var record : records) {
            assertEquals(capabilities.tenantMember(snapshot, record), batch.get(record.id()));
        }
        org.junit.jupiter.api.Assertions.assertFalse(batch.get("2002").get(assignDepartments.getCode()).allowed());
        org.junit.jupiter.api.Assertions.assertFalse(batch.get("2004").get(status.getCode()).allowed());
        org.junit.jupiter.api.Assertions.assertTrue(batch.get("2004").get(assignDepartments.getCode()).allowed());
        org.junit.jupiter.api.Assertions.assertFalse(batch.get("2101").get(update.getCode()).allowed());

        var departmentIds = LongStream.rangeClosed(1, 20).mapToObj(Long::toString).toList();
        counting.reset();
        capabilities.departments(snapshot, departmentIds.subList(0, 1));
        int departmentOne = counting.count();
        counting.reset();
        var departmentBatch = capabilities.departments(snapshot, departmentIds);
        assertEquals(departmentOne, counting.count());
        org.junit.jupiter.api.Assertions.assertTrue(counting.count() <= 2);
        for (var id : departmentIds) {
            assertEquals(capabilities.department(snapshot, id), departmentBatch.get(id));
        }
        org.junit.jupiter.api.Assertions.assertTrue(departmentBatch.get("1").get(departmentUpdate.getCode()).allowed());
        org.junit.jupiter.api.Assertions.assertFalse(departmentBatch.get("1").get(departmentDelete.getCode()).allowed());
    }

    private static MemberRecord tenantMember(String id, MemberStatus status, String departmentId) {
        return new MemberRecord(id, "成员", null, null, null, null, status,
                List.of(new MemberDepartmentView(departmentId, "部门", true)));
    }

}
