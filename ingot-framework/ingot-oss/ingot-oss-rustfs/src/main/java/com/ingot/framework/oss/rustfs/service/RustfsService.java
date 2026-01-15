package com.ingot.framework.oss.rustfs.service;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import com.ingot.framework.oss.common.S3Client;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * <p>Description  : RustfsService - 基于AWS S3 SDK实现的RustFS客户端.</p>
 * <p>Author       : jy.</p>
 * <p>Date         : 2026/1/12.</p>
 * <p>Time         : 10:30.</p>
 */
@RequiredArgsConstructor
public class RustfsService implements S3Client, InitializingBean {
    private final String endpoint;
    private final String accessKey;
    private final String secretKey;
    private final String region;

    private software.amazon.awssdk.services.s3.S3Client awsS3Client;
    private S3Presigner presigner;

    @Override
    public void createBucket(String bucketName) {
        try {
            if (!bucketExists(bucketName)) {
                CreateBucketRequest request = CreateBucketRequest.builder()
                        .bucket(bucketName)
                        .build();
                awsS3Client.createBucket(request);
            }
        } catch (Exception e) {
            throw new RuntimeException("创建Bucket失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean bucketExists(String bucketName) {
        try {
            HeadBucketRequest request = HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            awsS3Client.headBucket(request);
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("检查Bucket是否存在失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeBucket(String bucketName) {
        try {
            DeleteBucketRequest request = DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build();
            awsS3Client.deleteBucket(request);
        } catch (Exception e) {
            throw new RuntimeException("删除Bucket失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void putObject(String bucketName, String objectName, InputStream stream) throws Exception {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .contentType("application/octet-stream")
                    .build();

            awsS3Client.putObject(request, RequestBody.fromInputStream(stream, stream.available()));
        } catch (Exception e) {
            throw new Exception("上传对象失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void putObject(String bucketName, String objectName, InputStream stream, long size, String contentType) throws Exception {
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .contentType(contentType)
                    .contentLength(size)
                    .build();

            awsS3Client.putObject(request, RequestBody.fromInputStream(stream, size));
        } catch (Exception e) {
            throw new Exception("上传对象失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream getObject(String bucketName, String objectName) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .build();

            return awsS3Client.getObject(request);
        } catch (Exception e) {
            throw new RuntimeException("获取对象失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getPresignedObjectUrl(String bucketName, String objectName, int duration, TimeUnit unit) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(unit.toSeconds(duration)))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = presigner.presignGetObject(presignRequest);
            return presignedRequest.url().toString();
        } catch (Exception e) {
            throw new RuntimeException("获取预签名URL失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void removeObject(String bucketName, String objectName) throws Exception {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectName)
                    .build();

            awsS3Client.deleteObject(request);
        } catch (Exception e) {
            throw new Exception("删除对象失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.hasText(endpoint, "RustFS url 为空");
        Assert.hasText(accessKey, "RustFS accessKey为空");
        Assert.hasText(secretKey, "RustFS secretKey为空");
        Assert.hasText(region, "RustFS region为空");

        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);

        this.awsS3Client = software.amazon.awssdk.services.s3.S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region))
                .forcePathStyle(true) // RustFS 需启用 Path-Style，S3 默认virtual-host-style，http://{bucket}.{endpoint}/{object}
                .build();

        this.presigner = S3Presigner.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region))
                .serviceConfiguration(
                        S3Configuration.builder()
                                .pathStyleAccessEnabled(true)  // S3 默认virtual-host-style，http://{bucket}.{endpoint}/{object}
                                .build()
                )
                .build();
    }
}
