package com.ingot.framework.security.recording.spool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.security.recording.config.SecurityEventProperties;
import com.ingot.framework.security.recording.config.SecurityEventRecordingAutoConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * <p>file spool 自动配置，替换 Phase 01 的 {@link com.ingot.framework.security.recording.runtime.UnconfiguredDurableRecordQueue}。</p>
 *
 * <p>实际目录为 {@code delivery.spool.directory}/{@code spring.application.name}，避免多服务共用一份 state。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration
@AutoConfigureBefore(SecurityEventRecordingAutoConfiguration.class)
@ConditionalOnProperty(prefix = "ingot.security.event", name = "enabled", havingValue = "true", matchIfMissing = false)
public class FileSpoolAutoConfiguration {

    /**
     * 装配按应用隔离的 DURABLE {@link FileSpoolRecordQueue}。
     *
     * @param properties      {@code ingot.security.event} 绑定
     * @param objectMapper    序列化 segment payload
     * @param applicationName {@code spring.application.name}，缺省 {@link FileSpoolRecordQueue#DEFAULT_APPLICATION_NAME}
     * @return 独占目录锁的 durable 队列
     * @throws java.io.IOException 目录锁被占用或启动恢复失败
     */
    @Bean(name = "durableRecordQueue")
    @ConditionalOnMissingBean(name = "durableRecordQueue")
    public FileSpoolRecordQueue durableRecordQueue(
            SecurityEventProperties properties,
            ObjectMapper objectMapper,
            @Value("${spring.application.name:" + FileSpoolRecordQueue.DEFAULT_APPLICATION_NAME + "}")
            String applicationName) throws java.io.IOException {
        return new FileSpoolRecordQueue(properties, objectMapper, applicationName);
    }
}
