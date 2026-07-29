package com.ingot.cloud.security.api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一安全事件上报 DTO。
 *
 * @author jy
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecurityEventReportDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String eventType;
    private String eventCategory;
    private LocalDateTime occurredAt;

    private Long tenantId;
    private Long userId;
    private String userType;
    private String account;

    private String clientId;
    private String appId;
    private String sessionId;
    private String deviceId;

    private String clientIp;
    private String requestUri;
    private String userAgent;

    private String result;
    private String reasonCode;
    private String reasonDetail;

    /** 上报模块：ingot-pms / ingot-member / ingot-gateway 等 */
    private String sourceModule;
    private String source;

    private Long operatorId;
    private String operatorName;
    private String traceId;

    private Map<String, Object> extension;
}
