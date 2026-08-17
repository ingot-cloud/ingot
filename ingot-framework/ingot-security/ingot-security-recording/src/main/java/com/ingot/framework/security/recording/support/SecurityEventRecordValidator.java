package com.ingot.framework.security.recording.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.model.SecurityEventRecord;

import java.util.Locale;
import java.util.Set;

/**
 * <p>安全事件记录入队前校验：必填字段、extension 大小与秘密字段拒绝。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class SecurityEventRecordValidator {

    public static final int MAX_EXTENSION_BYTES = 16 * 1024;

    private static final Set<String> SECRET_KEYS = Set.of(
            "password", "passwd", "secret", "token", "access_token",
            "refresh_token", "api_key", "apikey", "private_key", "credential");

    private final ObjectMapper objectMapper;

    public SecurityEventRecordValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper == null ? new ObjectMapper() : objectMapper;
    }

    public void validate(SecurityEventRecord record) {
        if (record == null) {
            throw new IllegalArgumentException("record must not be null");
        }
        if (record.getEventType() == null || record.getEventType().isBlank()) {
            throw new IllegalArgumentException("eventType is required");
        }
        if (record.getEventCategory() == null || record.getEventCategory().isBlank()) {
            throw new IllegalArgumentException("eventCategory is required");
        }
        if (record.getOccurredAt() == null) {
            throw new IllegalArgumentException("occurredAt is required");
        }
        if (record.getEventId() != null && record.getEventId().length() != 32) {
            throw new IllegalArgumentException("eventId must be 32 characters when present");
        }
        if (record.getExtension() != null && !record.getExtension().isEmpty()) {
            rejectSecretKeys(record.getExtension().keySet());
            try {
                byte[] bytes = objectMapper.writeValueAsBytes(record.getExtension());
                if (bytes.length > MAX_EXTENSION_BYTES) {
                    throw new IllegalArgumentException(
                            "extension exceeds " + MAX_EXTENSION_BYTES + " bytes");
                }
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("extension is not serializable", e);
            }
        }
    }

    private static void rejectSecretKeys(Iterable<String> keys) {
        for (String key : keys) {
            if (key == null) {
                continue;
            }
            String normalized = key.toLowerCase(Locale.ROOT);
            for (String secret : SECRET_KEYS) {
                if (normalized.contains(secret)) {
                    throw new IllegalArgumentException("extension must not contain secret key: " + key);
                }
            }
        }
    }
}
