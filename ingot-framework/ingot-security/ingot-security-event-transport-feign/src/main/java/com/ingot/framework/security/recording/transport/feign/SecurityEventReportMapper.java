package com.ingot.framework.security.recording.transport.feign;

import com.ingot.cloud.security.api.model.dto.SecurityEventReportDTO;
import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.SecurityEventRecord;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

/**
 * <p>{@link SecurityEventReportDTO} 与 {@link SecurityEventRecord} 互转。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class SecurityEventReportMapper {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private SecurityEventReportMapper() {
    }

    public static SecurityEventRecord toRecord(SecurityEventReportDTO dto) {
        if (dto == null) {
            return null;
        }
        Instant occurredAt = dto.getOccurredAt() == null
                ? Instant.now()
                : dto.getOccurredAt().atZone(ZONE).toInstant();
        return SecurityEventRecord.builder()
                .eventId(dto.getEventId())
                .eventType(dto.getEventType())
                .eventCategory(dto.getEventCategory())
                .priority(parsePriority(dto.getPriority()))
                .occurredAt(occurredAt)
                .tenantId(dto.getTenantId())
                .userId(dto.getUserId())
                .userType(dto.getUserType())
                .account(dto.getAccount())
                .clientId(dto.getClientId())
                .appId(dto.getAppId())
                .sessionId(dto.getSessionId())
                .deviceId(dto.getDeviceId())
                .clientIp(dto.getClientIp())
                .requestUri(dto.getRequestUri())
                .userAgent(dto.getUserAgent())
                .result(dto.getResult())
                .reasonCode(dto.getReasonCode())
                .reasonDetail(dto.getReasonDetail())
                .sourceModule(dto.getSourceModule())
                .source(dto.getSource())
                .operatorId(dto.getOperatorId())
                .operatorName(dto.getOperatorName())
                .traceId(dto.getTraceId())
                .extension(dto.getExtension())
                .build();
    }

    public static SecurityEventReportDTO toDto(SecurityEventRecord record) {
        if (record == null) {
            return null;
        }
        return SecurityEventReportDTO.builder()
                .eventId(record.getEventId())
                .eventType(record.getEventType())
                .eventCategory(record.getEventCategory())
                .priority(record.getPriority() == null ? null : record.getPriority().name())
                .occurredAt(toLocalDateTime(record.getOccurredAt()))
                .tenantId(record.getTenantId())
                .userId(record.getUserId())
                .userType(record.getUserType())
                .account(record.getAccount())
                .clientId(record.getClientId())
                .appId(record.getAppId())
                .sessionId(record.getSessionId())
                .deviceId(record.getDeviceId())
                .clientIp(record.getClientIp())
                .requestUri(record.getRequestUri())
                .userAgent(record.getUserAgent())
                .result(record.getResult())
                .reasonCode(record.getReasonCode())
                .reasonDetail(record.getReasonDetail())
                .sourceModule(record.getSourceModule())
                .source(record.getSource())
                .operatorId(record.getOperatorId())
                .operatorName(record.getOperatorName())
                .traceId(record.getTraceId())
                .extension(record.getExtension())
                .build();
    }

    public static List<SecurityEventReportDTO> toDtos(List<SecurityEventRecord> records) {
        return records.stream().map(SecurityEventReportMapper::toDto).toList();
    }

    private static LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZONE);
    }

    private static RecordPriority parsePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return null;
        }
        return RecordPriority.valueOf(priority.trim().toUpperCase(Locale.ROOT));
    }
}
