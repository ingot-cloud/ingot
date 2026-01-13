package com.ingot.framework.oss.rustfs.service;

import com.ingot.framework.oss.common.AbstractS3OssService;
import com.ingot.framework.oss.common.S3Client;
import com.ingot.framework.oss.rustfs.properties.RustfsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : RustfsOSSService.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/12.</p>
 * <p>Time         : 10:30.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class RustfsOssService extends AbstractS3OssService {
    private final RustfsService rustfsService;
    private final RustfsProperties rustfsProperties;

    @Override
    protected S3Client getS3Client() {
        return rustfsService;
    }

    @Override
    protected int getDefaultExpiredTime() {
        return rustfsProperties.getExpiredTime();
    }
}
