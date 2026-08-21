package com.ingot.framework.security.recording.config;

import com.ingot.framework.security.recording.model.RecordingTarget;
import org.springframework.core.convert.converter.Converter;

/**
 * <p>将 YAML 字面量绑定为 {@link RecordingTarget}，兼容历史值 {@code remote}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class RecordingTargetConverter implements Converter<String, RecordingTarget> {

    @Override
    public RecordingTarget convert(String source) {
        return RecordingTarget.fromValue(source);
    }
}
