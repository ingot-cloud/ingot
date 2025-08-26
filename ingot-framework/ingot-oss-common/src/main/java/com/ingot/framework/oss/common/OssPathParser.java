package com.ingot.framework.oss.common;

/**
 * <p>Description  : MinioPathParser.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2025/8/23.</p>
 * <p>Time         : 10:53.</p>
 */
public class OssPathParser {

    /**
     * 解析OSS路径，支持以下两种格式：
     * 1. 完整URL，例如：https://host:9000/bucket/dir/file.png
     * 2. bucket + objectName，例如：bucket/dir/file.png
     *
     * @param path 存储在数据库中的路径
     * @return OssObjectInfo
     */
    public static OssObjectInfo parse(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("路径不能为空");
        }

        String bucket;
        String objectName;

        // 判断是否是URL
        if (path.startsWith("http://") || path.startsWith("https://")) {
            String noProtocol = path.replaceFirst("https?://", "");
            int firstSlash = noProtocol.indexOf('/');
            if (firstSlash == -1) {
                throw new IllegalArgumentException("URL格式错误：" + path);
            }
            String bucketAndObject = noProtocol.substring(firstSlash + 1);
            int secondSlash = bucketAndObject.indexOf('/');
            if (secondSlash == -1) {
                throw new IllegalArgumentException("URL中缺少objectName：" + path);
            }
            bucket = bucketAndObject.substring(0, secondSlash);
            objectName = bucketAndObject.substring(secondSlash + 1);
        } else {
            int firstSlash = path.indexOf('/');
            if (firstSlash == -1) {
                throw new IllegalArgumentException("路径缺少objectName：" + path);
            }
            bucket = path.substring(0, firstSlash);
            objectName = path.substring(firstSlash + 1);
        }

        return new OssObjectInfo(bucket, objectName);
    }
}
