package com.ingot.framework.security.account.adapter.port;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import com.ingot.framework.commons.model.security.UserTypeEnum;
import com.ingot.framework.security.account.adapter.entity.AccountLockStateEntity;
import com.ingot.framework.security.account.adapter.mapper.AccountLockStateMapper;
import com.ingot.framework.security.account.domain.model.enums.LockType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>验证锁定状态没有自动填充处理器时仍完整保存 UTC 创建与更新时间。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class DefaultLockStatePortAdapterTest {
    private final AccountLockStateMapper mapper = mock(AccountLockStateMapper.class);
    private final DefaultLockStatePortAdapter adapter = new DefaultLockStatePortAdapter(mapper);

    @Test
    void firstManualLockWritesRequiredUtcTimestamps() {
        Instant before = Instant.now();
        adapter.updateLockStatus(1L, UserTypeEnum.ADMIN, true, null, null, LockType.MANUAL, 2L, null);
        Instant after = Instant.now();
        var captured = ArgumentCaptor.forClass(AccountLockStateEntity.class);
        verify(mapper).insert(captured.capture());
        var entity = captured.getValue();
        assertNotNull(entity.getCreatedAt());
        assertEquals(entity.getCreatedAt(), entity.getUpdatedAt());
        assertEquals(entity.getCreatedAt(), entity.getLockedAt());
        Instant stored = entity.getCreatedAt().toInstant(ZoneOffset.UTC);
        assertFalse(stored.isBefore(before));
        assertFalse(stored.isAfter(after));
    }

    @Test
    void updatePreservesCreationAndAdvancesUtcUpdateTime() {
        var existing = new AccountLockStateEntity();
        existing.setId(10L);
        LocalDateTime created = LocalDateTime.now(ZoneOffset.UTC).minusDays(1);
        existing.setCreatedAt(created);
        existing.setUpdatedAt(created);
        when(mapper.selectOne(any())).thenReturn(existing);
        adapter.updateLockStatus(1L, UserTypeEnum.ADMIN, false, null, null, LockType.MANUAL, 2L, null);
        verify(mapper).updateById(existing);
        assertEquals(created, existing.getCreatedAt());
        assertTrue(existing.getUpdatedAt().isAfter(created));
    }
}
