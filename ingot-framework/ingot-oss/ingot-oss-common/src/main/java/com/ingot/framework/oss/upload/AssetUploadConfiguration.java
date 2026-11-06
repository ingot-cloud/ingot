package com.ingot.framework.oss.upload;

import com.ingot.framework.oss.common.S3Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * <p>供持有业务图片引用的服务启用本库上传暂存。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class AssetUploadConfiguration {

    /** 上传凭证与业务对象共用服务数据库及事务管理器。 */
    @Bean
    public StagedImageUploads stagedImageUploads(JdbcTemplate jdbc, S3Client storage,
            PlatformTransactionManager transactions, @Value("${ingot.oss.asset-bucket:ingot-assets}") String bucket) {
        return new StagedImageUploads(jdbc, storage, transactions, bucket);
    }
}
