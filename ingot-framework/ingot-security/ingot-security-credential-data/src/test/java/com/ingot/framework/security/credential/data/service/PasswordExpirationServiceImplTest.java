package com.ingot.framework.security.credential.data.service;

import com.ingot.framework.security.credential.data.mapper.PasswordExpirationMapper;
import com.ingot.framework.security.credential.model.domain.PasswordExpiration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link PasswordExpirationServiceImpl} 过期记录初始化/更新/强制改密标记单元测试。
 *
 * @author jy
 * @since 1.0.0
 */
class PasswordExpirationServiceImplTest {

    private final PasswordExpirationMapper mapper = mock(PasswordExpirationMapper.class);
    private final PasswordExpirationServiceImpl service = new PasswordExpirationServiceImpl(mapper);

    @Test
    void initExpiration_maxDaysPositive_setsForceChangeFalseAndWarning() {
        service.initExpiration(1L, 90, 3, 7);

        ArgumentCaptor<PasswordExpiration> captor = ArgumentCaptor.forClass(PasswordExpiration.class);
        verify(mapper).insert(captor.capture());
        PasswordExpiration saved = captor.getValue();

        assertEquals(Boolean.FALSE, saved.getForceChange());
        assertEquals(3, saved.getGraceLoginRemaining());
        assertNotNull(saved.getExpiresAt());
        assertNotNull(saved.getNextWarningAt());
        assertEquals(saved.getExpiresAt().minusDays(7), saved.getNextWarningAt());
    }

    @Test
    void initExpiration_maxDaysZero_noNpeAndNullExpiry() {
        service.initExpiration(1L, 0, 0, 7);

        ArgumentCaptor<PasswordExpiration> captor = ArgumentCaptor.forClass(PasswordExpiration.class);
        verify(mapper).insert(captor.capture());
        PasswordExpiration saved = captor.getValue();

        assertNull(saved.getExpiresAt());
        assertNull(saved.getNextWarningAt());
        assertEquals(Boolean.FALSE, saved.getForceChange());
    }

    @Test
    void updateLastChanged_existing_clearsForceChange() {
        PasswordExpiration existing = new PasswordExpiration();
        existing.setUserId(1L);
        existing.setForceChange(true);
        when(mapper.selectOne(any())).thenReturn(existing);

        service.updateLastChanged(1L, 90, 3, 7);

        ArgumentCaptor<PasswordExpiration> captor = ArgumentCaptor.forClass(PasswordExpiration.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(Boolean.FALSE, captor.getValue().getForceChange());
        assertNotNull(captor.getValue().getExpiresAt());
    }

    @Test
    void updateLastChanged_existingMaxDaysZero_noNpe() {
        PasswordExpiration existing = new PasswordExpiration();
        existing.setUserId(1L);
        when(mapper.selectOne(any())).thenReturn(existing);

        service.updateLastChanged(1L, 0, 0, 7);

        ArgumentCaptor<PasswordExpiration> captor = ArgumentCaptor.forClass(PasswordExpiration.class);
        verify(mapper).updateById(captor.capture());
        assertNull(captor.getValue().getExpiresAt());
        assertNull(captor.getValue().getNextWarningAt());
    }

    @Test
    void updateForceChange_existing_updates() {
        PasswordExpiration existing = new PasswordExpiration();
        existing.setUserId(1L);
        existing.setForceChange(false);
        when(mapper.selectOne(any())).thenReturn(existing);

        service.updateForceChange(1L, true);

        ArgumentCaptor<PasswordExpiration> captor = ArgumentCaptor.forClass(PasswordExpiration.class);
        verify(mapper).updateById(captor.capture());
        assertEquals(Boolean.TRUE, captor.getValue().getForceChange());
    }

    @Test
    void updateForceChange_notExisting_skips() {
        when(mapper.selectOne(any())).thenReturn(null);

        service.updateForceChange(1L, true);

        verify(mapper, never()).updateById(any(PasswordExpiration.class));
    }
}
