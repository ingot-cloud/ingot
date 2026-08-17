package com.ingot.framework.security.recording.store.mysql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.model.CursorPageRequest;
import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.SecurityEventQuery;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.store.mysql.mapper.SecurityEventStoreMapper;
import com.ingot.framework.security.recording.store.mysql.model.CanonicalSecurityEventEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>{@link MySqlSecurityEventQueryRepository} 游标查询单测（Phase 04 V3）。</p>
 */
@ExtendWith(MockitoExtension.class)
class MySqlSecurityEventQueryRepositoryTest {

    @Mock
    private SecurityEventStoreMapper mapper;

    @Test
    @DisplayName("hasMore 时返回 next cursor")
    void cursorPaginationWithHasMore() {
        CanonicalSecurityEventEntity first = entity(100L, LocalDateTime.of(2026, 8, 5, 12, 0));
        CanonicalSecurityEventEntity second = entity(99L, LocalDateTime.of(2026, 8, 5, 11, 0));
        CanonicalSecurityEventEntity extra = entity(98L, LocalDateTime.of(2026, 8, 5, 10, 0));
        when(mapper.selectByCursor(any(), any(), any(), any(), any(), eq(3)))
                .thenReturn(List.of(first, second, extra));

        MySqlSecurityEventQueryRepository repository =
                new MySqlSecurityEventQueryRepository(mapper, new ObjectMapper());
        var page = repository.query(
                new SecurityEventQuery(null, null, null, null, null, null, null, null),
                CursorPageRequest.firstPage(2));

        assertThat(page.items()).hasSize(2);
        assertThat(page.hasMore()).isTrue();
        assertThat(page.nextCursorId()).isEqualTo(99L);
    }

    @Test
    @DisplayName("page size 最大 200")
    void capsPageSizeAt200() {
        when(mapper.selectByCursor(any(), any(), any(), any(), any(), eq(201))).thenReturn(List.of());
        MySqlSecurityEventQueryRepository repository =
                new MySqlSecurityEventQueryRepository(mapper, new ObjectMapper());
        repository.query(
                new SecurityEventQuery(null, null, null, null, null, null, null, null),
                new CursorPageRequest(500, null, null));
        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(mapper).selectByCursor(any(), any(), any(), any(), any(), limitCaptor.capture());
        assertThat(limitCaptor.getValue()).isEqualTo(201);
    }

    private static CanonicalSecurityEventEntity entity(long id, LocalDateTime receivedAt) {
        CanonicalSecurityEventEntity entity = new CanonicalSecurityEventEntity();
        entity.setId(id);
        entity.setEventId(String.format("%032d", id));
        entity.setEventType("LOGIN_SUCCESS");
        entity.setEventCategory("AUTH");
        entity.setPriority(RecordPriority.BEST_EFFORT.name());
        entity.setOccurredAt(receivedAt);
        entity.setReceivedAt(receivedAt);
        entity.setSourceModule("ingot-pms");
        return entity;
    }
}
