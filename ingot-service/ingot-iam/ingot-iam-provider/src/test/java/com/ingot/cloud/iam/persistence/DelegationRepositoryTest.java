package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>验证委派管理员名称在可信管理域内先筛选再分页，且按页批量回显。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class DelegationRepositoryTest {
    private static DriverManagerDataSource source;
    private JdbcTemplate jdbc;
    private DelegationRepository delegations;

    @BeforeAll
    static void source() {
        source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    @BeforeEach
    void database() {
        jdbc = new JdbcTemplate(source);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("CREATE TABLE iam_platform_member (id BIGINT PRIMARY KEY, display_name VARCHAR(255))");
        jdbc.execute("CREATE TABLE iam_tenant_member (id BIGINT PRIMARY KEY, tenant_id BIGINT, display_name VARCHAR(255))");
        jdbc.execute("CREATE TABLE iam_delegation_grant (id BIGINT PRIMARY KEY, domain VARCHAR(20), tenant_id BIGINT,"
                + " platform_administrator_id BIGINT, tenant_administrator_id BIGINT, valid_from TIMESTAMP,"
                + " valid_until TIMESTAMP, assignment_duration_mode VARCHAR(16) DEFAULT 'LIMITED', max_assignment_duration_seconds BIGINT, max_assignment_duration_nanos INT,"
                + " status VARCHAR(20), version BIGINT, created_at TIMESTAMP)");
        jdbc.update("INSERT INTO iam_platform_member VALUES (101,'张三'),(102,'张_四'),(103,'李四')");
        jdbc.update("INSERT INTO iam_tenant_member VALUES (201,7,'张五')");
        jdbc.update("INSERT INTO iam_delegation_grant (id,domain,platform_administrator_id,status,version)"
                + " VALUES (1,'PLATFORM',101,'ACTIVE',0),(2,'PLATFORM',102,'ACTIVE',0),"
                + " (3,'PLATFORM',103,'ACTIVE',0)");
        jdbc.update("INSERT INTO iam_delegation_grant (id,domain,tenant_id,tenant_administrator_id,status,version)"
                + " VALUES (4,'TENANT',7,201,'ACTIVE',0)");
        delegations = IamMybatisTestAccess.delegations(source);
    }

    @Test
    void platformNameFilterAppliesBeforePageAndCount() {
        var first = delegations.page(AuthorizationDomain.PLATFORM, null, 1, 1, "张");
        var second = delegations.page(AuthorizationDomain.PLATFORM, null, 2, 1, "张");
        assertEquals(2, first.getTotal());
        assertEquals(BigInteger.valueOf(2), first.getRecords().get(0).getId());
        assertEquals(2, second.getTotal());
        assertEquals(BigInteger.ONE, second.getRecords().get(0).getId());
        assertEquals(3, delegations.page(AuthorizationDomain.PLATFORM, null, 1, 20).getTotal());
    }

    @Test
    void wildcardCharactersRemainLiteralAndNamesStayWithinDomain() {
        var literal = delegations.page(AuthorizationDomain.PLATFORM, null, 1, 20, "张_");
        assertEquals(1, literal.getTotal());
        assertEquals(BigInteger.valueOf(2), literal.getRecords().get(0).getId());
        assertEquals(1, delegations.page(AuthorizationDomain.TENANT, 7L, 1, 20).getTotal());
        assertEquals(Map.of(BigInteger.valueOf(102), "张_四"), delegations.platformAdministratorNames(
                List.of(BigInteger.valueOf(102))));
    }
}
