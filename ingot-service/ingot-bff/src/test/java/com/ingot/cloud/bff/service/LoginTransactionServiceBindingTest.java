package com.ingot.cloud.bff.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.cloud.bff.config.BffProperties;
import com.ingot.framework.commons.constants.CacheConstants;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 浏览器绑定 Redis 键删除。
 *
 * @author jy
 * @since 1.0.0
 */
class LoginTransactionServiceBindingTest {

    @Test
    void deleteBinding_blank_doesNotTouchRedis() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        LoginTransactionService service = new LoginTransactionService(
                redis, new ObjectMapper(), new BffProperties());
        service.deleteBinding(null);
        service.deleteBinding(" ");
        verifyNoInteractions(redis);
    }

    @Test
    void deleteBinding_deletesAuthBindingKey() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        LoginTransactionService service = new LoginTransactionService(
                redis, new ObjectMapper(), new BffProperties());
        service.deleteBinding("bind-1");
        verify(redis).delete(CacheConstants.bffAuthBindingKey("bind-1"));
    }
}
