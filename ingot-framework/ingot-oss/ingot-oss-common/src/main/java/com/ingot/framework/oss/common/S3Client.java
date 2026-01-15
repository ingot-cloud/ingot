package com.ingot.framework.oss.common;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * <p>Description  : S3兼容的客户端接口，用于抽象MinIO、RustFS等S3兼容的存储服务.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/12.</p>
 * <p>Time         : 10:00.</p>
 */
public interface S3Client {

    /**
     * 创建bucket
     *
     * @param bucketName bucket名称
     */
    void createBucket(String bucketName);

    /**
     * 检查bucket是否存在
     *
     * @param bucketName bucket名称
     * @return true-存在，false-不存在
     */
    boolean bucketExists(String bucketName);

    /**
     * 删除bucket
     *
     * @param bucketName bucket名称
     */
    void removeBucket(String bucketName);

    /**
     * 上传对象
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @param stream     文件流
     * @throws Exception 上传异常
     */
    void putObject(String bucketName, String objectName, InputStream stream) throws Exception;

    /**
     * 上传对象（指定大小和内容类型）
     *
     * @param bucketName  bucket名称
     * @param objectName  对象名称
     * @param stream      文件流
     * @param size        大小
     * @param contentType 内容类型
     * @throws Exception 上传异常
     */
    void putObject(String bucketName, String objectName, InputStream stream, long size, String contentType) throws Exception;

    /**
     * 获取对象
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @return 对象流
     */
    InputStream getObject(String bucketName, String objectName);

    /**
     * 获取对象的预签名URL
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @param duration   过期时间
     * @param unit       时间单位
     * @return 预签名URL
     */
    String getPresignedObjectUrl(String bucketName, String objectName, int duration, TimeUnit unit);

    /**
     * 删除对象
     *
     * @param bucketName bucket名称
     * @param objectName 对象名称
     * @throws Exception 删除异常
     */
    void removeObject(String bucketName, String objectName) throws Exception;
}
