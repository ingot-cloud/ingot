package com.ingot.cloud.iam.application.tenant;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.domain.authorization.ObjectScopeClause;
import com.ingot.cloud.iam.infrastructure.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.infrastructure.persistence.GroupRepository;
import com.ingot.cloud.iam.infrastructure.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.infrastructure.persistence.MemberQueryRepository;
import com.ingot.cloud.iam.infrastructure.persistence.QueryCountingDataSource;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantMemberMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>通过真实 SQL 验证工作台计数的租户、对象范围、成员状态及多部门去重。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class TenantWorkbenchCountTest {
    private MemberQueryRepository members;
    private DepartmentQueryRepository departments;
    private GroupRepository groups;
    private QueryCountingDataSource counting;

    @BeforeEach
    void database() {
        counting = new QueryCountingDataSource(new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        var jdbc = new JdbcTemplate(counting);
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, tenant_id BIGINT, status VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY, tenant_id BIGINT, parent_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, member_id BIGINT, department_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_group(id BIGINT PRIMARY KEY, tenant_id BIGINT)");
        jdbc.update("INSERT INTO iam_tenant_member VALUES (101,10,'ACTIVE'),(102,10,'SUSPENDED'),"
                + "(103,10,'REMOVED'),(201,20,'ACTIVE')");
        jdbc.update("INSERT INTO iam_department VALUES (10,10,NULL),(11,10,10),(12,10,10),(20,20,NULL),(21,20,20)");
        jdbc.update("INSERT INTO iam_member_department VALUES (10,101,11),(10,101,12),(10,102,11),(20,201,21)");
        jdbc.update("INSERT INTO iam_tenant_group VALUES (1,10),(2,10),(3,20)");
        members = IamMybatisTestAccess.memberQueries(counting);
        groups = IamMybatisTestAccess.groups(counting);
        departments = new DepartmentQueryRepository(IamMybatisTestAccess.mapper(counting, IamDepartmentMapper.class),
                IamMybatisTestAccess.mapper(counting, IamMemberDepartmentMapper.class),
                IamMybatisTestAccess.mapper(counting, IamTenantMemberMapper.class));
    }

    @Test
    void countsRemainTenantBoundAndExcludeRemovedMembersAndRootDepartments() {
        assertEquals(2L, members.countTenant(10, ObjectScope.all()));
        assertEquals(1L, members.countTenant(20, ObjectScope.all()));
        assertEquals(2L, departments.countDepartments(10, ObjectScope.all()));
        assertEquals(1L, departments.countDepartments(20, ObjectScope.all()));
        assertEquals(2L, groups.countTenant(10, ObjectScope.all()));
        assertEquals(1L, groups.countTenant(20, ObjectScope.all()));
    }

    @Test
    void multipleDepartmentDimensionsAreIntersectedWithoutCountingMembershipTwice() {
        var intersection = ObjectScope.of(List.of(new ObjectScopeClause(Set.of(),
                List.of(Set.of(BigInteger.valueOf(11)), Set.of(BigInteger.valueOf(12))))));
        counting.reset();
        assertEquals(1L, members.countTenant(10, intersection));
        assertEquals(1, counting.from("iam_tenant_member"));
        assertEquals(0L, members.countTenant(20, intersection));
    }

    @Test
    void objectAndEmptyScopesHaveTheSameSemanticsAsTheLists() {
        var member = ObjectScope.of(List.of(new ObjectScopeClause(Set.of(BigInteger.valueOf(102)), List.of())));
        var department = ObjectScope.of(List.of(new ObjectScopeClause(
                Set.of(BigInteger.TEN, BigInteger.valueOf(11)), List.of())));
        assertEquals(1L, members.countTenant(10, member));
        assertEquals(1L, departments.countDepartments(10, department));
        assertEquals(0L, members.countTenant(10, ObjectScope.none()));
        assertEquals(0L, departments.countDepartments(10, ObjectScope.none()));
    }
}
