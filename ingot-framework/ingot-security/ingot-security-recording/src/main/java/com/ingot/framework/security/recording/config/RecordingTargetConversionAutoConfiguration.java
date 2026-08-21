package com.ingot.framework.security.recording.config;

import com.ingot.framework.security.recording.model.RecordingTarget;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;

/**
 * <p>注册 {@link RecordingTarget} 的配置绑定转换器，使 {@code target}/{@code shadow-targets}
 * 在 {@code @EnableConfigurationProperties} 绑定期即可识别 {@code remote} 别名。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
public class RecordingTargetConversionAutoConfiguration {

    @Bean
    @ConfigurationPropertiesBinding
    public Converter<String, RecordingTarget> recordingTargetConverter() {
        return new RecordingTargetConverter();
    }
}
