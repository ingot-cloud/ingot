package com.ingot.framework.oss.minio.service;

import com.ingot.framework.oss.common.AbstractS3OssService;
import com.ingot.framework.oss.common.S3Client;
import com.ingot.framework.oss.minio.properties.MinioProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : MinioOSSService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/8/7.</p>
 * <p>Time         : 10:04 AM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class MinioOssService extends AbstractS3OssService {
    private final MinioService minioService;
    private final MinioProperties minioProperties;

    @Override
    protected S3Client getS3Client() {
        return minioService;
    }

    @Override
    protected int getDefaultExpiredTime() {
        return minioProperties.getExpiredTime();
    }
}
