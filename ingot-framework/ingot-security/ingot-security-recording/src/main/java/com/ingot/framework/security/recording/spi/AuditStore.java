package com.ingot.framework.security.recording.spi;

import com.ingot.framework.security.recording.model.AuditRecord;
import com.ingot.framework.security.recording.model.StoreCapabilities;

import java.util.List;

/**
 * <p>审计记录最终存储 SPI 预留契约。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface AuditStore {

    String storeId();

    StoreCapabilities capabilities();

    void appendBatch(List<AuditRecord> records);
}
