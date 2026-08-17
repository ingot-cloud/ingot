package com.ingot.framework.security.recording.config;

import com.ingot.framework.security.recording.model.RecordingTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <p>{@link RecordingConfigResolver} 配置解析单测。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class RecordingConfigResolverTest {

    @Test
    @DisplayName("缺省 target 解析为 LOCAL")
    void defaultTargetIsLocal() {
        SecurityEventProperties properties = new SecurityEventProperties();
        properties.setTarget(null);

        EffectiveRecordingConfig config = RecordingConfigResolver.resolve(properties);

        assertThat(config.primaryTarget()).isEqualTo(RecordingTarget.LOCAL);
        assertThat(config.shadowTargets()).isEmpty();
    }

    @Test
    @DisplayName("target=center 解析为 CENTER")
    void centerTarget() {
        SecurityEventProperties properties = new SecurityEventProperties();
        properties.setTarget("center");

        EffectiveRecordingConfig config = RecordingConfigResolver.resolve(properties);

        assertThat(config.primaryTarget()).isEqualTo(RecordingTarget.CENTER);
        assertThat(config.shadowTargets()).isEmpty();
    }

    @Test
    @DisplayName("delivery.memory 透传")
    void memoryFromDelivery() {
        SecurityEventProperties properties = new SecurityEventProperties();
        properties.getDelivery().getMemory().setQueueCapacity(4096);
        properties.getDelivery().getMemory().setBatchSize(64);

        EffectiveRecordingConfig config = RecordingConfigResolver.resolve(properties);

        assertThat(config.memory().getQueueCapacity()).isEqualTo(4096);
        assertThat(config.memory().getBatchSize()).isEqualTo(64);
    }

    @Test
    @DisplayName("shadow-targets 与主 target 去重")
    void shadowTargetsDedupPrimary() {
        SecurityEventProperties properties = new SecurityEventProperties();
        properties.setTarget("center");
        properties.setShadowTargets(List.of("center", "local"));

        EffectiveRecordingConfig config = RecordingConfigResolver.resolve(properties);

        assertThat(config.primaryTarget()).isEqualTo(RecordingTarget.CENTER);
        assertThat(config.shadowTargets()).containsExactly(RecordingTarget.LOCAL);
    }

    @Test
    @DisplayName("未知 target 抛出异常")
    void unknownTargetFails() {
        SecurityEventProperties properties = new SecurityEventProperties();
        properties.setTarget("invalid");

        assertThatThrownBy(() -> RecordingConfigResolver.resolve(properties))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
