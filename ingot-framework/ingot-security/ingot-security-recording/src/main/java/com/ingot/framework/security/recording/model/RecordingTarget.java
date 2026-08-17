package com.ingot.framework.security.recording.model;

/**
 * <p>安全事件投递目标拓扑。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public enum RecordingTarget {

    /** 写入本地 SecurityEventStore。 */
    LOCAL,

    /** 经 SecurityEventTransport 送达安全中心。 */
    CENTER
}
