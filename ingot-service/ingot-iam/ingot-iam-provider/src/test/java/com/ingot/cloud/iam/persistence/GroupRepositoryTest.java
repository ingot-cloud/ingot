package com.ingot.cloud.iam.persistence;

import java.util.UUID;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>验证租户组有效人数按显式成员与部门来源展开后去重，且随任职变化。</p>
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
}
