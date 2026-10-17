package com.ingot.cloud.iam.organization;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.persistence.QueryCountingDataSource;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证部门直属 ACTIVE 成员按当前页批量计数，暂停成员与他部门不计入。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class DepartmentQueryRepositoryTest {
    private DepartmentQueryRepository departments;
    private QueryCountingDataSource counting;

    @BeforeEach
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        counting = new QueryCountingDataSource(dataSource);
        JdbcTemplate jdbc = new JdbcTemplate(counting);
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, tenant_id BIGINT, status VARCHAR(16))");
        jdbc.execute("""
                CREATE TABLE iam_member_department(
                  tenant_id BIGINT, member_id BIGINT, department_id BIGINT, is_primary BOOLEAN,
                  PRIMARY KEY(tenant_id, member_id, department_id))
                """);
        jdbc.update("INSERT INTO iam_tenant_member VALUES (101,10,'ACTIVE'),(102,10,'ACTIVE'),"
                + "(103,10,'SUSPENDED'),(104,10,'ACTIVE'),(105,10,'REMOVED')");
        jdbc.update("INSERT INTO iam_member_department VALUES (10,101,11,TRUE),(10,102,11,FALSE),"
                + "(10,103,11,FALSE),(10,104,12,TRUE),(10,105,11,FALSE)");
        departments = new DepartmentQueryRepository(
                IamMybatisTestAccess.mapper(counting, IamDepartmentMapper.class),
                IamMybatisTestAccess.mapper(counting, IamMemberDepartmentMapper.class));
    }

    @Test
    void activeMemberCountsIgnoresSuspendedAndOtherDepartments() {
        counting.reset();
        Map<Long, Integer> counts = departments.activeMemberCounts(10, List.of(11L, 12L, 13L));
        assertEquals(2, counts.get(11L));
        assertEquals(1, counts.get(12L));
        assertTrue(!counts.containsKey(13L));
        assertEquals(1, counting.from("iam_member_department"));
    }

    @Test
    void emptyIdsSkipDatabase() {
        counting.reset();
        assertEquals(Map.of(), departments.activeMemberCounts(10, List.of()));
        assertEquals(0, counting.count());
    }
}
