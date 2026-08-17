package com.ingot.framework.security.recording.runtime;

import com.ingot.framework.security.recording.model.PublishOutcome;
import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.RecordingTarget;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.spi.SecurityEventPublisher;

/**
 * <p>{@code enabled=false} 时的明确 NoOp Publisher。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class DisabledSecurityEventPublisher implements SecurityEventPublisher {

    @Override
    public PublishOutcome publish(SecurityEventRecord record) {
        return PublishOutcome.DISABLED;
    }
}
