package com.ingot.framework.vc.common;

import java.time.Duration;
import java.time.Instant;
import java.util.TimeZone;
import java.util.UUID;

import com.ingot.framework.data.redis.config.InRedisTemplateConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证码使用 UTC Instant；Redis 保留独立类型编码和既有寿命。</p>
 * @author jy
 * @since 1.0.0
 */
class VerificationCodeUtcTest {
    @Test
    void expiryAndInternalSerializationAreIndependentOfJvmZone() {
        TimeZone original = TimeZone.getDefault();
        Instant start = Instant.parse("2026-10-08T01:00:00Z");
        Instant cutoff = start.plusSeconds(60);
        Instant before = cutoff.minusNanos(1);
        try {
            for (String zone : new String[]{"UTC", "Asia/Shanghai", "America/New_York"}) {
                TimeZone.setDefault(TimeZone.getTimeZone(zone));
                var template = new InRedisTemplateConfiguration().redisTemplate(mock(RedisConnectionFactory.class));
                @SuppressWarnings("unchecked")
                RedisSerializer<Object> serializer = (RedisSerializer<Object>) template.getValueSerializer();
                VC code;
                try (var instants = mockStatic(Instant.class, CALLS_REAL_METHODS)) {
                    instants.when(Instant::now).thenReturn(start);
                    code = VC.instance(VCType.SMS, "123456", 60);
                    assertEquals(cutoff, code.getExpireTime());
                    instants.when(Instant::now).thenReturn(before);
                    assertFalse(code.isExpired());
                    instants.when(Instant::now).thenReturn(cutoff);
                    assertTrue(code.isExpired());
                }
                VC loaded = (VC) serializer.deserialize(serializer.serialize(code));
                assertEquals(cutoff, loaded.getExpireTime());
                assertEquals(60, loaded.getExpireIn());
                assertEquals(VCType.SMS, loaded.getType());
                assertEquals("123456", loaded.getValue());
            }
        } finally {
            TimeZone.setDefault(original);
        }
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "TIME_TEST_REDIS_HOST", matches = ".+")
    void realRedisPreservesCodeInstantAndTtl() {
        var config = new RedisStandaloneConfiguration(System.getenv("TIME_TEST_REDIS_HOST"),
                Integer.parseInt(System.getenv().getOrDefault("TIME_TEST_REDIS_PORT", "6379")));
        String password = System.getenv("TIME_TEST_REDIS_PASSWORD");
        if (password != null && !password.isEmpty()) config.setPassword(password);
        var factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        factory.start();
        var template = new InRedisTemplateConfiguration().redisTemplate(factory);
        String key = "time-contract:probe:" + UUID.randomUUID();
        try {
            VC code = VC.instance(VCType.SMS, "123456", 60);
            template.opsForValue().set(key, code, Duration.ofSeconds(code.getExpireIn()));
            VC loaded = (VC) template.opsForValue().get(key);
            assertNotNull(loaded);
            assertEquals(code.getExpireTime(), loaded.getExpireTime());
            assertFalse(loaded.isExpired());
            Long ttl = template.getExpire(key);
            assertNotNull(ttl);
            assertTrue(ttl > 0 && ttl <= 60);
        } finally {
            template.delete(key);
            factory.destroy();
        }
    }
}
