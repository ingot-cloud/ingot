package com.ingot.cloud.iam.adapter;

import java.time.LocalDateTime;
import java.util.UUID;
import javax.sql.DataSource;

import com.ingot.cloud.iam.persistence.IamMybatisTestAccess;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证凭证端口只读写 {@code iam_account}，版本冲突时不覆盖。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class IamUserCredentialPortAdapterTest {
    private static DataSource dataSource;
    private JdbcTemplate jdbc;
    private IamUserCredentialPortAdapter adapter;

    @BeforeAll
    static void source() {
        dataSource = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    @BeforeEach
    void database() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP ALL OBJECTS");
        jdbc.execute("""
                CREATE TABLE iam_account(id BIGINT PRIMARY KEY, username VARCHAR(64), password_hash VARCHAR(300),
                  phone VARCHAR(32), email VARCHAR(128), enabled BOOLEAN, must_change_password BOOLEAN,
                  password_changed_at TIMESTAMP, last_login_at TIMESTAMP, version BIGINT, created_at TIMESTAMP,
                  updated_at TIMESTAMP, deleted_at TIMESTAMP)
                """);
        jdbc.execute("""
                CREATE TABLE account_lock_state(id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id BIGINT, user_type VARCHAR(16),
                  locked BOOLEAN, lock_type VARCHAR(16), lock_reason_code VARCHAR(64), lock_reason_detail VARCHAR(255),
                  locked_at TIMESTAMP, locked_until TIMESTAMP, operator_id BIGINT, operator_name VARCHAR(64),
                  failed_login_count INT, last_failed_at TIMESTAMP, created_at TIMESTAMP, updated_at TIMESTAMP)
                """);
        jdbc.execute("CREATE TABLE iam_tenant(id BIGINT PRIMARY KEY, name VARCHAR(128), enabled BOOLEAN, deleted_at TIMESTAMP, version BIGINT)");
        jdbc.execute("CREATE TABLE iam_tenant_member(id BIGINT PRIMARY KEY, account_id BIGINT, tenant_id BIGINT, status VARCHAR(16), version BIGINT)");
        jdbc.execute("CREATE TABLE iam_member_department(tenant_id BIGINT, member_id BIGINT, department_id BIGINT, is_primary BOOLEAN)");
        jdbc.update("""
                INSERT INTO iam_account(id,username,password_hash,phone,enabled,must_change_password,version)
                VALUES (1,'alice','old-hash','13800000000',TRUE,FALSE,0)
                """);
        adapter = new IamUserCredentialPortAdapter(IamMybatisTestAccess.accounts(dataSource),
                IamMybatisTestAccess.accountWrites(dataSource, () -> 99L));
    }

    @Test
    void passwordUpdateWritesIamAccountAndRejectsStaleVersion() {
        LocalDateTime changedAt = LocalDateTime.parse("2026-09-16T00:00:00");
        assertEquals("old-hash", adapter.getPasswordHash(1L, UserTypeEnum.ADMIN));
        assertTrue(adapter.updatePassword(1L, UserTypeEnum.ADMIN, "new-hash", changedAt, 0L, true));
        assertEquals("new-hash", jdbc.queryForObject("SELECT password_hash FROM iam_account WHERE id=1", String.class));
        assertEquals(Boolean.TRUE, jdbc.queryForObject("SELECT must_change_password FROM iam_account WHERE id=1",
                Boolean.class));
        assertEquals(1L, jdbc.queryForObject("SELECT version FROM iam_account WHERE id=1", Long.class));
        assertFalse(adapter.updatePassword(1L, UserTypeEnum.ADMIN, "ignored", changedAt, 0L, false));
        assertEquals("new-hash", jdbc.queryForObject("SELECT password_hash FROM iam_account WHERE id=1", String.class));
    }
}
