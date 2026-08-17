package com.ingot.framework.security.recording.store.mysql;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.model.StoreCapabilities;
import com.ingot.framework.security.recording.model.StoreCapability;
import com.ingot.framework.security.recording.spi.SecurityEventStore;
import com.ingot.framework.security.recording.store.mysql.internal.SecurityEventRecordConverter;
import com.ingot.framework.security.recording.store.mysql.internal.SecurityEventWriteSemaphore;
import com.ingot.framework.security.recording.store.mysql.mapper.SecurityEventStoreMapper;
import com.ingot.framework.security.recording.store.mysql.model.CanonicalSecurityEventEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

/**
 * <p>MySQL canonical {@code security_event} Store：真批量写入、eventId 幂等与 writer 舱壁。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class MySqlSecurityEventStore implements SecurityEventStore {

    private final SecurityEventStoreMapper mapper;
    private final SecurityEventWriteSemaphore writeSemaphore;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;

    public MySqlSecurityEventStore(
            SecurityEventStoreMapper mapper,
            SecurityEventWriteSemaphore writeSemaphore,
            TransactionTemplate transactionTemplate,
            ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.writeSemaphore = writeSemaphore;
        this.transactionTemplate = transactionTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String storeId() {
        return "mysql";
    }

    @Override
    public StoreCapabilities capabilities() {
        return StoreCapabilities.of(StoreCapability.IDEMPOTENT, StoreCapability.QUERY, StoreCapability.RETENTION);
    }

    @Override
    public void appendBatch(List<SecurityEventRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<CanonicalSecurityEventEntity> entities =
                SecurityEventRecordConverter.toEntities(records, objectMapper);
        writeSemaphore.acquire();
        try {
            transactionTemplate.executeWithoutResult(status -> {
                try {
                    mapper.insertBatch(entities);
                } catch (DuplicateKeyException ignored) {
                    // event_id 幂等：重复视为成功
                }
            });
        } finally {
            writeSemaphore.release();
        }
    }
}
