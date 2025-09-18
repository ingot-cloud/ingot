package com.ingot.framework.oss.minio.service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.io.IoUtil;
import com.ingot.framework.commons.oss.OSSResult;
import com.ingot.framework.commons.oss.OssService;
import com.ingot.framework.oss.common.OssObjectInfo;
import com.ingot.framework.oss.common.OssPathParser;
import com.ingot.framework.oss.minio.properties.MinioProperties;
import jakarta.servlet.http.HttpServletResponse;
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
public class MinioOssService implements OssService {
    private final MinioService minioService;
    private final MinioProperties minioProperties;

    @Override
    public OSSResult uploadFile(String bucket, String fileName, InputStream inputStream) {
        try {
            OSSResult result = new OSSResult();
            minioService.putObject(bucket, fileName, inputStream);
            result.setBucketName(bucket);
            result.setFileName(fileName);
            result.setUrl(getObjectURL(bucket + "/" + fileName));
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void getFile(String bucket, String fileName, HttpServletResponse response) {
        try (InputStream inputStream = minioService.getObject(bucket, fileName)) {
            response.setContentType("application/octet-stream; charset=UTF-8");
            IoUtil.copy(inputStream, response.getOutputStream());
        } catch (Exception e) {
            log.error("文件读取异常", e);
        }
    }

    @Override
    public String getObjectURL(String url) {
        OssObjectInfo objectInfo = OssPathParser.parse(url);
        return minioService.getObjectURL(
                objectInfo.bucket(),
                objectInfo.objectName(),
                minioProperties.getExpiredTime(),
                TimeUnit.SECONDS);
    }

    @Override
    public String getObjectURL(String url, int expiredSeconds) {
        OssObjectInfo objectInfo = OssPathParser.parse(url);
        return minioService.getObjectURL(
                objectInfo.bucket(),
                objectInfo.objectName(),
                expiredSeconds,
                TimeUnit.SECONDS);
    }


}
