package com.ingot.cloud.iam.infrastructure.persistence;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ingot.cloud.iam.infrastructure.persistence.mapper.TenantAdministrationMapper;
import org.apache.ibatis.scripting.defaults.DefaultParameterHandler;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>执行真实租户分配计数 SQL，验证空可见范围与空委派来源均拒绝直接分配和跨租户记录。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class TenantAssignmentEmptyScopeTest {
    private DriverManagerDataSource source;
    private JdbcTemplate jdbc;
    private Configuration configuration;

    @BeforeEach
    void database() {
        source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE iam_role_definition(id BIGINT PRIMARY KEY,name VARCHAR(64))");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT,tenant_id BIGINT,display_name VARCHAR(64))");
        jdbc.execute("CREATE TABLE iam_tenant_group(id BIGINT,tenant_id BIGINT,name VARCHAR(64))");
        jdbc.execute("CREATE TABLE iam_role_assignment(id BIGINT PRIMARY KEY,tenant_id BIGINT,domain VARCHAR(16),"
                + "role_id BIGINT,tenant_member_id BIGINT,tenant_group_id BIGINT,subject_type VARCHAR(16),delegation_grant_id BIGINT)");
        jdbc.update("INSERT INTO iam_role_definition VALUES(1,'测试角色')");
        jdbc.update("INSERT INTO iam_role_assignment VALUES(101,10,'TENANT',1,1001,NULL,'MEMBER',NULL),"
                + "(102,10,'TENANT',1,1002,NULL,'MEMBER',501),(201,20,'TENANT',1,2001,NULL,'MEMBER',501),"
                + "(301,NULL,'PLATFORM',1,NULL,NULL,'MEMBER',NULL)");
        configuration = new Configuration();
        configuration.addMapper(TenantAdministrationMapper.class);
    }

    @Test
    void emptyVisibleAndSourceScopesReturnZeroEvenForNullableSources() throws Exception {
        assertEquals(2, count(null, null));
        assertEquals(0, count(List.of(), null));
        assertEquals(0, count(null, List.of()));
        assertEquals(0, count(List.of(BigInteger.valueOf(101)), List.of()));
        assertEquals(1, count(null, List.of(BigInteger.valueOf(501))));
        assertEquals(1, count(List.of(BigInteger.valueOf(101)), null));
        assertEquals(0, count(List.of(BigInteger.valueOf(201), BigInteger.valueOf(301)), null));
    }

    @Test
    void emptyMembershipNeverMatchesNullAndDoesNotSuppressAnIndependentVisibleBranch() {
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_assignment WHERE delegation_grant_id IN (NULL)", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM iam_role_assignment WHERE tenant_id=10 "
                + "AND (delegation_grant_id IN (NULL) OR id=101)", Integer.class));
    }

    private long count(List<BigInteger> visible, List<BigInteger> sources) throws Exception {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("tenantId", BigInteger.TEN);
        parameters.put("visible", visible);
        parameters.put("sources", sources);
        parameters.put("type", null);
        parameters.put("keyword", null);
        var mapped = configuration.getMappedStatement(TenantAdministrationMapper.class.getName() + ".assignmentsCount");
        var bound = mapped.getBoundSql(parameters);
        var wall = IamSqlWallTestInterceptor.provider().check(bound.getSql());
        assertTrue(wall.getViolations().isEmpty(), () -> wall.getViolations().toString());
        try (var connection = source.getConnection(); var statement = connection.prepareStatement(bound.getSql())) {
            new DefaultParameterHandler(mapped, parameters, bound).setParameters(statement);
            try (var result = statement.executeQuery()) {
                assertTrue(result.next());
                return result.getLong(1);
            }
        }
    }
}
