package com.ingot.framework.security.recording.spi;

import com.ingot.framework.security.recording.model.ClaimedRecord;
import com.ingot.framework.security.recording.model.EnqueueResult;

import java.time.Duration;
import java.util.List;

/**
 * <p>分级可靠队列 SPI，BEST_EFFORT 内存队列与 DURABLE file spool 共用契约。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param <T> 队列元素类型
 */
public interface RecordQueue<T> {

    EnqueueResult enqueue(T record);

    List<ClaimedRecord<T>> claim(int limit, Duration wait);

    void ack(List<String> claimIds);

    void nack(List<String> claimIds, Throwable cause);
}
