package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.evaluation.ObjectScopeClause;
import com.ingot.cloud.iam.persistence.mapper.IamRoleApplicationItemMapper;
import com.ingot.cloud.iam.persistence.mapper.IamRoleApplicationTaskMapper;
import com.ingot.framework.commons.model.iam.RoleApplicationItemStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * <p>使用真实 Mapper 验证聚合范围、枚举、缺失状态、当前进度和常数查询规模。</p>
 * @author jy
 * @since 1.0.0
 */
class RoleApplicationCountsTest {
    private QueryCountingDataSource source;
    private JdbcTemplate jdbc;
    private RoleApplicationRepository repository;
    private final List<BigInteger> tasks = IntStream.rangeClosed(1, 20).mapToObj(BigInteger::valueOf).toList();

    @BeforeEach
    void setup() {
        source = new QueryCountingDataSource(new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE iam_role_application_item(id BIGINT PRIMARY KEY, task_id BIGINT, assignment_id BIGINT, status VARCHAR(32))");
        long id = 1;
        for (var task : tasks) for (var state : RoleApplicationItemStatus.values()) for (long assignment : new long[]{1, 2})
            jdbc.update("INSERT INTO iam_role_application_item VALUES (?,?,?,?)", id++, task, assignment, state.name());
        repository = new RoleApplicationRepository(mock(IamRoleApplicationTaskMapper.class),
                IamMybatisTestAccess.mapper(source, IamRoleApplicationItemMapper.class));
        source.reset();
    }

    @Test
    void oneOrTwentyTasksUseOneQueryAndExactlyTheSameAssignmentBoundary() {
        var scope = ObjectScope.of(List.of(new ObjectScopeClause(Set.of(BigInteger.ONE), List.of())));
        var one = repository.counts(List.of(BigInteger.ONE), scope);
        assertEquals(1, source.count());
        source.reset();
        var twenty = repository.counts(tasks, scope);
        assertEquals(1, source.count());
        assertEquals(one.get(BigInteger.ONE), twenty.get(BigInteger.ONE));
        assertEquals(20, twenty.size());
        twenty.values().forEach(states -> {
            assertEquals(RoleApplicationItemStatus.values().length, states.size());
            states.values().forEach(total -> assertEquals(1L, total));
        });
        repository.counts(tasks, ObjectScope.all()).values().forEach(states -> states.values().forEach(total -> assertEquals(2L, total)));
        assertEquals(one.get(BigInteger.ONE), repository.counts(1, scope));
    }

    @Test
    void missingStatusesZeroFillAndUpdatesAreNotCached() {
        jdbc.update("DELETE FROM iam_role_application_item WHERE task_id=1 AND status='PENDING'");
        assertEquals(0L, repository.counts(1, ObjectScope.all()).get(RoleApplicationItemStatus.PENDING));
        jdbc.update("UPDATE iam_role_application_item SET status='PENDING' WHERE task_id=1 AND status='BLOCKED'");
        var after = repository.counts(1, ObjectScope.all());
        assertEquals(2L, after.get(RoleApplicationItemStatus.PENDING));
        assertEquals(0L, after.get(RoleApplicationItemStatus.BLOCKED));
    }

    @Test
    void emptyTaskPageAndEmptyScopeAvoidQueryAndDoNotExposeOutsideCounts() {
        assertTrue(repository.counts(List.of(), ObjectScope.all()).isEmpty());
        repository.counts(tasks, ObjectScope.none()).values().forEach(states -> states.values().forEach(total -> assertEquals(0L, total)));
        assertEquals(0, source.count());
        var invisible = ObjectScope.of(List.of(new ObjectScopeClause(Set.of(BigInteger.valueOf(999)), List.of())));
        repository.counts(tasks, invisible).values().forEach(states -> states.values().forEach(total -> assertEquals(0L, total)));
    }
}
