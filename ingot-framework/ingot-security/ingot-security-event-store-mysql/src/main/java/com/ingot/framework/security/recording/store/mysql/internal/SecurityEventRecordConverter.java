package com.ingot.framework.security.recording.store.mysql.internal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.store.mysql.model.CanonicalSecurityEventEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * <p>{@link SecurityEventRecord} 与 canonical 实体互转。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class SecurityEventRecordConverter {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private SecurityEventRecordConverter() {
    }

    public static CanonicalSecurityEventEntity toEntity(SecurityEventRecord record, ObjectMapper objectMapper) {
        LocalDateTime now = LocalDateTime.now();
        CanonicalSecurityEventEntity entity = new CanonicalSecurityEventEntity();
        entity.setEventId(record.getEventId());
        entity.setEventType(record.getEventType());
        entity.setEventCategory(record.getEventCategory());
        entity.setPriority(record.getPriority() == null
                ? RecordPriority.BEST_EFFORT.name()
                : record.getPriority().name());
        entity.setOccurredAt(toLocalDateTime(record.getOccurredAt()));
        entity.setReceivedAt(record.getReceivedAt() == null ? now : toLocalDateTime(record.getReceivedAt()));
        entity.setTenantId(record.getTenantId());
        entity.setUserId(record.getUserId());
        entity.setUserType(record.getUserType());
        entity.setAccount(record.getAccount());
        entity.setClientId(record.getClientId());
        entity.setAppId(record.getAppId());
        entity.setSessionId(record.getSessionId());
        entity.setDeviceId(record.getDeviceId());
        entity.setClientIp(record.getClientIp());
        entity.setRequestUri(record.getRequestUri());
        entity.setUserAgent(record.getUserAgent());
        entity.setResult(record.getResult());
        entity.setReasonCode(record.getReasonCode());
        entity.setReasonDetail(record.getReasonDetail());
        entity.setSourceModule(record.getSourceModule());
        entity.setSource(record.getSource());
        entity.setOperatorId(record.getOperatorId());
        entity.setOperatorName(record.getOperatorName());
        entity.setTraceId(record.getTraceId());
        entity.setExtensionJson(toJson(record.getExtension(), objectMapper));
        return entity;
    }

    public static SecurityEventRecord toRecord(CanonicalSecurityEventEntity entity, ObjectMapper objectMapper) {
        RecordPriority priority = entity.getPriority() == null
                ? RecordPriority.BEST_EFFORT
                : RecordPriority.valueOf(entity.getPriority());
        return SecurityEventRecord.builder()
                .eventId(entity.getEventId())
                .eventType(entity.getEventType())
                .eventCategory(entity.getEventCategory())
                .priority(priority)
                .occurredAt(toInstant(entity.getOccurredAt()))
                .receivedAt(toInstant(entity.getReceivedAt()))
                .tenantId(entity.getTenantId())
                .userId(entity.getUserId())
                .userType(entity.getUserType())
                .account(entity.getAccount())
                .clientId(entity.getClientId())
                .appId(entity.getAppId())
                .sessionId(entity.getSessionId())
                .deviceId(entity.getDeviceId())
                .clientIp(entity.getClientIp())
                .requestUri(entity.getRequestUri())
                .userAgent(entity.getUserAgent())
                .result(entity.getResult())
                .reasonCode(entity.getReasonCode())
                .reasonDetail(entity.getReasonDetail())
                .sourceModule(entity.getSourceModule())
                .source(entity.getSource())
                .operatorId(entity.getOperatorId())
                .operatorName(entity.getOperatorName())
                .traceId(entity.getTraceId())
                .extension(fromJson(entity.getExtensionJson(), objectMapper))
                .build();
    }

    public static List<CanonicalSecurityEventEntity> toEntities(
            List<SecurityEventRecord> records,
            ObjectMapper objectMapper) {
        return records.stream().map(record -> toEntity(record, objectMapper)).toList();
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZONE);
    }

    private static Instant toInstant(LocalDateTime time) {
        return time == null ? null : time.atZone(ZONE).toInstant();
    }

    private static String toJson(Map<String, Object> extension, ObjectMapper objectMapper) {
        if (extension == null || extension.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(extension);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("extension not serializable", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> fromJson(String json, ObjectMapper objectMapper) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
