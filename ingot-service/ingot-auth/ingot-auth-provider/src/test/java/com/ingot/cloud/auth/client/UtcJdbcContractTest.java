package com.ingot.cloud.auth.client;

import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TimeZone;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>真实 Connector/J 会话、DATETIME/TIMESTAMP、默认时间及客户端密钥时间验证；仅创建连接私有临时表。</p>
 * @author jy
 * @since 1.0.0
 */
@EnabledIfEnvironmentVariable(named = "TIME_TEST_MYSQL_URL", matches = ".+")
class UtcJdbcContractTest {
    @Test
    void utcSessionAndOAuthClientInstantsRoundTripAcrossJvmTimeZones() throws Exception {
        TimeZone original = TimeZone.getDefault();
        Instant point = Instant.parse("2030-01-02T03:04:05.123456Z");
        try {
            for (String zone : new String[]{"UTC", "Asia/Shanghai", "America/New_York"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                try (var connection = DriverManager.getConnection(System.getenv("TIME_TEST_MYSQL_URL"),
                        System.getenv("TIME_TEST_MYSQL_USER"), System.getenv("TIME_TEST_MYSQL_PASSWORD"))) {
                    JdbcTemplate jdbc = new JdbcTemplate(new SingleConnectionDataSource(connection, true));
                    assertTrue(jdbc.queryForObject("SELECT @@session.time_zone", String.class).matches("UTC|\\+00:00"));
                    jdbc.execute("CREATE TEMPORARY TABLE time_contract_probe (dt DATETIME(6), ts TIMESTAMP(6), created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6))");
                    Instant before = Instant.now();
                    jdbc.update("INSERT INTO time_contract_probe(dt, ts) VALUES (?,?)", LocalDateTime.ofInstant(point, ZoneOffset.UTC), Timestamp.from(point));
                    Instant after = Instant.now();
                    jdbc.query("SELECT * FROM time_contract_probe", rs -> {
                        assertEquals(LocalDateTime.ofInstant(point, ZoneOffset.UTC), rs.getObject("dt", LocalDateTime.class));
                        assertEquals(point, rs.getTimestamp("dt").toInstant());
                        assertEquals(point, rs.getTimestamp("ts").toInstant());
                        Instant createdAt = rs.getObject("created_at", LocalDateTime.class).toInstant(ZoneOffset.UTC);
                        assertFalse(createdAt.isBefore(before.minusSeconds(1)));
                        assertFalse(createdAt.isAfter(after.plusSeconds(1)));
                    });
                    jdbc.execute("""
                            CREATE TEMPORARY TABLE oauth2_registered_client (
                              id VARCHAR(100) PRIMARY KEY, client_id VARCHAR(100), client_id_issued_at DATETIME(6),
                              client_secret VARCHAR(255), client_secret_expires_at DATETIME(6), client_name VARCHAR(200),
                              client_authentication_methods TEXT, authorization_grant_types TEXT, redirect_uris TEXT,
                              post_logout_redirect_uris TEXT, scopes TEXT, client_settings TEXT, token_settings TEXT,
                              deleted_at DATETIME(6))
                            """);
                    var repository = new InJdbcRegisteredClientRepository(jdbc);
                    var client = RegisteredClient.withId("utc-time-contract").clientId("utc-time-contract")
                            .clientName("UTC contract probe").clientIdIssuedAt(point).clientSecret("{noop}probe")
                            .clientSecretExpiresAt(point.plusSeconds(3600))
                            .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                            .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS).scope("probe").build();
                    repository.save(client);
                    var loaded = repository.findByClientId(client.getClientId());
                    assertNotNull(loaded);
                    assertEquals(point, loaded.getClientIdIssuedAt());
                    assertEquals(point.plusSeconds(3600), loaded.getClientSecretExpiresAt());

                    jdbc.execute("CREATE TEMPORARY TABLE time_contract_entity_probe (id BIGINT PRIMARY KEY, occurred_at DATETIME(6), expires_at TIMESTAMP(6), created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6))");
                    var configuration = new MybatisConfiguration();
                    configuration.setMapUnderscoreToCamelCase(true);
                    configuration.setEnvironment(new Environment("utc-probe", new JdbcTransactionFactory(),
                            new SingleConnectionDataSource(connection, true)));
                    configuration.addMapper(TimeProbeMapper.class);
                    var factory = new MybatisSqlSessionFactoryBuilder().build(configuration);
                    try (var session = factory.openSession(true)) {
                        var mapper = session.getMapper(TimeProbeMapper.class);
                        var entity = new TimeProbe();
                        entity.id = 1L;
                        entity.occurredAt = LocalDateTime.ofInstant(point, ZoneOffset.UTC);
                        entity.expiresAt = point;
                        assertEquals(1, mapper.insert(entity));
                        var restored = mapper.selectById(entity.id);
                        assertNotNull(restored);
                        assertEquals(entity.occurredAt, restored.occurredAt);
                        assertEquals(point, restored.expiresAt);
                        assertTrue(restored.createdAt.toInstant(ZoneOffset.UTC).isAfter(before.minusSeconds(1)));
                    }
                }
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }

    /** 独立业务实体：已有时间点 LocalDateTime 明确为 UTC，新字段可使用 Instant。 */
    @TableName("time_contract_entity_probe")
    public static class TimeProbe {
        /** 测试记录 ID。 */
        public Long id;
        /** UTC DATETIME 时间点。 */
        public LocalDateTime occurredAt;
        /** TIMESTAMP 时间点。 */
        public Instant expiresAt;
        /** 数据库默认生成的 UTC 时间。 */
        public LocalDateTime createdAt;
    }

    /** 使用框架真实 MyBatis-Plus 类型处理器完成实体读写。 */
    public interface TimeProbeMapper extends BaseMapper<TimeProbe> {
    }
}
