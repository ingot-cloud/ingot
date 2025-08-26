package com.ingot.framework.minio.service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.io.IoUtil;
import com.ingot.framework.core.oss.OSSResult;
import com.ingot.framework.core.oss.OssService;
import com.ingot.framework.minio.common.MinioObjectInfo;
import com.ingot.framework.minio.properties.MinioProperties;
import com.ingot.framework.minio.common.MinioPathParser;
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
        MinioObjectInfo objectInfo = MinioPathParser.parse(url);
        return minioService.getObjectURL(
                objectInfo.bucket(),
                objectInfo.objectName(),
                minioProperties.getExpiredTime(),
                TimeUnit.SECONDS);
    }

    @Override
    public String getObjectURL(String url, int expiredSeconds) {
        MinioObjectInfo objectInfo = MinioPathParser.parse(url);
        return minioService.getObjectURL(
                objectInfo.bucket(),
                objectInfo.objectName(),
                expiredSeconds,
                TimeUnit.SECONDS);
    }


}
