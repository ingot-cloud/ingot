package com.ingot.framework.security.recording.runtime;

import com.ingot.framework.security.recording.model.PublishOutcome;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.support.SecurityEventRecordValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>{@link TransactionalSecurityEventPublisher} 事务 afterCommit 与 fail-open 单测。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class TransactionalSecurityEventPublisherTest {

    @Test
    @DisplayName("无事务时立即入队")
    void publishesImmediatelyWithoutTransaction() {
        AtomicReference<SecurityEventRecord> captured = new AtomicReference<>();
        TransactionalSecurityEventPublisher publisher = newPublisher(captured);

        PublishOutcome outcome = publisher.publish(validRecord());

        assertThat(outcome).isEqualTo(PublishOutcome.ACCEPTED);
        assertThat(captured.get()).isNotNull();
    }

    @Test
    @DisplayName("事务回滚时不发布")
    void doesNotPublishOnRollback() {
        AtomicReference<SecurityEventRecord> captured = new AtomicReference<>();
        TransactionalSecurityEventPublisher publisher = newPublisher(captured);

        TransactionSynchronizationManager.initSynchronization();
        try {
            TransactionSynchronizationManager.setActualTransactionActive(true);
            PublishOutcome outcome = publisher.publish(validRecord());
            assertThat(outcome).isEqualTo(PublishOutcome.ACCEPTED);
            TransactionSynchronizationManager.clearSynchronization();
            assertThat(captured.get()).isNull();
        } finally {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.clearSynchronization();
            }
        }
    }

    @Test
    @DisplayName("事务提交后 afterCommit 发布")
    void publishesAfterCommit() {
        AtomicReference<SecurityEventRecord> captured = new AtomicReference<>();
        TransactionalSecurityEventPublisher publisher = newPublisher(captured);

        TransactionSynchronizationManager.initSynchronization();
        try {
            TransactionSynchronizationManager.setActualTransactionActive(true);
            publisher.publish(validRecord());
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);
            assertThat(captured.get()).isNotNull();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    @DisplayName("category 关闭时返回 DISABLED")
    void disabledCategoryReturnsDisabled() {
        TransactionalSecurityEventPublisher publisher = new TransactionalSecurityEventPublisher(
                record -> PublishOutcome.ACCEPTED,
                new SecurityEventRecordValidator(null),
                category -> false);

        PublishOutcome outcome = publisher.publish(validRecord());
        assertThat(outcome).isEqualTo(PublishOutcome.DISABLED);
    }

    private static TransactionalSecurityEventPublisher newPublisher(AtomicReference<SecurityEventRecord> captured) {
        return new TransactionalSecurityEventPublisher(
                record -> {
                    captured.set(record);
                    return PublishOutcome.ACCEPTED;
                },
                new SecurityEventRecordValidator(null),
                category -> true);
    }

    private static SecurityEventRecord validRecord() {
        return SecurityEventRecord.builder()
                .eventId("01234567890123456789012345678901")
                .eventType("LOGIN_SUCCESS")
                .eventCategory("AUTH")
                .occurredAt(Instant.parse("2026-08-04T00:00:00Z"))
                .build();
    }
}
