package com.ingot.framework.security.recording.runtime;

import java.util.Locale;
import java.util.Map;

import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.SecurityEventRecord;

/**
 * <p>按事件类型与可热刷新的 override 映射默认优先级。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public class DefaultPriorityClassifier implements PriorityClassifier {

    private static final RecordPriority DEFAULT_BEST_EFFORT = RecordPriority.BEST_EFFORT;
    private static final RecordPriority DEFAULT_DURABLE = RecordPriority.DURABLE;

    private final SecurityEventProperties properties;

    public DefaultPriorityClassifier(SecurityEventProperties properties) {
        this.properties = properties;
    }

    @Override
    public RecordPriority classify(SecurityEventRecord record) {
        if (record.getPriority() != null) {
            return record.getPriority();
        }
        String type = record.getEventType();
        if (type == null || type.isBlank()) {
            return DEFAULT_BEST_EFFORT;
        }
        Map<String, String> overrides = properties.getPriorityOverrides();
        if (overrides != null && !overrides.isEmpty()) {
            String override = overrides.get(type);
            if (override != null && !override.isBlank()) {
                return RecordPriority.valueOf(override.trim().toUpperCase(Locale.ROOT));
            }
        }
        return defaultForType(type);
    }

    static RecordPriority defaultForType(String eventType) {
        String normalized = eventType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "LOGIN_SUCCESS", "LOGIN_FAILURE", "RATE_LIMIT_VIOLATION" -> DEFAULT_BEST_EFFORT;
            case "ACCOUNT_CREATED", "ACCOUNT_ENABLED", "ACCOUNT_DISABLED", "ACCOUNT_LOCKED",
                 "ACCOUNT_UNLOCKED", "ACCOUNT_DELETED", "PASSWORD_CHANGED", "PASSWORD_RESET",
                 "PASSWORD_EXPIRED", "FORCE_CHANGE_PASSWORD", "BLACKLIST_BLOCK",
                 "LOGIN_FAIL_IP_EXCEED", "LOGIN_FAIL_DEVICE_EXCEED", "LOGIN_FAIL_CLIENT_EXCEED",
                 "LOGIN_FAIL_ACCOUNT_IP_EXCEED" -> DEFAULT_DURABLE;
            default -> DEFAULT_BEST_EFFORT;
        };
    }
}
