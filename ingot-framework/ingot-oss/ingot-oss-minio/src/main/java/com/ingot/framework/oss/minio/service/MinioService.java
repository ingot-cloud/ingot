package com.ingot.framework.oss.minio.service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.ingot.framework.oss.common.S3Client;
import com.ingot.framework.oss.minio.common.MinioItem;
import io.minio.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * <p>Description  : MinioService.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019-08-27.</p>
 * <p>Time         : 14:34.</p>
 */
@RequiredArgsConstructor
public class MinioService implements S3Client, InitializingBean {
    private final String endpoint;
    private final String accessKey;
    private final String secretKey;
    private MinioClient client;

    @Override
    public void createBucket(String bucketName) {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            return client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取全部bucket
     * <p>
     * <a href="https://docs.minio.io/cn/java-client-api-reference.html#listBuckets">listBuckets</a>
     */
    public List<Bucket> getAllBuckets() {
        try {
            return client.listBuckets();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param bucketName bucket名称
     */
    public Optional<Bucket> getBucket(String bucketName) {
        try {
            return client.listBuckets().stream().filter(b -> b.name().equals(bucketName)).findFirst();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void removeBucket(String bucketName) {
        try {
            client.removeBucket(RemoveBucketArgs.builder().bucket(bucketName).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据文件前置查询文件
     *
     * @param bucketName bucket名称
     * @param prefix     前缀
     * @param recursive  是否递归查询
     * @return MinioItem 列表
     */
    public List<MinioItem> getAllObjectsByPrefix(String bucketName, String prefix, boolean recursive) {
        List<MinioItem> objectList = new ArrayList<>();
        Iterable<Result<Item>> objectsIterator = client
                .listObjects(ListObjectsArgs.builder()
                        .bucket(bucketName).prefix(prefix).recursive(recursive).build());

        while (objectsIterator.iterator().hasNext()) {
            try {
                objectList.add(new MinioItem(objectsIterator.iterator().next().get()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return objectList;
    }

    @Override
    public String getPresignedObjectUrl(String bucketName, String objectName, int duration, TimeUnit unit) {
        try {
            return client.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .object(objectName).expiry(duration, unit).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取文件外链（兼容旧方法名）
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @param duration   过期时间
     * @param unit       单位
     * @return url
     */
    public String getObjectURL(String bucketName, String objectName, int duration, TimeUnit unit) {
        return getPresignedObjectUrl(bucketName, objectName, duration, unit);
    }

    @Override
    public InputStream getObject(String bucketName, String objectName) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void putObject(String bucketName, String objectName, InputStream stream) throws Exception {
        client.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(stream, stream.available(), -1)
                .contentType("application/octet-stream").build());
    }

    @Override
    public void putObject(String bucketName, String objectName, InputStream stream, long size, String contentType) throws Exception {
        client.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(stream, size, -1)
                .contentType(contentType).build());
    }

    /**
     * 获取文件信息
     *
     * @param bucketName bucket名称
     * @param objectName 文件名称
     * @throws Exception <a href="https://docs.minio.io/cn/java-client-api-reference.html#statObject">statObject</a>
     */
    public StatObjectResponse getObjectInfo(String bucketName, String objectName) throws Exception {
        return client.statObject(StatObjectArgs.builder()
                .bucket(bucketName).object(objectName).build());
    }

    @Override
    public void removeObject(String bucketName, String objectName) throws Exception {
        client.removeObject(RemoveObjectArgs.builder()
                .bucket(bucketName).object(objectName).build());
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.hasText(endpoint, "Minio url 为空");
        Assert.hasText(accessKey, "Minio accessKey为空");
        Assert.hasText(secretKey, "Minio secretKey为空");
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey).build();
    }

}
