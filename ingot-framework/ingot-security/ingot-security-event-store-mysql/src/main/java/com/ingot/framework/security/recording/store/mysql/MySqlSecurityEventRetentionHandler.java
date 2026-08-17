package com.ingot.framework.security.recording.store.mysql;

import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.runtime.MemoryRecordQueue;
import com.ingot.framework.security.recording.spi.SecurityEventRetentionHandler;
import com.ingot.framework.security.recording.spool.FileSpoolRecordQueue;
import com.ingot.framework.security.recording.store.mysql.internal.SecurityEventWriteSemaphore;
import com.ingot.framework.security.recording.store.mysql.mapper.SecurityEventStoreMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>MySQL 安全事件 retention：命名锁、分批删除、时间预算与队列积压让步。</p>
 *
 * <p>队列让步分别探测 {@link MemoryRecordQueue} 与 {@link FileSpoolRecordQueue}。
 * 不得注入裸 {@code RecordQueue}：内存队列与 durable spool 均实现该接口，
 * {@link ObjectProvider#getIfAvailable()} 会因多候选 Bean 抛出
 * {@code NoUniqueBeanDefinitionException}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class MySqlSecurityEventRetentionHandler implements SecurityEventRetentionHandler {

    private static final String LOCK_NAME = "ingot:security-event:retention";

    private final SecurityEventStoreMapper mapper;
    private final SecurityEventWriteSemaphore writeSemaphore;
    private final TransactionTemplate transactionTemplate;
    private final SecurityEventProperties properties;
    private final ObjectProvider<MemoryRecordQueue> memoryQueueProvider;
    private final ObjectProvider<FileSpoolRecordQueue> durableQueueProvider;

    public MySqlSecurityEventRetentionHandler(
            SecurityEventStoreMapper mapper,
            SecurityEventWriteSemaphore writeSemaphore,
            TransactionTemplate transactionTemplate,
            SecurityEventProperties properties,
            ObjectProvider<MemoryRecordQueue> memoryQueueProvider,
            ObjectProvider<FileSpoolRecordQueue> durableQueueProvider) {
        this.mapper = mapper;
        this.writeSemaphore = writeSemaphore;
        this.transactionTemplate = transactionTemplate;
        this.properties = properties;
        this.memoryQueueProvider = memoryQueueProvider;
        this.durableQueueProvider = durableQueueProvider;
    }

    @Override
    public int runRetentionRound() {
        SecurityEventProperties.Retention retention = properties.getRetention();
        if (retention == null || !retention.isEnabled() || retention.getDays() <= 0) {
            return 0;
        }
        if (shouldYield()) {
            return 0;
        }
        Integer locked = mapper.acquireNamedLock(LOCK_NAME);
        if (locked == null || locked != 1) {
            return 0;
        }
        try {
            return purgeExpired(retention);
        } finally {
            mapper.releaseNamedLock(LOCK_NAME);
        }
    }

    private int purgeExpired(SecurityEventProperties.Retention retention) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retention.getDays());
        int batchSize = Math.max(retention.getBatchSize(), 1);
        int maxRounds = Math.max(retention.getMaxRounds(), 1);
        long deadline = System.currentTimeMillis() + retention.getMaxDurationSeconds() * 1000L;
        int total = 0;
        for (int round = 0; round < maxRounds && System.currentTimeMillis() < deadline; round++) {
            if (shouldYield()) {
                break;
            }
            int deleted = deleteBatch(cutoff, batchSize);
            total += deleted;
            if (deleted <= 0) {
                break;
            }
        }
        return total;
    }

    private int deleteBatch(LocalDateTime cutoff, int batchSize) {
        writeSemaphore.acquire();
        try {
            return transactionTemplate.execute(status -> {
                List<Long> ids = mapper.selectExpiredIds(cutoff, batchSize);
                if (ids.isEmpty()) {
                    return 0;
                }
                return mapper.deleteByIds(ids);
            });
        } finally {
            writeSemaphore.release();
        }
    }

    private boolean shouldYield() {
        SecurityEventProperties.Retention retention = properties.getRetention();
        int threshold = retention.getYieldQueueUsagePercent();
        MemoryRecordQueue memoryQueue = memoryQueueProvider.getIfAvailable();
        if (memoryQueue != null && memoryQueue.usageRatio() * 100 >= threshold) {
            return true;
        }
        FileSpoolRecordQueue durableQueue = durableQueueProvider.getIfAvailable();
        return durableQueue != null && durableQueue.usageRatio() * 100 >= threshold;
    }
}
