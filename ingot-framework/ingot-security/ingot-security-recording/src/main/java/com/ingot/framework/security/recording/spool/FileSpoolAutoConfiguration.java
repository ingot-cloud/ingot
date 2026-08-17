package com.ingot.framework.security.recording.spool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.config.SecurityEventRecordingAutoConfiguration;
import com.ingot.framework.security.recording.model.SecurityEventRecord;
import com.ingot.framework.security.recording.spi.RecordQueue;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * <p>file spool 自动配置，替换 Phase 01 的 {@link com.ingot.framework.security.recording.runtime.UnconfiguredDurableRecordQueue}。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureBefore(SecurityEventRecordingAutoConfiguration.class)
    @ConditionalOnProperty(prefix = "ingot.security.event", name = "enabled", havingValue = "true", matchIfMissing = false)
public class FileSpoolAutoConfiguration {

    @Bean(name = "durableRecordQueue")
    @ConditionalOnMissingBean(name = "durableRecordQueue")
    public RecordQueue<SecurityEventRecord> durableRecordQueue(
            SecurityEventProperties properties,
            ObjectMapper objectMapper) throws java.io.IOException {
        return new FileSpoolRecordQueue(properties, objectMapper);
    }
}
