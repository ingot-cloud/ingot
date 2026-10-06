package com.ingot.cloud.iam.organization;

import java.util.List;
import java.math.BigInteger;
import java.util.UUID;

import com.ingot.cloud.iam.evaluation.ObjectScope;
import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.cloud.iam.persistence.MemberQueryRepository;
import com.ingot.framework.commons.model.iam.MemberStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * <p>验证平台成员按 ID 精确回显，不把大页码当全量。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class MemberQueryRepositoryTest {
    private MemberQueryRepository members;
    private JdbcTemplate jdbc;

    @BeforeEach
    void database() {
        var dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE iam_account(id BIGINT PRIMARY KEY, username VARCHAR(64), phone VARCHAR(32),
                  email VARCHAR(128), deleted_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE iam_platform_member(id BIGINT PRIMARY KEY, account_id BIGINT, display_name VARCHAR(128),
                  avatar VARCHAR(256), phone VARCHAR(32), email VARCHAR(128), status VARCHAR(16), version BIGINT DEFAULT 0, created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, tenant_id BIGINT, status VARCHAR(16))");
        jdbc.execute("CREATE TABLE iam_department(id BIGINT PRIMARY KEY, tenant_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, member_id BIGINT, department_id BIGINT)");
        jdbc.execute("CREATE TABLE iam_platform_group_member(group_id BIGINT, member_id BIGINT)");
        jdbc.update("INSERT INTO iam_account VALUES (1,'alice',NULL,NULL,NULL),(2,'bob',NULL,NULL,NULL)");
        jdbc.update("INSERT INTO iam_platform_member(id,account_id,display_name,avatar,status,version,created_at,updated_at) VALUES (1001,1,'池鑫',NULL,'ACTIVE',0,NULL,NULL),"
                + "(1002,2,'杨紫微',NULL,'ACTIVE',0,NULL,NULL)");
        jdbc.update("INSERT INTO iam_platform_group_member VALUES (9,1002),(9,1001)");
        members = IamMybatisTestAccess.memberQueries(dataSource);
    }

    @Test
    void pagePlatformFiltersByIds() {
        var page = members.pagePlatform(ObjectScope.all(), 1, 20, null, null, List.of(1002L));
        assertEquals(1, page.getTotal());
        assertEquals(1002L, page.getRecords().getFirst().getId().longValue());
        assertEquals("杨紫微", page.getRecords().getFirst().getDisplayName());
    }

    @Test
    void pagePlatformWithoutIdsKeepsNameFilter() {
        var all = members.pagePlatform(ObjectScope.all(), 1, 20, null, MemberStatus.ACTIVE, List.of());
        assertEquals(2, all.getTotal());
        var byName = members.pagePlatform(ObjectScope.all(), 1, 20, "池", null, List.of());
        assertEquals(1, byName.getTotal());
        assertEquals("池鑫", byName.getRecords().getFirst().getDisplayName());
    }

    @Test
    void pagePlatformByGroupFiltersMembershipAndName() {
        var page = members.pagePlatformByGroup(ObjectScope.all(), 9, 1, 20, null);
        assertEquals(2, page.getTotal());
        var byName = members.pagePlatformByGroup(ObjectScope.all(), 9, 1, 20, "杨");
        assertEquals(1, byName.getTotal());
        assertEquals(1002L, byName.getRecords().getFirst().getId().longValue());
        var empty = members.pagePlatformByGroup(ObjectScope.all(), 8, 1, 20, null);
        assertEquals(0, empty.getTotal());
    }

    @Test
    void platformContactWritesDoNotModifyLoginContacts() {
        jdbc.update("UPDATE iam_account SET phone='13800000001',email='login@example.com' WHERE id=1");
        assertEquals(1, members.updatePlatform(1001, null, null, "13900000001", "contact@example.com", BigInteger.ZERO));
        assertEquals("13900000001", members.findPlatform(1001).getPhone());
        assertEquals("contact@example.com", members.findPlatform(1001).getEmail());
        assertEquals("13800000001", jdbc.queryForObject("SELECT phone FROM iam_account WHERE id=1", String.class));
        assertEquals("login@example.com", jdbc.queryForObject("SELECT email FROM iam_account WHERE id=1", String.class));
    }

    @Test
    void newMemberCopiesContactsOnceAndDoesNotFollowAccountChanges() {
        jdbc.update("UPDATE iam_account SET phone='13800000001',email='login@example.com' WHERE id=1");
        members.insertPlatform(1003, 1, "新成员", null);
        jdbc.update("UPDATE iam_account SET phone='13800000002',email='changed@example.com' WHERE id=1");
        assertEquals("13800000001", members.findPlatform(1003).getPhone());
        assertEquals("login@example.com", members.findPlatform(1003).getEmail());
    }

    @Test
    void nullKeepsContactsBlankClearsAndStaleVersionDoesNotWrite() {
        members.updatePlatform(1001, null, null, "13900000001", "contact@example.com", BigInteger.ZERO);
        assertEquals(1, members.updatePlatform(1001, "新显示名", null, null, null, BigInteger.ONE));
        assertEquals("13900000001", members.findPlatform(1001).getPhone());
        assertEquals(0, members.updatePlatform(1001, null, null, "other", "other", BigInteger.ZERO));
        assertEquals("contact@example.com", members.findPlatform(1001).getEmail());
        assertEquals(1, members.updatePlatform(1001, null, null, "", " ", BigInteger.TWO));
        assertNull(members.findPlatform(1001).getPhone());
        assertNull(members.findPlatform(1001).getEmail());
    }
}
