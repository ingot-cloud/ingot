package com.ingot.framework.security.recording.model;

/**
 * <p>安全事件发布调用方的同步结果，不表示最终 Store 持久化完成。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public enum PublishOutcome {

    /** 已进入分类与队列，或 durable spool 已接纳。 */
    ACCEPTED,

    /** BEST_EFFORT 队列满等非阻塞丢弃。 */
    DROPPED,

    /** {@code enabled=false} 或未装配目标时的明确禁用。 */
    DISABLED,

    /** durable spool 拒绝、超时或其它不可恢复入队失败；业务仍 fail-open。 */
    FAILED
}
