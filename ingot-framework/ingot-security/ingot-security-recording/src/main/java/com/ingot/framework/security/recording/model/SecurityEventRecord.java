package com.ingot.framework.security.recording.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;

/**
 * <p>安全事件统一内部模型，贯穿 Publisher、Queue、Store 与 Transport。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see RecordPriority
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
@AllArgsConstructor
public class SecurityEventRecord {

    /** 32 字符 UUID，producer 首次发布前生成，重试保持不变。 */
    String eventId;

    String eventType;
    String eventCategory;
    RecordPriority priority;

    /** 业务发生时间，必填。 */
    Instant occurredAt;

    /** Store 接收时间，由最终 Store 填充。 */
    Instant receivedAt;

    Long tenantId;
    Long userId;
    String userType;
    String account;

    String clientId;
    String appId;
    String sessionId;
    String deviceId;

    String clientIp;
    String requestUri;
    String userAgent;

    String result;
    String reasonCode;
    String reasonDetail;

    String sourceModule;
    String source;

    Long operatorId;
    String operatorName;
    String traceId;

    /** JSON 对象序列化后最大 16 KiB。 */
    Map<String, Object> extension;
}
