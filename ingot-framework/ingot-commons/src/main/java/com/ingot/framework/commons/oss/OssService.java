package com.ingot.framework.commons.oss;

import java.io.InputStream;

import jakarta.servlet.http.HttpServletResponse;

/**
 * <p>Description  : OssService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/8/6.</p>
 * <p>Time         : 1:25 PM.</p>
 */
public interface OssService {

    /**
     * 上传文件
     *
     * @param bucket      bucket name
     * @param fileName    file name
     * @param inputStream {@link InputStream}
     * @return {@link OSSResult}
     */
    OSSResult uploadFile(String bucket, String fileName, InputStream inputStream);

    /**
     * 删除文件
     *
     * @param bucketName bucket name
     * @param objectName file name
     */
    void removeFile(String bucketName, String objectName);

    /**
     * 获取文件
     *
     * @param bucket   bucket name
     * @param fileName file name
     * @param response {@link HttpServletResponse}
     */
    void getFile(String bucket, String fileName, HttpServletResponse response);

    /**
     * 将库内存储路径转换为带过期时间的临时访问 URL。
     *
     * @param url 当前存储路径，兼容全路径和 {@code bucket/objectName}；{@code null} 或空串原样返回
     * @return 预签名 URL；解析失败、签名失败或超时时返回入参原值
     */
    String getObjectURL(String url);

    /**
     * 将库内存储路径转换为指定过期时间的临时访问 URL。
     *
     * @param url            当前存储路径，兼容全路径和 {@code bucket/objectName}；{@code null} 或空串原样返回
     * @param expiredSeconds 过期时间，单位秒
     * @return 预签名 URL；解析失败、签名失败或超时时返回入参原值
     */
    String getObjectURL(String url, int expiredSeconds);
}
