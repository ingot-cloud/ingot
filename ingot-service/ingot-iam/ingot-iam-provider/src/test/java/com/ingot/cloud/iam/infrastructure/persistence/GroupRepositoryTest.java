package com.ingot.cloud.iam.infrastructure.persistence;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;

import com.ingot.cloud.iam.domain.authorization.ObjectScope;
import com.ingot.cloud.iam.domain.authorization.ObjectScopeClause;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证租户组有效成员及成员所属组按直接与部门来源去重，分页、计数和租户范围一致。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class GroupRepositoryTest {
    private JdbcTemplate jdbc;
    private GroupRepository groups;
    /** 整个类共用一个库，避免每个用例重建一份 MyBatis 配置。 */
    private static DataSource dataSource;

    @BeforeAll
    static void source() {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    @BeforeEach
    void database() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT, tenant_id BIGINT, parent_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, department_id BIGINT, member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_group_member(tenant_id BIGINT, group_id BIGINT, member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_group_department(tenant_id BIGINT, group_id BIGINT,"
                + " department_id BIGINT, include_descendants BOOLEAN DEFAULT FALSE)");
        jdbc.execute("CREATE TABLE iam_tenant_group(id BIGINT, tenant_id BIGINT, name VARCHAR(100),"
                + " description VARCHAR(100), version BIGINT DEFAULT 0)");
        // 组织 10 的部门树为 700 → 710 → 720，另有平行部门 800。
        jdbc.update("INSERT INTO iam_department VALUES (700,10,NULL),(710,10,700),(720,10,710),(800,10,NULL)");
        jdbc.update("INSERT INTO iam_member_department VALUES (10,700,101),(10,710,102),(10,720,103),(10,800,104)");
        groups = IamMybatisTestAccess.groups(dataSource);
    }

    @Test
    void membershipCountsOnlyExplicitMembersWithoutDepartmentSources() {
        jdbc.update("INSERT INTO iam_tenant_group_member VALUES (10,502,104)");
        assertEquals(1, groups.countTenantMembership(10, 502));
    }

    @Test
    void membershipCountsDescendantDepartmentsOnlyWhenRequested() {
        jdbc.update("INSERT INTO iam_tenant_group_department VALUES (10,502,710,FALSE)");
        assertEquals(1, groups.countTenantMembership(10, 502));

        jdbc.update("UPDATE iam_tenant_group_department SET include_descendants=TRUE");
        assertEquals(2, groups.countTenantMembership(10, 502));
    }

    @Test
    void membershipDeduplicatesMembersReachedByBothSources() {
        jdbc.update("INSERT INTO iam_tenant_group_department VALUES (10,502,700,TRUE)");
        jdbc.update("INSERT INTO iam_tenant_group_member VALUES (10,502,101),(10,502,104)");
        // 部门来源覆盖 101/102/103，显式成员再加 104，101 不重复计数。
        assertEquals(4, groups.countTenantMembership(10, 502));
    }

    @Test
    void membershipFollowsAssignmentChangesWithoutRewritingTheGroup() {
        jdbc.update("INSERT INTO iam_tenant_group_department VALUES (10,502,710,TRUE)");
        assertEquals(2, groups.countTenantMembership(10, 502));

        jdbc.update("DELETE FROM iam_member_department WHERE tenant_id=10 AND member_id=103");
        jdbc.update("INSERT INTO iam_member_department VALUES (10,710,104)");

        assertEquals(2, groups.countTenantMembership(10, 502));
    }

    @Test
    void membershipIgnoresOtherTenants() {
        jdbc.update("INSERT INTO iam_department VALUES (900,20,NULL)");
        jdbc.update("INSERT INTO iam_member_department VALUES (20,900,201)");
        jdbc.update("INSERT INTO iam_tenant_group_department VALUES (20,502,900,TRUE)");
        jdbc.update("INSERT INTO iam_tenant_group_member VALUES (20,502,201)");

        assertEquals(0, groups.countTenantMembership(10, 502));
    }

    @Test
    void memberGroupsPageDeduplicatesDirectAndDepartmentSources() {
        memberGroups();
        var first = groups.pageTenantByMember(10, 103, 1, 2, ObjectScope.all());
        assertEquals(5, first.getTotal());
        assertEquals(List.of(BigInteger.valueOf(501), BigInteger.valueOf(502)),
                first.getRecords().stream().map(row -> row.getId()).toList());
        var last = groups.pageTenantByMember(10, 103, 3, 2, ObjectScope.all());
        assertEquals(5, last.getTotal());
        assertEquals(List.of(BigInteger.valueOf(506)), last.getRecords().stream().map(row -> row.getId()).toList());
    }

    @Test
    void memberGroupsCountAndRowsShareVisibilityAndTenantBoundary() {
        memberGroups();
        var scope = ObjectScope.of(List.of(new ObjectScopeClause(
                Set.of(BigInteger.valueOf(501), BigInteger.valueOf(504), BigInteger.valueOf(601)), List.of())));
        var page = groups.pageTenantByMember(10, 103, 1, 20, scope);
        assertEquals(2, page.getTotal());
        assertEquals(List.of(BigInteger.valueOf(501), BigInteger.valueOf(504)),
                page.getRecords().stream().map(row -> row.getId()).toList());
    }

    @Test
    void memberWithoutGroupsOrVisibleGroupScopeGetsEmptyPage() {
        memberGroups();
        var noMembership = groups.pageTenantByMember(10, 999, 1, 20, ObjectScope.all());
        assertEquals(0, noMembership.getTotal());
        assertTrue(noMembership.getRecords().isEmpty());
        var noVisibility = groups.pageTenantByMember(10, 103, 1, 20, ObjectScope.none());
        assertEquals(0, noVisibility.getTotal());
        assertTrue(noVisibility.getRecords().isEmpty());
    }

    private void memberGroups() {
        jdbc.update("INSERT INTO iam_tenant_group(id,tenant_id,name) VALUES"
                + "(501,10,'直接组'),(502,10,'上级含下级'),(503,10,'上级不含下级'),"
                + "(504,10,'当前部门'),(505,10,'根部门'),(506,10,'重叠来源'),(601,20,'其他组织')");
        jdbc.update("INSERT INTO iam_tenant_group_member VALUES (10,501,103),(10,506,103),(20,601,103)");
        jdbc.update("INSERT INTO iam_tenant_group_department VALUES"
                + "(10,502,710,TRUE),(10,503,710,FALSE),(10,504,720,FALSE),"
                + "(10,505,700,TRUE),(10,506,710,TRUE),(20,601,720,TRUE)");
    }
}
