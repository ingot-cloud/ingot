package com.ingot.framework.oss.upload;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.AuthorizationContext;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.oss.common.OssPathParser;
import com.ingot.framework.oss.common.S3Client;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * <p>将用途已获授权的图片暂存于私有存储，凭证随业务引用同事务消费。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public class StagedImageUploads {
    private static final Duration VALIDITY = Duration.ofMinutes(30);
    private static final long CLEANUP_INTERVAL_MS = 60_000;
    private static final int CLEANUP_BATCH_SIZE = 50;
    private static final String PENDING = "PENDING";
    private static final String READY = "READY";
    private static final String REFERENCE_PREFIX = "asset-upload/";
    private static final String OBJECT_PREFIX = "images/";
    private static final Pattern ICON = Pattern.compile("[a-z0-9-]+:[a-z0-9-]+");
    private static final Pattern UPLOAD_ID = Pattern.compile("[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}");
    private static final String LOCK_UPLOAD = """
            SELECT owner_key,purpose,target_key,bucket_name,object_name,state,expires_at
              FROM asset_upload WHERE id=? FOR UPDATE
            """;

    private final JdbcTemplate jdbc;
    private final S3Client storage;
    private final TransactionTemplate transactions;
    private final String bucket;

    /** 使用调用业务服务的库，确保凭证和业务引用可以原子提交。 */
    public StagedImageUploads(JdbcTemplate jdbc, S3Client storage, PlatformTransactionManager manager, String bucket) {
        this.jdbc = jdbc;
        this.storage = storage;
        this.transactions = new TransactionTemplate(manager);
        this.bucket = bucket;
    }

    /** 以服务器从认证上下文恢复的域、组织及成员组成凭证归属。 */
    public static String iamOwner(AuthorizationContext identity) {
        return identity.domain().name() + "/" + Objects.toString(identity.tenantId(), "") + "/" + identity.memberId();
    }

    /**
     * 上传已由业务入口授权的图片，返回限时凭证。
     * @param owner 可信上传者
     * @param purpose 服务器认可的用途编码
     * @param target 已有目标的可信标识，新建时为空
     * @param body 图片流，由调用者关闭
     * @return 上传凭证
     */
    public AssetUpload stage(String owner, String purpose, String target, InputStream body) {
        var image = ImageUploadInput.read(body);
        String id = UUID.randomUUID().toString();
        String object = OBJECT_PREFIX + id + "." + image.extension();
        jdbc.update("""
                INSERT INTO asset_upload (id,owner_key,purpose,target_key,bucket_name,object_name,state,expires_at)
                VALUES (?,?,?,?,?,?,?,?)
                """, id, owner, purpose, key(target), bucket, object, PENDING,
                Timestamp.from(Instant.now().plus(VALIDITY)));
        transactions.executeWithoutResult(status -> {
            Row pending = lock(id);
            if (pending == null || !pending.expiresAt().isAfter(Instant.now())) {
                throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
            }
            try {
                storage.putObject(bucket, object, new ByteArrayInputStream(image.bytes()),
                        image.bytes().length, "image/" + image.extension());
            } catch (Exception failure) {
                throw new IllegalStateException("暂存图片上传失败", failure);
            }
            jdbc.update("UPDATE asset_upload SET state=? WHERE id=? AND state=?", READY, id, PENDING);
        });
        return new AssetUpload(REFERENCE_PREFIX + id);
    }

    /** 判断表单是否提交了需要消费的上传凭证。 */
    public static boolean isReference(String submitted) {
        return submitted != null && submitted.startsWith(REFERENCE_PREFIX);
    }

    /**
     * 保留原图、按既有字段语义清除或原子消费凭证。原图地址只能用于维持已有引用。
     * @param owner 当前可信操作者
     * @param purpose 业务方法认可的上传用途
     * @param target 本次已校验目标
     * @param submitted 表单值或上传凭证
     * @param current 已有业务图片路径
     * @param iconAllowed 是否允许 Iconify 名
     * @return 可入库的相对路径、图标名或空值
     */
    public String bind(String owner, String purpose, String target, String submitted, String current, boolean iconAllowed) {
        if (submitted == null || submitted.isBlank()) return submitted;
        if (iconAllowed && ICON.matcher(submitted).matches()) return submitted;
        if (current != null && samePath(submitted, current)) return current;
        if (!isReference(submitted)) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        String id = submitted.substring(REFERENCE_PREFIX.length());
        if (!UPLOAD_ID.matcher(id).matches()) throw new BizException(IamReasonCode.INVALID_ARGUMENT);
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("图片绑定必须与业务写入使用同一事务");
        }
        Row row = lock(id);
        if (row == null || !READY.equals(row.state()) || !row.expiresAt().isAfter(Instant.now())
                || !row.owner().equals(owner) || !row.purpose().equals(purpose) || !row.target().equals(key(target))) {
            throw new BizException(IamReasonCode.OBJECT_NOT_FOUND);
        }
        jdbc.update("DELETE FROM asset_upload WHERE id=?", id);
        return row.bucket() + "/" + row.object();
    }

    /** 清理未绑定到业务记录的过期对象，失败后保留记录供下一轮重试。 */
    @Scheduled(fixedDelay = CLEANUP_INTERVAL_MS)
    public void cleanExpired() {
        var now = Instant.now();
        var ids = jdbc.queryForList("SELECT id FROM asset_upload WHERE expires_at<=? ORDER BY expires_at LIMIT ?",
                String.class, Timestamp.from(now), CLEANUP_BATCH_SIZE);
        for (String id : ids) {
            try {
                transactions.executeWithoutResult(status -> {
                    Row row = lock(id);
                    if (row != null && !row.expiresAt().isAfter(now)) {
                        try {
                            storage.removeObject(row.bucket(), row.object());
                        } catch (Exception failure) {
                            throw new IllegalStateException("暂存图片删除失败", failure);
                        }
                        jdbc.update("DELETE FROM asset_upload WHERE id=?", id);
                    }
                });
            } catch (RuntimeException failure) {
                log.warn("暂存图片清理失败，等待重试，上传 ID {}", id, failure);
            }
        }
    }

    private Row lock(String id) {
        return jdbc.query(LOCK_UPLOAD, rs -> rs.next() ? new Row(rs.getString("owner_key"),
                rs.getString("purpose"), rs.getString("target_key"), rs.getString("bucket_name"),
                rs.getString("object_name"), rs.getString("state"), rs.getTimestamp("expires_at").toInstant())
                : null, id);
    }

    private static boolean samePath(String submitted, String current) {
        if (current.equals(submitted)) return true;
        if (submitted.startsWith("http://") || submitted.startsWith("https://")) {
            var object = OssPathParser.parse(submitted);
            return current.equals(object.bucket() + "/" + object.objectName());
        }
        return false;
    }

    private static String key(String target) {
        return Objects.toString(target, "");
    }

    /** <p>锁定的凭证及暂存对象元数据。</p> */
    private record Row(String owner, String purpose, String target, String bucket, String object,
                       String state, Instant expiresAt) {
    }
}
