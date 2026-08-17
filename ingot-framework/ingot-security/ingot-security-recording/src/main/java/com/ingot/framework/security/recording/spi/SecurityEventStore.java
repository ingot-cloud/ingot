package com.ingot.framework.security.recording.spi;

import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.model.StoreCapabilities;

import java.util.List;

/**
 * <p>安全事件最终存储 SPI，由 MySQL/日志/未来 ES module 实现。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface SecurityEventStore {

    String storeId();

    StoreCapabilities capabilities();

    void appendBatch(List<SecurityEventRecord> records);
}
