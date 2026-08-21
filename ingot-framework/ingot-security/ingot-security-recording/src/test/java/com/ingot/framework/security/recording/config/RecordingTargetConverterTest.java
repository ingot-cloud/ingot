package com.ingot.framework.security.recording.config;

import com.ingot.framework.security.recording.model.RecordingTarget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * <p>{@link RecordingTargetConverter} 与 {@link RecordingTarget#fromValue(String)} 绑定别名单测。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class RecordingTargetConverterTest {

    private final RecordingTargetConverter converter = new RecordingTargetConverter();

    @Test
    @DisplayName("local 解析为 LOCAL")
    void localValue() {
        assertThat(converter.convert("local")).isEqualTo(RecordingTarget.LOCAL);
        assertThat(converter.convert("LOCAL")).isEqualTo(RecordingTarget.LOCAL);
    }

    @Test
    @DisplayName("center 解析为 CENTER")
    void centerValue() {
        assertThat(converter.convert("center")).isEqualTo(RecordingTarget.CENTER);
    }

    @Test
    @DisplayName("历史值 remote 映射为 CENTER")
    void remoteAliasIsCenter() {
        assertThat(converter.convert("remote")).isEqualTo(RecordingTarget.CENTER);
        assertThat(converter.convert("REMOTE")).isEqualTo(RecordingTarget.CENTER);
    }

    @Test
    @DisplayName("空白视为 LOCAL")
    void blankIsLocal() {
        assertThat(RecordingTarget.fromValue(null)).isEqualTo(RecordingTarget.LOCAL);
        assertThat(RecordingTarget.fromValue("  ")).isEqualTo(RecordingTarget.LOCAL);
    }

    @Test
    @DisplayName("未知值抛出异常")
    void unknownValueFails() {
        assertThatThrownBy(() -> converter.convert("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown recording target");
    }
}
