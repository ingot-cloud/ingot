package com.ingot.framework.security.account.adapter.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.account.domain.model.AccountSecurityEvent;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link AccountSecurityEvent} → {@link SecurityEventRecord} 映射。
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class AccountSecurityEventRecordMapper {

    private static final ZoneId ZONE = ZoneId.systemDefault();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final SecurityEventProperties properties;
    private final ObjectMapper objectMapper;

    public SecurityEventRecord toRecord(AccountSecurityEvent event) {
        if (event == null || event.getEventType() == null) {
            return null;
        }
        Instant occurredAt = event.getCreatedAt() == null
                ? Instant.now()
                : event.getCreatedAt().atZone(ZONE).toInstant();
        return SecurityEventRecord.builder()
                .eventType(event.getEventType().getCode())
                .eventCategory(event.getEventCategory())
                .occurredAt(occurredAt)
                .tenantId(event.getTenantId())
                .userId(event.getUserId())
                .userType(event.getUserType() != null ? event.getUserType().name() : null)
                .clientIp(event.getClientIp())
                .userAgent(event.getUserAgent())
                .result(mapResult(event.getResult()))
                .reasonCode(event.getReasonCode())
                .reasonDetail(event.getReasonDetail())
                .sourceModule(properties.getSourceModule())
                .source(event.getSource() != null ? event.getSource().getValue() : null)
                .operatorId(event.getOperatorId())
                .operatorName(event.getOperatorName())
                .extension(parseExtension(event.getExtraData()))
                .build();
    }

    private static String mapResult(Boolean result) {
        if (result == null) {
            return null;
        }
        return result ? "SUCCESS" : "FAILURE";
    }

    private Map<String, Object> parseExtension(String extraData) {
        if (!StringUtils.hasText(extraData)) {
            return null;
        }
        try {
            return objectMapper.readValue(extraData, MAP_TYPE);
        } catch (Exception ignored) {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("extraData", extraData);
            return fallback;
        }
    }
}
