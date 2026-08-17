package com.ingot.framework.security.recording.store.mysql;

import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.runtime.MemoryRecordQueue;
import com.ingot.framework.security.recording.spool.FileSpoolRecordQueue;
import com.ingot.framework.security.recording.store.mysql.internal.SecurityEventWriteSemaphore;
import com.ingot.framework.security.recording.store.mysql.mapper.SecurityEventStoreMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * <p>{@link MySqlSecurityEventRetentionHandler} retention 行为单测（Phase 04 V3）。</p>
 */
@ExtendWith(MockitoExtension.class)
class MySqlSecurityEventRetentionHandlerTest {

    @Mock
    private SecurityEventStoreMapper mapper;

    @Mock
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("days=0 时跳过清理")
    void skipsWhenPermanentRetention() {
        MySqlSecurityEventRetentionHandler handler = createHandler(0, null, null);
        assertThat(handler.runRetentionRound()).isZero();
        verify(mapper, never()).acquireNamedLock(any());
    }

    @Test
    @DisplayName("命名锁获取失败时跳过")
    void skipsWhenLockNotAcquired() {
        when(mapper.acquireNamedLock(any())).thenReturn(0);
        MySqlSecurityEventRetentionHandler handler = createHandler(30, null, null);
        assertThat(handler.runRetentionRound()).isZero();
        verify(mapper, never()).selectExpiredIds(any(), anyInt());
    }

    @Test
    @DisplayName("队列积压超过阈值时让步")
    void yieldsWhenMemoryQueueBacklogged() {
        MemoryRecordQueue queue = new MemoryRecordQueue(10, "test");
        for (int i = 0; i < 10; i++) {
            queue.enqueue(sampleRecord(i));
        }
        MySqlSecurityEventRetentionHandler handler = createHandler(30, queue, null);
        assertThat(handler.runRetentionRound()).isZero();
        verify(mapper, never()).acquireNamedLock(any());
    }

    @Test
    @DisplayName("成功获取锁后分批删除")
    void purgesExpiredRows() {
        when(mapper.acquireNamedLock(any())).thenReturn(1);
        when(mapper.selectExpiredIds(any(), eq(500))).thenReturn(List.of(1L, 2L)).thenReturn(List.of());
        when(mapper.deleteByIds(any())).thenReturn(2);
        doAnswer(invocation -> {
            TransactionCallback<Integer> callback = invocation.getArgument(0);
            return callback.doInTransaction(new SimpleTransactionStatus());
        }).when(transactionTemplate).execute(any());

        MySqlSecurityEventRetentionHandler handler = createHandler(30, null, null);
        assertThat(handler.runRetentionRound()).isEqualTo(2);
        verify(mapper).releaseNamedLock(any());
    }

    private MySqlSecurityEventRetentionHandler createHandler(
            int days,
            MemoryRecordQueue memoryQueue,
            FileSpoolRecordQueue durableQueue) {
        SecurityEventProperties properties = new SecurityEventProperties();
        SecurityEventProperties.Retention retention = new SecurityEventProperties.Retention();
        retention.setEnabled(true);
        retention.setDays(days);
        retention.setBatchSize(500);
        retention.setMaxRounds(100);
        retention.setMaxDurationSeconds(30);
        retention.setYieldQueueUsagePercent(50);
        properties.setRetention(retention);

        ObjectProvider<MemoryRecordQueue> memoryProvider = new ObjectProvider<>() {
            @Override
            public MemoryRecordQueue getObject() {
                return memoryQueue;
            }

            @Override
            public MemoryRecordQueue getObject(Object... args) {
                return memoryQueue;
            }

            @Override
            public MemoryRecordQueue getIfAvailable() {
                return memoryQueue;
            }

            @Override
            public MemoryRecordQueue getIfUnique() {
                return memoryQueue;
            }
        };
        ObjectProvider<FileSpoolRecordQueue> durableProvider = new ObjectProvider<>() {
            @Override
            public FileSpoolRecordQueue getObject() {
                return durableQueue;
            }

            @Override
            public FileSpoolRecordQueue getObject(Object... args) {
                return durableQueue;
            }

            @Override
            public FileSpoolRecordQueue getIfAvailable() {
                return durableQueue;
            }

            @Override
            public FileSpoolRecordQueue getIfUnique() {
                return durableQueue;
            }
        };

        return new MySqlSecurityEventRetentionHandler(
                mapper,
                new SecurityEventWriteSemaphore(1, 5),
                transactionTemplate,
                properties,
                memoryProvider,
                durableProvider);
    }

    private static com.ingot.framework.security.recording.model.SecurityEventRecord sampleRecord(int i) {
        return com.ingot.framework.security.recording.model.SecurityEventRecord.builder()
                .eventId(String.format("%032d", i))
                .eventType("LOGIN_SUCCESS")
                .eventCategory("AUTH")
                .occurredAt(java.time.Instant.parse("2026-08-05T00:00:00Z"))
                .sourceModule("test")
                .build();
    }
}
