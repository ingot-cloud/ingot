package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ingot.cloud.iam.persistence.mapper.IamDepartmentLeaderMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import com.ingot.framework.commons.error.BizException;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>用真实 MyBatis DML 计数和行创建标记验证差量维护不删除重插保留关系。</p>
 * @author jy
 * @since 1.0.0
 */
class RelationWriteTest {
    private JdbcTemplate jdbc;
    private DriverManagerDataSource source;
    private TransactionTemplate transaction;
    private Writes writes;

    @BeforeEach
    void database() {
        source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(source));
        jdbc.execute("CREATE SEQUENCE relation_stamp START WITH 1");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT,member_id BIGINT,department_id BIGINT,is_primary BOOLEAN,"
                + "primary_member_id BIGINT GENERATED ALWAYS AS(CASE WHEN is_primary THEN member_id ELSE NULL END),"
                + "stamp BIGINT DEFAULT NEXT VALUE FOR relation_stamp,PRIMARY KEY(tenant_id,member_id,department_id),UNIQUE(tenant_id,primary_member_id))");
        jdbc.execute("CREATE TABLE iam_department_leader(tenant_id BIGINT,department_id BIGINT,member_id BIGINT,is_primary BOOLEAN,"
                + "primary_department_id BIGINT GENERATED ALWAYS AS(CASE WHEN is_primary THEN department_id ELSE NULL END),"
                + "stamp BIGINT DEFAULT NEXT VALUE FOR relation_stamp,PRIMARY KEY(tenant_id,department_id,member_id),UNIQUE(tenant_id,primary_department_id))");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY,tenant_id BIGINT,version BIGINT)");
        jdbc.execute("CREATE TABLE iam_platform_group_member(group_id BIGINT,member_id BIGINT,stamp BIGINT DEFAULT NEXT VALUE FOR relation_stamp,PRIMARY KEY(group_id,member_id))");
        jdbc.execute("CREATE TABLE iam_tenant_group_member(tenant_id BIGINT,group_id BIGINT,member_id BIGINT,stamp BIGINT DEFAULT NEXT VALUE FOR relation_stamp,PRIMARY KEY(tenant_id,group_id,member_id))");
        jdbc.execute("CREATE TABLE iam_tenant_group_department(tenant_id BIGINT,group_id BIGINT,department_id BIGINT,include_descendants BOOLEAN,stamp BIGINT DEFAULT NEXT VALUE FOR relation_stamp,PRIMARY KEY(tenant_id,group_id,department_id))");
        jdbc.execute("CREATE TABLE iam_menu_action(application_id BIGINT,menu_id BIGINT,action_id BIGINT,stamp BIGINT DEFAULT NEXT VALUE FOR relation_stamp,PRIMARY KEY(application_id,menu_id,action_id))");
        jdbc.execute("CREATE TABLE iam_plan_application(plan_id BIGINT,application_id BIGINT,stamp BIGINT DEFAULT NEXT VALUE FOR relation_stamp,PRIMARY KEY(plan_id,application_id))");
        writes = new Writes();
        IamMybatisTestAccess.observeSql(source, writes);
    }

    @Test
    void memberDepartmentsPreserveRowsAndOnlyChangeNeededFlags() {
        jdbc.update("INSERT INTO iam_member_department(tenant_id,member_id,department_id,is_primary) VALUES(10,101,11,TRUE),(10,101,12,FALSE),(20,101,11,TRUE)");
        var repository = new MemberDepartmentWriteRepository(IamMybatisTestAccess.mapper(source,
                com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper.class));
        long stamp = stamp("iam_member_department", "tenant_id=10 AND department_id=11");
        transaction.executeWithoutResult(status -> repository.replace(10,101,Map.of("11",true,"12",false)));
        assertEquals(0, writes.count("iam_member_department"));
        transaction.executeWithoutResult(status -> repository.replace(10,101,Map.of("11",true,"13",false)));
        assertEquals(2, writes.count("iam_member_department"));
        assertEquals(stamp, stamp("iam_member_department", "tenant_id=10 AND department_id=11"));
        writes.sql.clear();
        transaction.executeWithoutResult(status -> repository.replace(10,101,Map.of("11",false,"13",true)));
        assertEquals(2, writes.count("iam_member_department"));
        assertTrue(writes.sql.stream().allMatch(sql -> sql.startsWith("UPDATE ")));
        assertEquals(stamp, stamp("iam_member_department", "tenant_id=10 AND department_id=11"));
        writes.sql.clear();
        transaction.executeWithoutResult(status -> repository.replace(10,101,null));
        assertEquals(2, writes.count("iam_member_department"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_member_department WHERE tenant_id=20", Integer.class));
    }

    @Test
    void firstPrimaryMembershipNeedsOnlyOneInsert() {
        var repository = new MemberDepartmentWriteRepository(IamMybatisTestAccess.mapper(source,
                com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper.class));
        transaction.executeWithoutResult(status -> repository.replace(10,101,Map.of("11",true)));
        assertEquals(1,writes.count("iam_member_department"));
        assertTrue(writes.sql.getFirst().startsWith("INSERT "));
        assertTrue(jdbc.queryForObject("SELECT is_primary FROM iam_member_department",Boolean.class));
    }

    @Test
    void bothMemberEntryPointsUseSharedWriterAndRollbackAtomically() {
        jdbc.update("INSERT INTO iam_member_department(tenant_id,member_id,department_id,is_primary) VALUES(10,101,11,TRUE)");
        transaction.executeWithoutResult(status -> IamMybatisTestAccess.memberQueries(source).replaceDepartments(10,101,Map.of("11",true)));
        transaction.executeWithoutResult(status -> IamMybatisTestAccess.members(source).replaceDepartments(10,101,Map.of("11",true)));
        assertEquals(0, writes.count("iam_member_department"));
        assertThrows(NullPointerException.class, () -> IamMybatisTestAccess.members(source).replaceDepartments(10,101,null));
        assertEquals(0, writes.count("iam_member_department"));
        long stamp = stamp("iam_member_department", "tenant_id=10");
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            IamMybatisTestAccess.members(source).replaceDepartments(10,101,Map.of("12",true));
            throw new IllegalStateException("simulate audit failure");
        }));
        assertEquals(stamp, stamp("iam_member_department", "tenant_id=10"));
    }

    @Test
    void groupsPreserveMembersAndUpdateDepartmentAttributeInPlace() {
        jdbc.update("INSERT INTO iam_platform_group_member(group_id,member_id) VALUES(50,101),(50,102)");
        jdbc.update("INSERT INTO iam_tenant_group_member(tenant_id,group_id,member_id) VALUES(10,50,101),(10,50,102),(20,50,101)");
        jdbc.update("INSERT INTO iam_tenant_group_department(tenant_id,group_id,department_id,include_descendants) VALUES(10,50,11,FALSE),(20,50,11,FALSE)");
        var groups = IamMybatisTestAccess.groups(source);
        long memberStamp = stamp("iam_tenant_group_member", "tenant_id=10 AND member_id=101");
        long departmentStamp = stamp("iam_tenant_group_department", "tenant_id=10");
        transaction.executeWithoutResult(status -> {
            groups.replacePlatformMembers(50,List.of(101L,102L));
            groups.replaceTenantSelection(10,50,List.of(101L,102L),Map.of(11L,false));
        });
        assertEquals(0,writes.sql.size());
        transaction.executeWithoutResult(status -> {
            groups.replacePlatformMembers(50,List.of(101L,103L));
            groups.replaceTenantSelection(10,50,List.of(101L,103L),Map.of(11L,true,12L,false));
        });
        assertEquals(2,writes.count("iam_platform_group_member"));
        assertEquals(2,writes.count("iam_tenant_group_member"));
        assertEquals(2,writes.count("iam_tenant_group_department"));
        assertEquals(memberStamp,stamp("iam_tenant_group_member","tenant_id=10 AND member_id=101"));
        assertEquals(departmentStamp,stamp("iam_tenant_group_department","tenant_id=10 AND department_id=11"));
        assertFalse(jdbc.queryForObject("SELECT include_descendants FROM iam_tenant_group_department WHERE tenant_id=20",Boolean.class));
        writes.sql.clear();
        transaction.executeWithoutResult(status -> groups.replaceTenantSelection(10,50,List.of(),Map.of()));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM iam_tenant_group_member WHERE tenant_id=10",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM iam_tenant_group_member WHERE tenant_id=20",Integer.class));
        assertThrows(BizException.class,()->groups.replacePlatformMembers(50,List.of(101L,101L)));
    }

    @Test
    void menuAndPlanBindingsUseDifferenceAndRejectDuplicatesBeforeWriting() {
        jdbc.update("INSERT INTO iam_menu_action(application_id,menu_id,action_id) VALUES(1,2,3),(1,2,4),(2,2,3)");
        jdbc.update("INSERT INTO iam_plan_application(plan_id,application_id) VALUES(50,1),(50,2)");
        var catalog = IamMybatisTestAccess.catalogs(source);
        long menuStamp = stamp("iam_menu_action","application_id=1 AND action_id=3");
        long planStamp = stamp("iam_plan_application","application_id=1");
        transaction.executeWithoutResult(status -> {
            catalog.replaceMenuActions(1,2,List.of(4L,3L));
            catalog.replacePlanApplications(50,List.of(2L,1L));
        });
        assertEquals(0,writes.sql.size());
        transaction.executeWithoutResult(status -> {
            catalog.replaceMenuActions(1,2,List.of(3L,5L));
            catalog.replacePlanApplications(50,List.of(1L,3L));
        });
        assertEquals(2,writes.count("iam_menu_action"));
        assertEquals(2,writes.count("iam_plan_application"));
        assertEquals(menuStamp,stamp("iam_menu_action","application_id=1 AND action_id=3"));
        assertEquals(planStamp,stamp("iam_plan_application","application_id=1"));
        writes.sql.clear();
        assertThrows(BizException.class,()->catalog.replaceMenuActions(1,2,List.of(3L,3L)));
        assertThrows(BizException.class,()->catalog.replacePlanApplications(50,List.of(1L,1L)));
        assertEquals(0,writes.sql.size());
        transaction.executeWithoutResult(status -> {
            catalog.replaceMenuActions(1,2,null);
            catalog.replacePlanApplications(50,List.of());
        });
        assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM iam_menu_action",Integer.class));
    }

    @Test
    void unchangedLeaderPrimaryIsNeverClearedOrReset() {
        jdbc.update("INSERT INTO iam_department VALUES(11,10,0)");
        jdbc.update("INSERT INTO iam_department_leader(tenant_id,department_id,member_id,is_primary) VALUES(10,11,101,TRUE),(10,11,102,FALSE)");
        var repository = new DepartmentLeaderRepository(IamMybatisTestAccess.mapper(source,IamDepartmentLeaderMapper.class),
                IamMybatisTestAccess.mapper(source,IamTenantMemberMapper.class));
        var before = Map.of(BigInteger.valueOf(101),true,BigInteger.valueOf(102),false);
        transaction.executeWithoutResult(status -> repository.save(BigInteger.TEN,BigInteger.valueOf(11),before,before,BigInteger.ZERO,false));
        assertEquals(0,writes.sql.size());
        var added = Map.of(BigInteger.valueOf(101),true,BigInteger.valueOf(102),false,BigInteger.valueOf(103),false);
        transaction.executeWithoutResult(status -> repository.save(BigInteger.TEN,BigInteger.valueOf(11),before,added,BigInteger.ZERO,false));
        assertEquals(1,writes.count("iam_department_leader"));
        assertTrue(writes.sql.getFirst().startsWith("INSERT "));
        writes.sql.clear();
        var switched = Map.of(BigInteger.valueOf(101),false,BigInteger.valueOf(102),true,BigInteger.valueOf(103),false);
        transaction.executeWithoutResult(status -> repository.save(BigInteger.TEN,BigInteger.valueOf(11),added,switched,BigInteger.ZERO,false));
        assertEquals(2,writes.count("iam_department_leader"));
        assertEquals(102L,jdbc.queryForObject("SELECT member_id FROM iam_department_leader WHERE is_primary=TRUE",Long.class));
    }

    private long stamp(String table,String predicate) {
        return jdbc.queryForObject("SELECT stamp FROM " + table + " WHERE " + predicate,Long.class);
    }

    /** 观察实际 Executor.update，包含 INSERT/UPDATE/DELETE，排除夹具准备SQL。 */
    @Intercepts(@Signature(type=Executor.class,method="update",args={MappedStatement.class,Object.class}))
    private static class Writes implements Interceptor {
        private final List<String> sql = new ArrayList<>();
        @Override
        public Object intercept(Invocation invocation) throws Throwable {
            var statement = (MappedStatement) invocation.getArgs()[0];
            sql.add(statement.getBoundSql(invocation.getArgs()[1]).getSql().strip().replaceAll("\\s+"," ").toUpperCase());
            return invocation.proceed();
        }
        private long count(String table) {
            return sql.stream().filter(statement -> statement.matches(".*\\b" + table.toUpperCase() + "\\b.*")).count();
        }
    }
}
