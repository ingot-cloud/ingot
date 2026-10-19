package com.ingot.cloud.iam.organization;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ObjectScopeClause;
import com.ingot.cloud.iam.persistence.DepartmentQueryRepository;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.persistence.mapper.IamDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamMemberDepartmentMapper;
import com.ingot.cloud.iam.persistence.mapper.IamTenantMemberMapper;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * <p>通过真实SQL验证部门详情的父名称查询遵守租户及读取范围。</p>
 * @author jy
 * @since 1.0.0
 */
class DepartmentDetailParentTest {
    @Test
    void parentNameLookupCannotCrossTenantOrReadScope() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY,tenant_id BIGINT,parent_id BIGINT,name VARCHAR(100),sort_order INT,version BIGINT,created_at TIMESTAMP,updated_at TIMESTAMP)");
        jdbc.update("INSERT INTO iam_department(id,tenant_id,name) VALUES(1,10,'公司'),(2,10,'研发'),(3,20,'其他租户')");
        var repository = new DepartmentQueryRepository(IamMybatisTestAccess.mapper(source, IamDepartmentMapper.class),
                mock(IamMemberDepartmentMapper.class), mock(IamTenantMemberMapper.class));
        var scope = ObjectScope.of(List.of(new ObjectScopeClause(Set.of(BigInteger.ONE), List.of())));
        assertEquals("公司", repository.findVisible(10, 1, scope).getName());
        assertNull(repository.findVisible(10, 2, scope));
        assertNull(repository.findVisible(10, 1, ObjectScope.none()));
        assertNull(repository.findVisible(10, 3, ObjectScope.all()));
    }
}
