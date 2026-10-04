package com.ingot.cloud.iam.persistence;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.ingot.framework.commons.model.iam.AssignmentEffectiveStatus;
import com.ingot.framework.commons.model.iam.SubjectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>通过生产 Mapper 验证平台计算状态筛选、分页计数和委派可见边界。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class AssignmentStatusRepositoryTest {
    private DriverManagerDataSource source;
    private JdbcTemplate jdbc;
    private AssignmentRepository assignments;

    @BeforeEach
    void fixture() {
        source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;INIT=SET TIME ZONE 'UTC'", "sa", "");
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY,display_name VARCHAR(100))");
        jdbc.execute("CREATE TABLE iam_platform_group(id BIGINT PRIMARY KEY,name VARCHAR(100))");
        jdbc.execute("CREATE TABLE iam_platform_group_member(group_id BIGINT,member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_role_definition(id BIGINT PRIMARY KEY,name VARCHAR(100),enabled BOOLEAN)");
        jdbc.execute("CREATE TABLE iam_role_revision(id BIGINT PRIMARY KEY,role_id BIGINT,revision BIGINT)");
        jdbc.execute("CREATE TABLE iam_delegation_grant(id BIGINT PRIMARY KEY,domain VARCHAR(16),tenant_id BIGINT,"
                + "platform_administrator_id BIGINT,status VARCHAR(16),valid_from TIMESTAMP,valid_until TIMESTAMP,"
                + "assignment_duration_mode VARCHAR(16),max_assignment_duration_seconds BIGINT,max_assignment_duration_nanos INT)");
        jdbc.execute("CREATE TABLE iam_delegation_role_revision(delegation_id BIGINT,revision_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_delegation_recipient_member(delegation_id BIGINT,platform_member_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_role_assignment(id BIGINT PRIMARY KEY,domain VARCHAR(16),tenant_id BIGINT,"
                + "subject_type VARCHAR(16),platform_member_id BIGINT,platform_group_id BIGINT,tenant_member_id BIGINT,"
                + "tenant_group_id BIGINT,revision_id BIGINT,revision_kind VARCHAR(32),scope_bindings VARCHAR(100),"
                + "delegation_grant_id BIGINT,valid_from TIMESTAMP,valid_until TIMESTAMP,status VARCHAR(16),"
                + "source VARCHAR(16),version BIGINT,created_at TIMESTAMP)");
        jdbc.execute("CREATE TABLE iam_authorization_audit(id BIGINT PRIMARY KEY,assignment_id BIGINT,"
                + "change_type VARCHAR(16),actor_member_id BIGINT,occurred_at TIMESTAMP)");
        jdbc.update("INSERT INTO iam_platform_member VALUES(1,'张三'),(2,'李四'),(3,'名单外成员'),(90,'管理员'),(91,'其他管理员')");
        jdbc.update("INSERT INTO iam_platform_group VALUES(10,'运维组'),(11,'空组'),(12,'包含管理员组')");
        jdbc.update("INSERT INTO iam_platform_group_member VALUES(10,1),(10,2),(12,1),(12,90)");
        jdbc.update("INSERT INTO iam_role_definition VALUES(1,'启用角色',TRUE),(2,'停用角色',FALSE)");
        jdbc.update("INSERT INTO iam_role_revision VALUES(11,1,1),(12,2,1)");
        var now = LocalDateTime.now(ZoneOffset.UTC);
        for (long id : List.of(60L, 61L, 62L)) {
            jdbc.update("INSERT INTO iam_delegation_grant VALUES(?,'PLATFORM',NULL,?,?,?,?,'LIMITED',86400,0)",
                    id, id == 61 ? 91 : 90, id == 62 ? "REVOKED" : "ACTIVE", now.minusDays(3), now.plusDays(3));
            jdbc.update("INSERT INTO iam_delegation_role_revision VALUES(?,11)", id);
            jdbc.update("INSERT INTO iam_delegation_recipient_member VALUES(?,1),(?,2)", id, id);
        }
        insert(1, false, 1, 11, null, now.minusHours(1), null, false);
        insert(2, false, 1, 11, null, now.plusDays(1), null, false);
        insert(3, false, 1, 11, null, now.minusDays(1), now.minusHours(1), false);
        insert(4, false, 1, 12, null, now.minusDays(1), now.minusHours(1), true);
        insert(5, false, 1, 12, null, now.plusDays(1), null, false);
        insert(6, false, 1, 11, 60L, now.minusHours(1), now.plusHours(1), false);
        insert(7, false, 1, 11, 62L, now.minusHours(1), now.plusHours(1), false);
        insert(8, true, 10, 11, 60L, now.minusHours(1), now.plusHours(1), false);
        insert(9, true, 11, 11, 60L, now.minusHours(1), now.plusHours(1), false);
        insert(10, false, 90, 11, 60L, now.minusHours(1), now.plusHours(1), false);
        insert(11, false, 1, 11, 61L, now.minusHours(1), now.plusHours(1), false);
        insert(12, false, 1, 11, null, now.minusHours(1), null, false);
        jdbc.update("UPDATE iam_role_assignment SET domain='TENANT',tenant_id=7 WHERE id=12");
        insert(13, false, 1, 11, 60L, now.minusHours(1), now.plusDays(2), false);
        insert(14, true, 12, 11, 60L, now.minusHours(1), now.plusHours(1), false);
        assignments = IamMybatisTestAccess.assignments(source);
    }

    @Test
    void fiveStatesUseDisplayPrecedenceAndDatabasePagination() {
        var expected = Map.of(
                AssignmentEffectiveStatus.ACTIVE, List.of(1L, 6L, 8L, 11L),
                AssignmentEffectiveStatus.PENDING, List.of(2L),
                AssignmentEffectiveStatus.EXPIRED, List.of(3L),
                AssignmentEffectiveStatus.REVOKED, List.of(4L),
                AssignmentEffectiveStatus.SOURCE_INVALID, List.of(5L, 7L, 9L, 10L, 13L, 14L));
        expected.forEach((state, ids) -> {
            var page = assignments.pagePlatform(1, 20, null, null, null, state);
            assertEquals(ids.size(), page.getTotal(), state.name());
            assertEquals(ids.stream().map(BigInteger::valueOf).toList(), page.getRecords().stream()
                    .map(row -> row.getId()).toList(), state.name());
        });
        var second = assignments.pagePlatform(2, 2, null, null, null, AssignmentEffectiveStatus.ACTIVE);
        assertEquals(4, second.getTotal());
        assertEquals(List.of(BigInteger.valueOf(8), BigInteger.valueOf(11)), second.getRecords().stream()
                .map(row -> row.getId()).toList());
        var overflow = assignments.pagePlatform(3, 2, null, null, null, AssignmentEffectiveStatus.ACTIVE);
        assertEquals(4, overflow.getTotal());
        assertTrue(overflow.getRecords().isEmpty());
        assertEquals(13, assignments.pagePlatform(1, 20, null, null).getTotal());
    }

    @Test
    void revokedAndExpiredPagingDoNotQueryRoleOrSourceTables() {
        jdbc.execute("DROP TABLE iam_role_definition");
        jdbc.execute("DROP TABLE iam_role_revision");
        jdbc.execute("DROP TABLE iam_delegation_grant");
        jdbc.execute("DROP TABLE iam_delegation_role_revision");
        jdbc.execute("DROP TABLE iam_delegation_recipient_member");
        jdbc.execute("DROP TABLE iam_platform_group_member");
        var revoked = assignments.pagePlatform(1, 1, null, null, null, AssignmentEffectiveStatus.REVOKED);
        assertEquals(1, revoked.getTotal());
        assertEquals(BigInteger.valueOf(4), revoked.getRecords().getFirst().getId());
        var expired = assignments.pagePlatform(1, 1, null, null, null, AssignmentEffectiveStatus.EXPIRED);
        assertEquals(1, expired.getTotal());
        assertEquals(BigInteger.valueOf(3), expired.getRecords().getFirst().getId());
    }

    @Test
    void subjectAndNameFiltersComposeWithOwnerAndState() {
        var named = assignments.pagePlatform(1, 20, SubjectType.MEMBER, "张", null, AssignmentEffectiveStatus.ACTIVE);
        assertEquals(3, named.getTotal());
        var owned = assignments.pageOwned(90, 1, 20, SubjectType.MEMBER, "张", null, AssignmentEffectiveStatus.ACTIVE);
        assertEquals(1, owned.getTotal());
        assertEquals(BigInteger.valueOf(6), owned.getRecords().getFirst().getId());
        var group = assignments.pageOwned(90, 1, 20, SubjectType.GROUP, "运维", null, AssignmentEffectiveStatus.ACTIVE);
        assertEquals(1, group.getTotal());
        assertEquals(BigInteger.valueOf(8), group.getRecords().getFirst().getId());
        var invalid = assignments.pageOwned(90, 1, 20, null, null, null, AssignmentEffectiveStatus.SOURCE_INVALID);
        assertEquals(5, invalid.getTotal());
        assertFalse(invalid.getRecords().stream().anyMatch(row -> row.getDelegationGrantId() == null));
        var second = assignments.pageOwned(90, 2, 1, null, null, null, AssignmentEffectiveStatus.ACTIVE);
        assertEquals(2, second.getTotal());
        assertEquals(BigInteger.valueOf(6), second.getRecords().getFirst().getId());
        var empty = assignments.pageOwned(91, 1, 20, null, null, null, AssignmentEffectiveStatus.REVOKED);
        assertEquals(0, empty.getTotal());
        assertTrue(empty.getRecords().isEmpty());
    }

    @Test
    void sourceChangesAndGroupExpansionMatchPresentationValidity() {
        jdbc.update("INSERT INTO iam_platform_group_member VALUES(10,3)");
        var invalid = assignments.pagePlatform(1, 20, SubjectType.GROUP, "运维", null,
                AssignmentEffectiveStatus.SOURCE_INVALID);
        assertEquals(1, invalid.getTotal());
        assertFalse(assignments.presentation(List.of(BigInteger.valueOf(8))).get(BigInteger.valueOf(8)).sourceValid());
        jdbc.update("UPDATE iam_delegation_grant SET status='REVOKED' WHERE id=60");
        assertEquals(0, assignments.pageOwned(90, 1, 20, null, null, null, AssignmentEffectiveStatus.ACTIVE).getTotal());
        assertFalse(assignments.presentation(List.of(BigInteger.valueOf(6))).get(BigInteger.valueOf(6)).sourceValid());
    }

    @Test
    void validityStartIsInclusiveAndEndExclusiveAtTheDatabaseClock() {
        new TransactionTemplate(new DataSourceTransactionManager(source)).execute(status -> {
            LocalDateTime now = jdbc.queryForObject("SELECT LOCALTIMESTAMP", LocalDateTime.class);
            insert(100, false, 1, 11, null, now, null, false);
            insert(101, false, 1, 11, null, now.minusHours(1), now, false);
            var active = assignments.pagePlatform(1, 20, null, null, null, AssignmentEffectiveStatus.ACTIVE);
            assertTrue(active.getRecords().stream().anyMatch(row -> row.getId().equals(BigInteger.valueOf(100))));
            var expired = assignments.pagePlatform(1, 20, null, null, null, AssignmentEffectiveStatus.EXPIRED);
            assertTrue(expired.getRecords().stream().anyMatch(row -> row.getId().equals(BigInteger.valueOf(101))));
            return null;
        });
    }

    private void insert(long id, boolean group, long subject, long revision, Long delegation,
            LocalDateTime from, LocalDateTime until, boolean revoked) {
        jdbc.update("INSERT INTO iam_role_assignment(id,domain,subject_type,platform_member_id,platform_group_id,"
                + "revision_id,revision_kind,scope_bindings,delegation_grant_id,valid_from,valid_until,status,source,version)"
                + " VALUES(?,'PLATFORM',?,?,?,?,'PLATFORM_CUSTOM','{}',?,?,?,?,'MANUAL',0)",
                id, group ? "GROUP" : "MEMBER", group ? null : subject, group ? subject : null, revision,
                delegation, from, until, revoked ? "REVOKED" : "ACTIVE");
    }
}
