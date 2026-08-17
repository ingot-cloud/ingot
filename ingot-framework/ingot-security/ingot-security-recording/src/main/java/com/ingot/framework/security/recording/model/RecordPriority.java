package com.ingot.framework.security.recording.model;

/**
 * <p>安全记录投递优先级，决定事件进入内存队列还是 durable spool。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public enum RecordPriority {

    /** 尽力投递；队列满时可丢弃。 */
    BEST_EFFORT,

    /** 必须经 file spool 接纳后再投递；磁盘满时返回失败并告警。 */
    DURABLE
}
