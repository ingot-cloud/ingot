package com.ingot.framework.security.recording.model;

/**
 * <p>RecordQueue claim 后返回的带 claimId 记录，用于 ack/nack 语义。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param claimId 队列内唯一 claim 标识
 * @param record 原始记录
 * @param <T> 记录类型
 */
public record ClaimedRecord<T>(String claimId, T record) {
}
