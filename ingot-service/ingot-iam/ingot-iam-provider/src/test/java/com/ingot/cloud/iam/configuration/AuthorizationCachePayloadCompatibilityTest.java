package com.ingot.cloud.iam.configuration;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import com.ingot.framework.core.config.JacksonConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.iam.application.authorization.evaluation.AuthorizationEvaluator;
import com.ingot.cloud.iam.domain.authorization.AuthorizationView;
import com.ingot.framework.cache.registry.LayeredCacheRegistry;
import com.ingot.framework.cache.source.CacheSourceHolder;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * <p>验证类型迁移前的无类名 JSON 授权缓存仍可读写，保持命名空间与过期上界。</p>
 * @author jy
 * @since 1.0.0
 */
class AuthorizationCachePayloadCompatibilityTest {
    private static final String KEY = "PLATFORM::1:99";
    private static final String LEGACY_PAYLOAD = """
            {"actionCodes":["iam-platform:application:read"],
             "governedCodes":["iam-platform:application:read"],
             "scopes":{"iam-platform:application:read":{"clauses":[{
                "all":false,"self":false,"memberDepartments":false,"memberDepartmentDescendants":false,
                "departmentIds":[],"departmentDescendants":false,"objectIds":["42"]}],"empty":false}},
             "version":"existing-fingerprint","expiresAt":"2030-01-01T00:00:00Z",
             "fieldSources":[{"assignmentId":71,"groupId":null,"delegationId":null,
                "revisionId":"12","actionCode":"iam-platform:application:read","clauses":[]}],
             "platformAdministrator":false,"tenantOwner":false}
            """;

    @Test
    @SuppressWarnings("unchecked")
    void readsPreviousPayloadWithoutLoadingAndWritesSameShapeWithBoundedTtl() throws Exception {
        var properties = new IamAuthorizationCacheProperties();
        properties.setL1Enabled(false);
        properties.setL2Enabled(true);
        properties.setL2Ttl(Duration.ofSeconds(30));
        var builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().customizer(List.of(), List.of()).customize(builder);
        ObjectMapper mapper = builder.build();
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        String redisKey = properties.getRedisKeyPrefix() + "platform-super-admin-v3:" + KEY;
        when(values.get(redisKey)).thenReturn(LEGACY_PAYLOAD, (String) null);
        var evaluator = mock(AuthorizationEvaluator.class);
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("redis", redis);
        beans.registerSingleton("mapper", mapper);
        var cache = new AuthorizationCacheConfiguration().iamAuthorizationViewCache(properties, evaluator,
                beans.getBeanProvider(StringRedisTemplate.class), beans.getBeanProvider(ObjectMapper.class),
                beans.getBeanProvider(LayeredCacheRegistry.class), new CacheSourceHolder());

        AuthorizationView previous = cache.get(KEY);
        assertEquals("existing-fingerprint", previous.version());
        assertEquals("71", previous.fieldSources().getFirst().assignmentId().toString());
        assertEquals("42", previous.scope("iam-platform:application:read").clauses().getFirst().objectIds().getFirst());
        assertFalse(previous.platformAdministrator());
        assertFalse(previous.tenantOwner());
        assertFalse(previous.expired(Instant.parse("2029-01-01T00:00:00Z")));
        verifyNoInteractions(evaluator);

        when(evaluator.evaluateRaw(KEY)).thenReturn(previous);
        assertEquals(previous, cache.get(KEY));
        var payload = ArgumentCaptor.forClass(String.class);
        verify(values).set(eq(redisKey), payload.capture(), eq(Duration.ofSeconds(30)));
        assertEquals(mapper.readTree(LEGACY_PAYLOAD), mapper.readTree(payload.getValue()));
        assertFalse(payload.getValue().contains("com.ingot."));
    }
}
