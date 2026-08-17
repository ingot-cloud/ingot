package com.ingot.framework.security.recording.runtime;

import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.model.RecordPriority;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>{@link DefaultPriorityClassifier} 默认优先级映射单测。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class DefaultPriorityClassifierTest {

    private final DefaultPriorityClassifier classifier =
            new DefaultPriorityClassifier(new SecurityEventProperties());

    @Test
    @DisplayName("LOGIN_SUCCESS 默认 BEST_EFFORT")
    void loginSuccessIsBestEffort() {
        SecurityEventRecord record = baseRecord("LOGIN_SUCCESS").build();
        assertThat(classifier.classify(record)).isEqualTo(RecordPriority.BEST_EFFORT);
    }

    @Test
    @DisplayName("ACCOUNT_LOCKED 默认 DURABLE")
    void accountLockedIsDurable() {
        SecurityEventRecord record = baseRecord("ACCOUNT_LOCKED").build();
        assertThat(classifier.classify(record)).isEqualTo(RecordPriority.DURABLE);
    }

    @Test
    @DisplayName("record 自带 priority 时优先使用")
    void explicitPriorityWins() {
        SecurityEventRecord record = baseRecord("LOGIN_SUCCESS")
                .priority(RecordPriority.DURABLE)
                .build();
        assertThat(classifier.classify(record)).isEqualTo(RecordPriority.DURABLE);
    }

    @Test
    @DisplayName("热刷新 override 生效")
    void overrideApplies() {
        SecurityEventProperties properties = new SecurityEventProperties();
        properties.setPriorityOverrides(Map.of("LOGIN_SUCCESS", "DURABLE"));
        DefaultPriorityClassifier overridden = new DefaultPriorityClassifier(properties);

        SecurityEventRecord record = baseRecord("LOGIN_SUCCESS").build();
        assertThat(overridden.classify(record)).isEqualTo(RecordPriority.DURABLE);
    }

    private static SecurityEventRecord.SecurityEventRecordBuilder baseRecord(String type) {
        return SecurityEventRecord.builder()
                .eventId("01234567890123456789012345678901")
                .eventType(type)
                .eventCategory("AUTH")
                .occurredAt(Instant.parse("2026-08-04T00:00:00Z"));
    }
}
