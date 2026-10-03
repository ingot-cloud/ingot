package com.ingot.cloud.iam.assignment;

import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.persistence.mapper.TenantScopeCandidateMapper;
import com.ingot.cloud.iam.persistence.mapper.TenantScopeCandidateSql;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * <p>租户范围候选分页、搜索和回显始终在同一租户及资源内。</p>
 * @author jy
 * @since 1.0.0
 */
class TenantScopeCandidateSqlTest {
    private static TenantScopeCandidateMapper mapper;

    @BeforeAll
    static void fixture() {
        DataSource source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY,tenant_id BIGINT,display_name VARCHAR(100),status VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY,tenant_id BIGINT,parent_id BIGINT,name VARCHAR(100))");
        jdbc.execute("CREATE TABLE iam_tenant_group(id BIGINT PRIMARY KEY,tenant_id BIGINT,name VARCHAR(100))");
        jdbc.update("INSERT INTO iam_tenant_member VALUES(1,10,'张三','ACTIVE'),(2,10,'张四','ACTIVE'),"
                + "(3,10,'停用','SUSPENDED'),(4,20,'别租户','ACTIVE')");
        jdbc.update("INSERT INTO iam_department VALUES(11,10,NULL,'研发部'),"
                + "(13,10,11,'平台组'),(12,20,NULL,'别租户部门')");
        jdbc.update("INSERT INTO iam_tenant_group VALUES(21,10,'研发组'),(22,20,'别租户组')");
        mapper = IamMybatisTestAccess.mapper(source, TenantScopeCandidateMapper.class);
    }

    @Test
    void memberSearchAndPagingStayWithinCurrentTenant() {
        var first = query(TenantScopeObjectResource.MEMBER, "%张%", List.of(), 0, 1);
        var second = query(TenantScopeObjectResource.MEMBER, "%张%", List.of(), 1, 1);
        assertEquals(2, mapper.count(first));
        assertEquals(BigInteger.ONE, mapper.page(first).getFirst().id());
        assertEquals(BigInteger.TWO, mapper.page(second).getFirst().id());
    }

    @Test
    void selectedReplayDoesNotRevealOtherTenantOrWrongResource() {
        var selected = List.of(BigInteger.ONE, BigInteger.valueOf(4));
        var member = query(TenantScopeObjectResource.MEMBER, "%", selected, 0, 20);
        assertEquals(1, mapper.count(member));
        assertEquals(BigInteger.ONE, mapper.page(member).getFirst().id());
        assertEquals(0, mapper.count(query(TenantScopeObjectResource.GROUP, "%", selected, 0, 20)));
        assertEquals(BigInteger.valueOf(11), mapper.page(query(TenantScopeObjectResource.DEPARTMENT,
                "%", List.of(), 0, 20)).getFirst().id());
    }

    @Test
    void departmentTreePagesBranchesAndReturnsAncestorPaths() {
        var roots = new TenantScopeCandidateSql.Query(TenantScopeObjectResource.DEPARTMENT,
                10, "%", List.of(), 0, 20, true, null);
        assertEquals(1, mapper.count(roots));
        assertEquals(BigInteger.valueOf(11), mapper.page(roots).getFirst().id());
        var children = new TenantScopeCandidateSql.Query(TenantScopeObjectResource.DEPARTMENT,
                10, "%", List.of(), 0, 20, true, BigInteger.valueOf(11));
        assertEquals(BigInteger.valueOf(13), mapper.page(children).getFirst().id());
        assertEquals("研发部 / 平台组", mapper.departmentPaths(10,
                List.of(BigInteger.valueOf(13))).getFirst().ancestorPath());
    }

    private static TenantScopeCandidateSql.Query query(TenantScopeObjectResource resource, String keyword,
            List<BigInteger> ids, int offset, int size) {
        return new TenantScopeCandidateSql.Query(resource, 10, keyword, ids, offset, size);
    }
}
