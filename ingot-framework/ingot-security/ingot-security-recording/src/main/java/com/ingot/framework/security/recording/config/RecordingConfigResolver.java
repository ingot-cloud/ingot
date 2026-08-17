package com.ingot.framework.security.recording.config;

import com.ingot.framework.security.recording.model.RecordingTarget;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * <p>解析 effective target、shadow 与 delivery 参数。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class RecordingConfigResolver {

    private RecordingConfigResolver() {
    }

    public static EffectiveRecordingConfig resolve(SecurityEventProperties properties) {
        RecordingTarget target = resolveTarget(properties);
        List<RecordingTarget> shadowTargets = resolveShadowTargets(properties, target);
        SecurityEventProperties.MemoryQueueSettings memory = resolveMemory(properties);
        return new EffectiveRecordingConfig(target, shadowTargets, memory);
    }

    static RecordingTarget resolveTarget(SecurityEventProperties properties) {
        if (properties.getTarget() == null || properties.getTarget().isBlank()) {
            return RecordingTarget.LOCAL;
        }
        return parseTarget(properties.getTarget());
    }

    static List<RecordingTarget> resolveShadowTargets(
            SecurityEventProperties properties,
            RecordingTarget primary) {
        Set<RecordingTarget> shadows = new LinkedHashSet<>();
        if (properties.getShadowTargets() != null) {
            for (String raw : properties.getShadowTargets()) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                RecordingTarget parsed = parseTarget(raw);
                if (parsed != primary) {
                    shadows.add(parsed);
                }
            }
        }
        return List.copyOf(shadows);
    }

    static SecurityEventProperties.MemoryQueueSettings resolveMemory(
            SecurityEventProperties properties) {
        SecurityEventProperties.Delivery delivery = properties.getDelivery();
        if (delivery != null && delivery.getMemory() != null) {
            return delivery.getMemory();
        }
        return new SecurityEventProperties.MemoryQueueSettings();
    }

    private static RecordingTarget parseTarget(String raw) {
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "center", "remote" -> RecordingTarget.CENTER;
            case "local" -> RecordingTarget.LOCAL;
            default -> throw new IllegalArgumentException("Unknown recording target: " + raw);
        };
    }
}
