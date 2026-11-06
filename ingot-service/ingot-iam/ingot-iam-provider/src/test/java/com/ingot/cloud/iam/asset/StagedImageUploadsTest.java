package com.ingot.cloud.iam.asset;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.Duration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.oss.common.S3Client;
import com.ingot.framework.oss.minio.service.MinioService;
import com.ingot.framework.oss.upload.StagedImageUploads;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/** 使用同一事务数据源验证业务绑定回滚、并发与上传隔离。 */
class StagedImageUploadsTest {
    private static final String OWNER = "TENANT/10/11";
    private static final String PURPOSE = "tenant-member-update";
    private static final String TARGET = "12";
    private final S3Client storage = mock(S3Client.class);
    private JdbcTemplate jdbc;
    private TransactionTemplate transaction;
    private DataSourceTransactionManager manager;
    private StagedImageUploads uploads;

    @BeforeEach
    void setUp() {
        var source = new DriverManagerDataSource("jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        manager = new DataSourceTransactionManager(source);
        transaction = new TransactionTemplate(manager);
        jdbc = new JdbcTemplate(source);
        jdbc.execute("""
                CREATE TABLE asset_upload (id VARCHAR(36) PRIMARY KEY, owner_key VARCHAR(128), purpose VARCHAR(64),
                target_key VARCHAR(64), bucket_name VARCHAR(128), object_name VARCHAR(256),
                state VARCHAR(16), expires_at TIMESTAMP)
                """);
        uploads = new StagedImageUploads(jdbc, storage, manager, "private-assets");
    }

    @Test
    void validatesImageBytesBeforeStaging() {
        assertThrows(BizException.class, () -> uploads.stage(OWNER, PURPOSE, TARGET,
                new ByteArrayInputStream(new byte[] {1, 2, 3})));
        assertThrows(BizException.class, () -> uploads.stage(OWNER, PURPOSE, TARGET,
                new ByteArrayInputStream(new byte[5 * 1024 * 1024 + 1])));
        verifyNoInteractions(storage);
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM asset_upload", Integer.class));
    }

    @Test
    void rejectsWrongOwnerPurposeTargetAndArbitraryPathsBeforeConsuming() throws Exception {
        String token = stage();
        assertDenied("TENANT/20/11", PURPOSE, TARGET, token);
        assertDenied("TENANT/10/13", PURPOSE, TARGET, token);
        assertDenied(OWNER, "tenant-member-rejoin", TARGET, token);
        assertDenied(OWNER, PURPOSE, "13", token);
        assertThrows(BizException.class, () -> transaction.execute(status ->
                uploads.bind(OWNER, PURPOSE, TARGET, "foreign-bucket/untrusted.png", null, false)));
        String path = bind(token);
        assertTrue(path.startsWith("private-assets/images/") && path.endsWith(".png"));
        assertDenied(OWNER, PURPOSE, TARGET, token);
    }

    @Test
    void rollsBackConsumptionAndSerializesConcurrentBinds() throws Exception {
        String token = stage();
        transaction.executeWithoutResult(status -> {
            uploads.bind(OWNER, PURPOSE, TARGET, token, null, false);
            status.setRollbackOnly();
        });
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> tryBind(token));
            var second = executor.submit(() -> tryBind(token));
            assertEquals(1, first.get() + second.get());
        }
        String expired = stage();
        jdbc.update("UPDATE asset_upload SET expires_at=?", Timestamp.from(Instant.EPOCH));
        assertDenied(OWNER, PURPOSE, TARGET, expired);
        uploads.cleanExpired();
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM asset_upload", Integer.class));
    }

    @Test
    @EnabledIfEnvironmentVariable(named = "IAM_TEST_MINIO_ENDPOINT", matches = "http://127\\.0\\.0\\.1:[0-9]+")
    void boundImageRequiresSignedReadAndExpiredUploadIsDeletedFromStorage() throws Exception {
        String endpoint = System.getenv("IAM_TEST_MINIO_ENDPOINT");
        String bucket = "private-assets";
        var minio = new MinioService(endpoint, "asset-fixture", "asset-fixture-secret", "us-east-1",
                Duration.ofSeconds(5));
        minio.afterPropertiesSet();
        minio.createBucket(bucket);
        uploads = new StagedImageUploads(jdbc, minio, manager, bucket);
        String path = bind(stage());
        String object = path.substring(bucket.length() + 1);
        try (var client = HttpClient.newHttpClient()) {
            var unsigned = HttpRequest.newBuilder(URI.create(endpoint + "/" + path)).GET().build();
            assertEquals(403, client.send(unsigned, HttpResponse.BodyHandlers.discarding()).statusCode());
            var signed = HttpRequest.newBuilder(URI.create(
                    minio.getPresignedObjectUrl(bucket, object, 60, TimeUnit.SECONDS))).GET().build();
            var response = client.send(signed, HttpResponse.BodyHandlers.ofByteArray());
            assertEquals(200, response.statusCode());
            assertEquals("image/png", response.headers().firstValue("Content-Type").orElseThrow());
            assertTrue(response.body().length > 0);
        }
        stage();
        String orphan = jdbc.queryForObject("SELECT object_name FROM asset_upload", String.class);
        try (var input = minio.getObject(bucket, orphan)) {
            assertTrue(input.read() >= 0);
        }
        jdbc.update("UPDATE asset_upload SET expires_at=?", Timestamp.from(Instant.EPOCH));
        uploads.cleanExpired();
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM asset_upload", Integer.class));
        assertThrows(RuntimeException.class, () -> minio.getObject(bucket, orphan));
        try (var input = minio.getObject(bucket, object)) {
            assertTrue(input.read() >= 0);
        }
    }

    private String stage() throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return uploads.stage(OWNER, PURPOSE, TARGET, new ByteArrayInputStream(bytes.toByteArray())).uploadId();
    }

    private void assertDenied(String owner, String purpose, String target, String token) {
        BizException error = assertThrows(BizException.class, () -> transaction.execute(status ->
                uploads.bind(owner, purpose, target, token, null, false)));
        assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), error.getCode());
    }

    private String bind(String token) {
        return transaction.execute(status -> uploads.bind(OWNER, PURPOSE, TARGET, token, null, false));
    }

    private int tryBind(String token) {
        try {
            bind(token);
            return 1;
        } catch (BizException failure) {
            assertEquals(IamReasonCode.OBJECT_NOT_FOUND.getCode(), failure.getCode());
            return 0;
        }
    }
}
