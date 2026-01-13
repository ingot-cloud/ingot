package com.ingot.framework.oss.common;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import cn.hutool.core.io.IoUtil;
import com.ingot.framework.commons.oss.OSSResult;
import com.ingot.framework.commons.oss.OssService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>Description  : 基于S3兼容协议的OSS服务抽象类，提供公共实现.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/12.</p>
 * <p>Time         : 10:00.</p>
 */
@Slf4j
public abstract class AbstractS3OssService implements OssService {

    /**
     * 获取S3客户端
     *
     * @return S3客户端实例
     */
    protected abstract S3Client getS3Client();

    /**
     * 获取默认过期时间（秒）
     *
     * @return 过期时间
     */
    protected abstract int getDefaultExpiredTime();

    @Override
    public OSSResult uploadFile(String bucket, String fileName, InputStream inputStream) {
        try {
            OSSResult result = new OSSResult();
            getS3Client().putObject(bucket, fileName, inputStream);
            result.setBucketName(bucket);
            result.setFileName(fileName);
            result.setUrl(getObjectURL(bucket + "/" + fileName));
            return result;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void getFile(String bucket, String fileName, HttpServletResponse response) {
        try (InputStream inputStream = getS3Client().getObject(bucket, fileName)) {
            response.setContentType("application/octet-stream; charset=UTF-8");
            IoUtil.copy(inputStream, response.getOutputStream());
        } catch (Exception e) {
            log.error("文件读取异常", e);
            throw new RuntimeException("文件读取失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getObjectURL(String url) {
        OssObjectInfo objectInfo = OssPathParser.parse(url);
        return getS3Client().getPresignedObjectUrl(
                objectInfo.bucket(),
                objectInfo.objectName(),
                getDefaultExpiredTime(),
                TimeUnit.SECONDS);
    }

    @Override
    public String getObjectURL(String url, int expiredSeconds) {
        OssObjectInfo objectInfo = OssPathParser.parse(url);
        return getS3Client().getPresignedObjectUrl(
                objectInfo.bucket(),
                objectInfo.objectName(),
                expiredSeconds,
                TimeUnit.SECONDS);
    }
}
