package com.ingot.cloud.iam.persistence;

import java.util.UUID;

import com.ingot.framework.security.credential.data.mapper.PasswordHistoryMapper;
import com.ingot.framework.security.credential.data.service.PasswordHistoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>使用真实框架服务及MyBatis验证精简索引后的密码历史读写契约。</p>
 * @author jy
 * @since 1.0.0
 */
class PasswordHistoryIndexCompatibilityTest {
    @Test
    void readsOverwritesChecksAndDeletesHistoryWithoutTheRedundantIndex() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID()
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE", "sa", "");
        var jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE TABLE password_history(id BIGINT AUTO_INCREMENT PRIMARY KEY,user_id BIGINT NOT NULL,"
                + "password_hash VARCHAR(255),sequence_number INT NOT NULL,version BIGINT,created_at TIMESTAMP,updated_at TIMESTAMP,"
                + "UNIQUE(user_id,sequence_number))");
        jdbc.execute("CREATE INDEX idx_created_at ON password_history(created_at)");
        var encoder = mock(PasswordEncoder.class);
        when(encoder.encode(any())).thenAnswer(invocation -> "encoded:" + invocation.getArgument(0));
        when(encoder.matches(any(), any())).thenAnswer(invocation ->
                ("encoded:" + invocation.getArgument(0)).equals(invocation.getArgument(1)));
        var service = new PasswordHistoryServiceImpl(IamMybatisTestAccess.mapper(source, PasswordHistoryMapper.class), encoder);
        service.saveHistory(1L, "fixture-a", 2);
        service.saveHistory(1L, "fixture-b", 2);
        service.saveHistory(2L, "fixture-other", 2);
        Long originalId = jdbc.queryForObject("SELECT id FROM password_history WHERE user_id=1 AND sequence_number=1", Long.class);
        service.saveHistory(1L, "fixture-c", 2);
        assertEquals(originalId, jdbc.queryForObject("SELECT id FROM password_history WHERE user_id=1 AND sequence_number=1", Long.class));
        assertEquals(3L, jdbc.queryForObject("SELECT version FROM password_history WHERE user_id=1 AND sequence_number=1", Long.class));
        assertEquals(2, service.getRecentHistory(1L, 2).size());
        assertTrue(service.isPasswordUsed(1L, "fixture-c", 2));
        assertFalse(service.isPasswordUsed(1L, "fixture-a", 2));
        assertFalse(service.isPasswordUsed(1L, "fixture-other", 2));
        service.deleteByUserId(1L);
        assertTrue(service.getRecentHistory(1L, 2).isEmpty());
        assertEquals(1, service.getRecentHistory(2L, 2).size());
    }
}
