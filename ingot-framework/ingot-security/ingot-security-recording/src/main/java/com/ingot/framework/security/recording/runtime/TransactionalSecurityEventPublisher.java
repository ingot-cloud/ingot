package com.ingot.framework.security.recording.runtime;

import com.ingot.framework.security.recording.model.PublishOutcome;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.spi.SecurityEventEnqueue;
import com.ingot.framework.security.recording.spi.SecurityEventPublisher;
import com.ingot.framework.security.recording.support.SecurityEventRecordValidator;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <p>带事务 afterCommit 语义的安全事件 Publisher，默认 fail-open。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class TransactionalSecurityEventPublisher implements SecurityEventPublisher {

    private final SecurityEventEnqueue enqueue;
    private final SecurityEventRecordValidator validator;
    private final CategoryGate categoryGate;

    public TransactionalSecurityEventPublisher(
            SecurityEventEnqueue enqueue,
            SecurityEventRecordValidator validator,
            CategoryGate categoryGate) {
        this.enqueue = enqueue;
        this.validator = validator;
        this.categoryGate = categoryGate;
    }

    @Override
    public PublishOutcome publish(SecurityEventRecord record) {
        try {
            validator.validate(record);
        } catch (IllegalArgumentException e) {
            return PublishOutcome.FAILED;
        }
        if (!categoryGate.isEnabled(record.getEventCategory())) {
            return PublishOutcome.DISABLED;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    enqueue.enqueue(record);
                }
            });
            return PublishOutcome.ACCEPTED;
        }
        return enqueue.enqueue(record);
    }

    /**
     * <p>按 eventCategory 过滤是否上报。</p>
     *
     * @author jy
     * @since 1.0.0
     */
    @FunctionalInterface
    public interface CategoryGate {
        boolean isEnabled(String eventCategory);
    }
}
