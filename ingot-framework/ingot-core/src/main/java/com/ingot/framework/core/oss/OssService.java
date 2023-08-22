package com.ingot.framework.core.oss;

import jakarta.servlet.http.HttpServletResponse;
import java.io.InputStream;

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

}
