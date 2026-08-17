package com.ingot.framework.security.recording.model;

/**
 * <p>Store 实现声明的能力位，供装配与查询 API 边界判定。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public enum StoreCapability {

    /** 支持按 {@code eventId} 幂等写入。 */
    IDEMPOTENT,

    /** 提供 {@link com.ingot.framework.security.recording.spi.SecurityEventQueryRepository}。 */
    QUERY,

    /** 提供 {@link com.ingot.framework.security.recording.spi.SecurityEventRetentionHandler}。 */
    RETENTION
}
