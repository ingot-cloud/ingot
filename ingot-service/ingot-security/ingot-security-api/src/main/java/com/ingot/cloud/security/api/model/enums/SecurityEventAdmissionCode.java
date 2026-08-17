package com.ingot.cloud.security.api.model.enums;

import com.ingot.framework.commons.model.status.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 安全中心 admission 响应码，供 Feign Transport 区分可重试与终态失败。
 *
 * @author jy
 * @since 1.0.0
 */
@Getter
@RequiredArgsConstructor
public enum SecurityEventAdmissionCode implements ErrorCode {

    /** DURABLE spool 未能在时限内接纳，producer 应保留 claim 并重试。 */
    ADMISSION_RETRYABLE("SEC_EVENT_503", "security event admission temporarily unavailable"),

    /** BEST_EFFORT 队列满等明确拒绝；producer 应丢弃并 ack，不可重试。 */
    ADMISSION_REJECTED("SEC_EVENT_429", "security event admission rejected"),

    /** 请求校验失败。 */
    ADMISSION_INVALID("SEC_EVENT_400", "invalid security event payload");

    private final String code;
    private final String text;
}
