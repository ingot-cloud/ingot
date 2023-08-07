package com.ingot.framework.minio.service;

import cn.hutool.core.io.IoUtil;
import com.ingot.framework.core.oss.OSSResult;
import com.ingot.framework.core.oss.OSSService;
import com.ingot.framework.minio.properties.MinioProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;

/**
 * <p>Description  : MinioOSSService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/8/7.</p>
 * <p>Time         : 10:04 AM.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class MinioOSSService implements OSSService {
    private final MinioService minioService;
    private final MinioProperties minioProperties;

    @Override
    public OSSResult uploadFile(String bucket, String fileName, InputStream inputStream) {
        try {
            OSSResult result = new OSSResult();
            minioService.putObject(bucket, fileName, inputStream);
            result.setBucketName(bucket);
            result.setFileName(fileName);
            result.setUrl(minioProperties.getPublicUrl() +
                    "/" + bucket + "/" + fileName + "?t=" + System.currentTimeMillis());
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
}
