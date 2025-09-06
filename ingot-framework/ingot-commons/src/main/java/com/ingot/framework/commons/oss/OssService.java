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
     * 获取文件
     *
     * @param bucket   bucket name
     * @param fileName file name
     * @param response {@link HttpServletResponse}
     */
    void getFile(String bucket, String fileName, HttpServletResponse response);

    /**
     * 根据url获取临时访问路径
     *
     * @param url 当前存储的url，兼容全路径和bucket/fileName
     * @return 带过期时间的url
     */
    String getObjectURL(String url);

    /**
     * 根据url获取临时访问路径
     *
     * @param url            当前存储的url，兼容全路径和bucket/fileName
     * @param expiredSeconds 过期时间
     * @return 带过期时间的url
     */
    String getObjectURL(String url, int expiredSeconds);
}
