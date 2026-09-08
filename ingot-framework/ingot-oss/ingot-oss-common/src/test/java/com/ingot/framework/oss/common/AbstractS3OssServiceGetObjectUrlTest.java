package com.ingot.framework.oss.common;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.ingot.framework.commons.oss.OssService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>{@link AbstractS3OssService#getObjectURL(String)} 成功与失败回退的行为验证。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class AbstractS3OssServiceGetObjectUrlTest {

    private static final String ORIGINAL = "avatars/user/123.jpg";
    private static final String PRESIGNED = "https://oss.example/avatars/user/123.jpg?X-Amz-Signature=sig";
    private static final int DEFAULT_EXPIRED_SECONDS = 300;

    @Test
    @DisplayName("预签名成功时返回临时 URL")
    void returnsPresignedUrl() {
        RecordingS3Client client = new RecordingS3Client(PRESIGNED, null);
        OssService ossService = new TestOssService(client);

        assertThat(ossService.getObjectURL(ORIGINAL)).isEqualTo(PRESIGNED);
        assertThat(client.lastBucket).isEqualTo("avatars");
        assertThat(client.lastObjectName).isEqualTo("user/123.jpg");
        assertThat(client.lastDuration).isEqualTo(DEFAULT_EXPIRED_SECONDS);
        assertThat(client.callCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("自定义过期时间会传给客户端")
    void usesCustomExpiredSeconds() {
        RecordingS3Client client = new RecordingS3Client(PRESIGNED, null);
        OssService ossService = new TestOssService(client);

        assertThat(ossService.getObjectURL(ORIGINAL, 60)).isEqualTo(PRESIGNED);
        assertThat(client.lastDuration).isEqualTo(60);
    }

    @Test
    @DisplayName("null 与空串原样返回且不调用客户端")
    void returnsBlankAsIs() {
        RecordingS3Client client = new RecordingS3Client(PRESIGNED, null);
        OssService ossService = new TestOssService(client);

        assertThat(ossService.getObjectURL(null)).isNull();
        assertThat(ossService.getObjectURL("")).isEmpty();
        assertThat(client.callCount.get()).isZero();
    }

    @Test
    @DisplayName("路径无法解析时返回原始路径")
    void returnsOriginalWhenPathInvalid() {
        RecordingS3Client client = new RecordingS3Client(PRESIGNED, null);
        OssService ossService = new TestOssService(client);

        assertThat(ossService.getObjectURL("no-slash")).isEqualTo("no-slash");
        assertThat(client.callCount.get()).isZero();
    }

    @Test
    @DisplayName("SDK 异常时返回原始路径")
    void returnsOriginalWhenClientFails() {
        RecordingS3Client client = new RecordingS3Client(null, new RuntimeException("oss down"));
        OssService ossService = new TestOssService(client);

        assertThat(ossService.getObjectURL(ORIGINAL)).isEqualTo(ORIGINAL);
        assertThat(client.callCount.get()).isEqualTo(1);
    }

    private static final class TestOssService extends AbstractS3OssService {
        private final S3Client client;

        private TestOssService(S3Client client) {
            this.client = client;
        }

        @Override
        protected S3Client getS3Client() {
            return client;
        }

        @Override
        protected int getDefaultExpiredTime() {
            return DEFAULT_EXPIRED_SECONDS;
        }
    }

    private static final class RecordingS3Client implements S3Client {
        private final String presignedUrl;
        private final RuntimeException failure;
        private final AtomicInteger callCount = new AtomicInteger();
        private String lastBucket;
        private String lastObjectName;
        private int lastDuration;

        private RecordingS3Client(String presignedUrl, RuntimeException failure) {
            this.presignedUrl = presignedUrl;
            this.failure = failure;
        }

        @Override
        public String getPresignedObjectUrl(String bucketName, String objectName, int duration, TimeUnit unit) {
            callCount.incrementAndGet();
            lastBucket = bucketName;
            lastObjectName = objectName;
            lastDuration = duration;
            if (failure != null) {
                throw failure;
            }
            return presignedUrl;
        }

        @Override
        public void createBucket(String bucketName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean bucketExists(String bucketName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeBucket(String bucketName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putObject(String bucketName, String objectName, InputStream stream) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putObject(String bucketName, String objectName, InputStream stream, long size, String contentType) {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream getObject(String bucketName, String objectName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeObject(String bucketName, String objectName) {
            throw new UnsupportedOperationException();
        }
    }
}
