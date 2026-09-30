package com.ingot.cloud.iam.assignment;

import java.math.BigInteger;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateMapper;
import com.ingot.cloud.iam.persistence.mapper.AuthorizationCandidateSql;
import com.ingot.framework.commons.model.iam.AuthorizationCandidateKind;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>生产候选 SQL 的分页、名单、组全员约束与已选回显保持相同边界。</p>
 * @author jy
 * @since 1.0.0
 */
class PlatformCandidateSqlTest {
    private static DataSource source;
    private static AuthorizationCandidateMapper mapper;
    @BeforeAll
    static void fixture() {
        source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY,display_name VARCHAR(100),status VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_platform_group(id BIGINT PRIMARY KEY,name VARCHAR(100))");
        jdbc.execute("CREATE TABLE iam_platform_group_member(group_id BIGINT,member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_delegation_recipient_member(delegation_id BIGINT,platform_member_id BIGINT)");
        jdbc.update("INSERT INTO iam_platform_member VALUES(1,'张三','ACTIVE'),(2,'李四','ACTIVE'),(3,'王五','ACTIVE')");
        jdbc.update("INSERT INTO iam_platform_group VALUES(10,'允许组'),(11,'越界组'),(12,'空组')");
        jdbc.update("INSERT INTO iam_platform_group_member VALUES(10,1),(10,2),(11,1),(11,3)");
        jdbc.update("INSERT INTO iam_delegation_recipient_member VALUES(60,1),(60,2)");
        mapper = IamMybatisTestAccess.mapper(source, AuthorizationCandidateMapper.class);
    }
    @Test
    void groupCandidateMustBeNonEmptyAndEveryMemberMustBeReached() {
        var query = query(AuthorizationCandidateKind.GROUP, null, List.of(), 0, 20);
        assertEquals(1, mapper.count(query));
        assertEquals(BigInteger.TEN, mapper.page(query).getFirst().id());
    }
    @Test
    void selectedMemberReplayCannotEscapeAllowedIdsAndCountMatchesPage() {
        var query = query(AuthorizationCandidateKind.MEMBER, List.of(BigInteger.ONE, BigInteger.TWO),
                List.of(BigInteger.TWO, BigInteger.valueOf(3)), 0, 20);
        assertEquals(1, mapper.count(query));
        assertEquals(BigInteger.TWO, mapper.page(query).getFirst().id());
    }
    @Test
    void pagesContinueWithoutTruncatingAtOnePage() {
        var first = query(AuthorizationCandidateKind.MEMBER, null, List.of(), 0, 2);
        var second = query(AuthorizationCandidateKind.MEMBER, null, List.of(), 2, 2);
        assertEquals(3, mapper.count(first)); assertEquals(2, mapper.page(first).size());
        assertEquals(BigInteger.valueOf(3), mapper.page(second).getFirst().id());
    }
    private static AuthorizationCandidateSql.Query query(AuthorizationCandidateKind kind, List<BigInteger> allowed,
            List<BigInteger> ids, int offset, int size) {
        return new AuthorizationCandidateSql.Query(kind, BigInteger.ONE, BigInteger.valueOf(60), null, null,
                "%", ids, allowed, offset, size);
    }
}
