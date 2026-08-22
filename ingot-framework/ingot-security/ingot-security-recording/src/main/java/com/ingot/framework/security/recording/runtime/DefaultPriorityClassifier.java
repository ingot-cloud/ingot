package com.ingot.framework.security.recording.runtime;

import java.util.Locale;
import java.util.Map;

import com.ingot.framework.security.event.codes.SecurityEventCodes;
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
            case SecurityEventCodes.LOGIN_SUCCESS, SecurityEventCodes.LOGIN_FAILURE,
                 SecurityEventCodes.RATE_LIMIT_VIOLATION -> DEFAULT_BEST_EFFORT;
            case SecurityEventCodes.ACCOUNT_CREATED, SecurityEventCodes.ACCOUNT_ENABLED,
                 SecurityEventCodes.ACCOUNT_DISABLED, SecurityEventCodes.ACCOUNT_LOCKED,
                 SecurityEventCodes.ACCOUNT_UNLOCKED, SecurityEventCodes.ACCOUNT_DELETED,
                 SecurityEventCodes.PASSWORD_CHANGED, SecurityEventCodes.PASSWORD_RESET,
                 SecurityEventCodes.PASSWORD_EXPIRED, SecurityEventCodes.FORCE_CHANGE_PASSWORD,
                 SecurityEventCodes.BLACKLIST_BLOCK, SecurityEventCodes.LOGIN_FAIL_IP_EXCEED,
                 SecurityEventCodes.LOGIN_FAIL_DEVICE_EXCEED, SecurityEventCodes.LOGIN_FAIL_CLIENT_EXCEED,
                 SecurityEventCodes.LOGIN_FAIL_ACCOUNT_IP_EXCEED,
                 // 被动下线是安全结论，丢事件等于丢掉「谁在何时被踢下线」的唯一证据
                 SecurityEventCodes.SESSION_REVOKED,
                 SecurityEventCodes.SESSION_CONCURRENT_KICKOUT -> DEFAULT_DURABLE;
            default -> DEFAULT_BEST_EFFORT;
        };
    }
}
