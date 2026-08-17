package com.ingot.framework.security.account.domain.port.outbound.redis;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import com.ingot.framework.commons.constants.RedisKeyConstants;
import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.domain.config.AccountLockSignalProperties;
import com.ingot.framework.security.account.domain.model.AccountLockSignal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link RedisAccountLockSignalAdapter} 双 Key 写删与查询单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class RedisAccountLockSignalAdapterTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> valueOps = mock(ValueOperations.class);
    private final AccountLockSignalProperties properties = new AccountLockSignalProperties();

    private RedisAccountLockSignalAdapter adapter;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        adapter = new RedisAccountLockSignalAdapter(redisTemplate, properties);
    }

    @Test
    void writeLocked_writesUidAndNameKeys() {
        LocalDateTime until = LocalDateTime.now().plusMinutes(30);
        adapter.writeLocked(new AccountLockSignal(1L, UserTypeEnum.ADMIN, "admin", until));

        String uidKey = RedisKeyConstants.AccountLock.uidKey("0", 1L);
        String nameKey = RedisKeyConstants.AccountLock.nameKey("0", "admin");
        verify(valueOps).set(eq(uidKey), anyString(), anyLong(), eq(TimeUnit.SECONDS));
        verify(valueOps).set(eq(nameKey), anyString(), anyLong(), eq(TimeUnit.SECONDS));
    }

    @Test
    void clearLocked_deletesBothKeys() {
        adapter.clearLocked(new AccountLockSignal(1L, UserTypeEnum.ADMIN, "admin", null));

        verify(redisTemplate).delete(RedisKeyConstants.AccountLock.uidKey("0", 1L));
        verify(redisTemplate).delete(RedisKeyConstants.AccountLock.nameKey("0", "admin"));
    }

    @Test
    void isLocked_readsHasKey() {
        when(redisTemplate.hasKey(RedisKeyConstants.AccountLock.uidKey("0", 1L))).thenReturn(true);
        when(redisTemplate.hasKey(RedisKeyConstants.AccountLock.nameKey("0", "admin"))).thenReturn(false);

        assertTrue(adapter.isLockedByUserId(UserTypeEnum.ADMIN, 1L));
        assertFalse(adapter.isLockedByUsername(UserTypeEnum.ADMIN, "admin"));
    }

    @Test
    void redisKeyConstants_assembleExpectedShape() {
        assertEquals("in:sec:account:locked:uid:0:9",
                RedisKeyConstants.AccountLock.uidKey("0", 9L));
        assertEquals("in:sec:account:locked:name:0:alice",
                RedisKeyConstants.AccountLock.nameKey("0", "alice"));
    }
}
