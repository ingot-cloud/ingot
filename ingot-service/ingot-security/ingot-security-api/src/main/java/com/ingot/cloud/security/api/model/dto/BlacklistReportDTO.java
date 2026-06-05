package com.ingot.cloud.security.api.model.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 网关自动封禁事件上报。
 *
 * @author jy
 * @since 2026/5/26
 */
@Data
public class BlacklistReportDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * IP / DEVICE / USER。
     */
    private String keyType;

    private String keyValue;

    /**
     * B=封禁 / U=解封 / R=续期。
     */
    private String action;

    /**
     * A=自动 / M=手工。
     */
    private String triggerSource;

    private String ruleCode;

    private Integer countInWindow;

    private Integer ttlSec;

    private String traceId;

    private String requestPath;

    private String userAgent;

    private String realIp;

    private String remark;
}
