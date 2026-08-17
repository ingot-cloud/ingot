package com.ingot.framework.security.recording.model;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

/**
 * <p>业务审计记录预留契约，与安全事件分离的领域模型。</p>
 *
 * <p>本 change 仅定义模型与 SPI，不注册业务 producer。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see AuditFailurePolicy
 */
@Value
@Builder(toBuilder = true)
public class AuditRecord {

    String auditId;
    RecordPriority priority;

    String actorType;
    Long actorId;
    String actorName;

    String action;
    String targetType;
    String targetId;

    Map<String, Object> beforeState;
    Map<String, Object> afterState;

    String result;
    String reason;
    String sourceModule;
    String source;
    String traceId;

    Instant occurredAt;
    Instant receivedAt;
}
