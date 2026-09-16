package com.ingot.framework.security.recording.store.mysql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.store.mysql.internal.SecurityEventWriteSemaphore;
import com.ingot.framework.security.recording.store.mysql.mapper.SecurityEventStoreMapper;
import com.ingot.framework.security.recording.store.mysql.model.CanonicalSecurityEventEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>{@link MySqlSecurityEventStore} 批量写入与 semaphore 单测。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class MySqlSecurityEventStoreTest {

    @Mock
    private SecurityEventStoreMapper mapper;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("appendBatch 调用 mapper 真批量 INSERT")
    void appendBatchUsesMapperInsertBatch() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> consumer = invocation.getArgument(0);
            consumer.accept(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        SecurityEventWriteSemaphore semaphore = new SecurityEventWriteSemaphore(1, 5);
        MySqlSecurityEventStore store = new MySqlSecurityEventStore(
                mapper, semaphore, transactionTemplate, new ObjectMapper());

        SecurityEventRecord record = SecurityEventRecord.builder()
                .eventId("01234567890123456789012345678901")
                .eventType("ACCOUNT_LOCKED")
                .eventCategory("ACCOUNT")
                .priority(RecordPriority.DURABLE)
                .occurredAt(Instant.parse("2026-08-05T00:00:00Z"))
                .sourceModule("ingot-iam")
                .build();

        when(mapper.insertBatch(any())).thenReturn(1);
        store.appendBatch(List.of(record));

        ArgumentCaptor<List<CanonicalSecurityEventEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(mapper).insertBatch(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getEventId()).isEqualTo(record.getEventId());
        assertThat(store.storeId()).isEqualTo("mysql");
    }

    @Test
    @DisplayName("相同 eventId 重复批次仍走 INSERT ON DUPLICATE KEY（幂等）")
    void appendBatchIsIdempotentByEventId() {
        doAnswer(invocation -> {
            Consumer<TransactionStatus> consumer = invocation.getArgument(0);
            consumer.accept(new SimpleTransactionStatus());
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        SecurityEventWriteSemaphore semaphore = new SecurityEventWriteSemaphore(1, 5);
        MySqlSecurityEventStore store = new MySqlSecurityEventStore(
                mapper, semaphore, transactionTemplate, new ObjectMapper());

        SecurityEventRecord record = SecurityEventRecord.builder()
                .eventId("01234567890123456789012345678901")
                .eventType("ACCOUNT_LOCKED")
                .eventCategory("ACCOUNT")
                .priority(RecordPriority.DURABLE)
                .occurredAt(Instant.parse("2026-08-05T00:00:00Z"))
                .sourceModule("ingot-iam")
                .build();

        when(mapper.insertBatch(any())).thenReturn(1);
        store.appendBatch(List.of(record));
        store.appendBatch(List.of(record));

        verify(mapper, org.mockito.Mockito.times(2)).insertBatch(any());
    }
}
