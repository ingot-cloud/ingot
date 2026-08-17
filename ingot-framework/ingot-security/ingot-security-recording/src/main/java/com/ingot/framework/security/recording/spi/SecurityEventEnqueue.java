package com.ingot.framework.security.recording.spi;

import com.ingot.framework.security.recording.model.PublishOutcome;
import com.ingot.framework.security.recording.model.SecurityEventRecord;

/**
 * <p>安全事件入队契约，供 Publisher 与 dispatcher 解耦。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface SecurityEventEnqueue {

    PublishOutcome enqueue(SecurityEventRecord record);
}
