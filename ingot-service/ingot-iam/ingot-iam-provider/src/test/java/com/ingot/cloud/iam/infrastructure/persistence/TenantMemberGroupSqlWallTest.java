package com.ingot.cloud.iam.infrastructure.persistence;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.alibaba.druid.wall.WallConfig;
import com.alibaba.druid.wall.spi.MySqlWallProvider;
import com.ingot.cloud.iam.infrastructure.persistence.mapper.IamTenantGroupMemberMapper;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>用真实 Mapper 动态 SQL 验证成员所属组查询与 SQL 防护兼容，不放宽恒真条件检查。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class TenantMemberGroupSqlWallTest {

    @Test
    void unrestrictedCountAndPagePassWallWithTenantAndMemberParameters() {
        assertSafe(boundSql("countGroupsOfMember", null));
        assertSafe(boundSql("pageGroupsOfMember", null));
    }

    @Test
    void restrictedCountAndPageKeepParameterizedGroupFilter() {
        for (String method : Set.of("countGroupsOfMember", "pageGroupsOfMember")) {
            BoundSql sql = boundSql(method, Set.of(BigInteger.valueOf(501), BigInteger.valueOf(504)));
            assertSafe(sql);
            assertTrue(sql.getSql().matches("(?s).*mg\\.id IN\\s*\\(\\s*\\?\\s*,\\s*\\?\\s*\\).*"));
            assertEquals(2, sql.getParameterMappings().stream()
                    .filter(mapping -> mapping.getProperty().startsWith("__frch_groupId_")).count());
        }
    }

    @Test
    void wallStillRejectsTheReportedAlwaysTrueCondition() {
        String sql = boundSql("countGroupsOfMember", null).getSql().replaceFirst("(?i)\\s+WHERE\\s+1\\s*=\\s*1\\s*$", "");
        assertFalse(wall().checkValid(sql + " WHERE 1=1"));
    }

    private static BoundSql boundSql(String method, Set<BigInteger> groupIds) {
        var configuration = new Configuration();
        configuration.addMapper(IamTenantGroupMemberMapper.class);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("tenantId", BigInteger.TEN);
        parameters.put("memberId", BigInteger.valueOf(103));
        parameters.put("groupIds", groupIds);
        parameters.put("limit", 20);
        parameters.put("offset", 0);
        return configuration.getMappedStatement(IamTenantGroupMemberMapper.class.getName() + "." + method)
                .getBoundSql(parameters);
    }

    private static void assertSafe(BoundSql sql) {
        var result = wall().check(sql.getSql());
        assertTrue(result.getViolations().isEmpty(), () -> result.getViolations().toString());
        assertTrue(sql.getParameterMappings().stream()
                .anyMatch(mapping -> mapping.getProperty().equals("tenantId")));
        assertTrue(sql.getParameterMappings().stream()
                .anyMatch(mapping -> mapping.getProperty().equals("memberId")));
    }

    private static MySqlWallProvider wall() {
        var config = new WallConfig();
        config.setSelectWhereAlwayTrueCheck(true);
        return new MySqlWallProvider(config);
    }
}
