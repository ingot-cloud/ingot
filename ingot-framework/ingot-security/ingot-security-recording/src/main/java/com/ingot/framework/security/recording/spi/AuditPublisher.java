package com.ingot.framework.security.recording.spi;

import com.ingot.framework.security.recording.model.AuditRecord;
import com.ingot.framework.security.recording.model.PublishOutcome;

/**
 * <p>审计记录发布入口预留 SPI。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface AuditPublisher {

    PublishOutcome publish(AuditRecord record);
}
