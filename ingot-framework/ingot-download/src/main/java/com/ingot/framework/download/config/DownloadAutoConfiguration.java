package com.ingot.framework.download.config;

import java.time.Clock;

import com.ingot.framework.commons.oss.OssService;
import com.ingot.framework.download.DownloadCenter;
import com.ingot.framework.download.DownloadSourceAccess;
import com.ingot.framework.download.DownloadTaskStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * <p>宿主提供 {@link DownloadTaskStore} 与 {@link OssService} 时装配下载中心。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@AutoConfiguration(afterName = {
        "com.ingot.framework.oss.minio.MinioAutoConfiguration",
        "com.ingot.framework.oss.rustfs.RustfsAutoConfiguration"})
@ConditionalOnBean({DownloadTaskStore.class, OssService.class})
@EnableConfigurationProperties(DownloadProperties.class)
public class DownloadAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DownloadWorkers downloadWorkers(DownloadProperties properties) {
        return new DownloadWorkers(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public DownloadCenter downloadCenter(DownloadTaskStore store, OssService oss, DownloadProperties properties,
            DownloadWorkers workers, ObjectProvider<DownloadSourceAccess> accesses) {
        DownloadCenter center = new DownloadCenter(store, oss, properties, workers::execute,
                accesses.orderedStream().toList(), Clock.systemUTC());
        workers.scheduleCleanup(center);
        return center;
    }
}
