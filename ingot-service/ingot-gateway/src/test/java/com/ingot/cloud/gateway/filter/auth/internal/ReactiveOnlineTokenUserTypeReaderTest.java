package com.ingot.cloud.gateway.filter.auth.internal;

import com.ingot.framework.commons.constants.RedisKeyConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link ReactiveOnlineTokenUserTypeReader} 从 OnlineToken JSON 提取 userType。
 *
 * @author jy
 * @since 1.0.0
 */
class ReactiveOnlineTokenUserTypeReaderTest {

    @SuppressWarnings("unchecked")
    private final ReactiveValueOperations<String, String> valueOps = mock(ReactiveValueOperations.class);
    private final ReactiveStringRedisTemplate redis = mock(ReactiveStringRedisTemplate.class);
    private ReactiveOnlineTokenUserTypeReader reader;

    @BeforeEach
    void setUp() {
        when(redis.opsForValue()).thenReturn(valueOps);
        ObjectProvider<ReactiveStringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        reader = new ReactiveOnlineTokenUserTypeReader(provider);
    }

    @Test
    void readUserType_fromTypedObjectJson() {
        String jti = "jti-1";
        String json = "{\n"
                + "  \"@class\": \"com.ingot.framework.security.oauth2.server.authorization.OnlineToken\",\n"
                + "  \"userType\": \"0\",\n"
                + "  \"userId\": 123\n"
                + "}";
        when(valueOps.get(RedisKeyConstants.OnlineToken.jtiKey(jti))).thenReturn(Mono.just(json));

        assertEquals("0", reader.readUserType(jti).block());
    }

    @Test
    void readUserType_fromWrapperArrayJson() {
        String jti = "jti-2";
        String json = "[\"com.ingot.framework.security.oauth2.server.authorization.OnlineToken\","
                + "{\"userType\":\"1\",\"userId\":9}]";
        when(valueOps.get(RedisKeyConstants.OnlineToken.jtiKey(jti))).thenReturn(Mono.just(json));

        assertEquals("1", reader.readUserType(jti).block());
    }

    @Test
    void readUserType_miss_returnsEmpty() {
        when(valueOps.get(RedisKeyConstants.OnlineToken.jtiKey("missing"))).thenReturn(Mono.empty());
        assertNull(reader.readUserType("missing").block());
    }
}
