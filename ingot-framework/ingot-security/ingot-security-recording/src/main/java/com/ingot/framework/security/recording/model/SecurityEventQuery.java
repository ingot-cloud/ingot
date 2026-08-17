package com.ingot.framework.security.recording.model;

import java.time.Instant;

/**
 * <p>安全事件查询条件；时间范围默认最近 24 小时，单次最大 31 天。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public record SecurityEventQuery(
        Instant receivedFrom,
        Instant receivedTo,
        String eventType,
        String eventCategory,
        Long tenantId,
        Long userId,
        String sourceModule,
        String traceId) {

    public static final long MAX_RANGE_DAYS = 31;
}
