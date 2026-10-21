package com.ingot.cloud.iam.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.framework.commons.model.download.DownloadTaskStatus;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.download.DownloadFailures;
import com.ingot.framework.download.DownloadOwner;
import com.ingot.framework.download.DownloadTaskRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * <p>验证下载任务按域、租户与成员隔离，状态更新带前置状态，清理只作用于对应状态。</p>
 *
 * @author jy
 * @since 1.0.0
 */
class IamDownloadTaskStoreTest {
    private static final DownloadOwner OWNER = new DownloadOwner(AuthorizationDomain.TENANT, 10L, 101L);
    private static final DownloadOwner OTHER_MEMBER = new DownloadOwner(AuthorizationDomain.TENANT, 10L, 102L);
    private static final DownloadOwner OTHER_TENANT = new DownloadOwner(AuthorizationDomain.TENANT, 11L, 101L);
    private static final Instant NOW = Instant.parse("2026-10-21T01:00:00Z");

    private IamDownloadTaskStore store;

    @BeforeEach
    void database() {
        DriverManagerDataSource source = new DriverManagerDataSource(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        new JdbcTemplate(source).execute("""
                CREATE TABLE iam_download_task(id BIGINT PRIMARY KEY, domain VARCHAR(16), tenant_id BIGINT,
                  actor_member_id BIGINT, source VARCHAR(64), title VARCHAR(128), status VARCHAR(16),
                  filename VARCHAR(255), content_type VARCHAR(128), file_size BIGINT, object_path VARCHAR(512),
                  failure_reason VARCHAR(255), created_at TIMESTAMP, started_at TIMESTAMP, completed_at TIMESTAMP,
                  expires_at TIMESTAMP)
                """);
        AtomicLong ids = new AtomicLong(100);
        store = IamMybatisTestAccess.downloads(source, ids::incrementAndGet);
    }

    @Test
    void ownerFilterIsolatesMemberTenantAndOrdersNewestFirst() {
        long first = insert(OWNER, NOW);
        long second = insert(OWNER, NOW.plusSeconds(60));
        insert(OTHER_MEMBER, NOW);
        insert(OTHER_TENANT, NOW);

        assertEquals(2, store.count(OWNER));
        assertEquals(List.of(second, first), store.page(OWNER, 0, 20).stream().map(DownloadTaskRecord::id).toList());
        assertEquals(List.of(first), store.page(OWNER, 1, 1).stream().map(DownloadTaskRecord::id).toList());
        assertNull(store.find(OTHER_MEMBER, first));
        assertNull(store.find(OTHER_TENANT, first));
        assertEquals(OWNER, store.find(OWNER, first).owner());
    }

    @Test
    void statusTransitionsRequireExpectedState() {
        long id = insert(OWNER, NOW);

        assertFalse(store.markSucceeded(id, "a.xlsx", "x", 3, "b/o", NOW));
        assertTrue(store.markRunning(id, NOW));
        assertFalse(store.markRunning(id, NOW));
        assertTrue(store.markSucceeded(id, "a.xlsx", "x", 3, "b/o", NOW));
        assertFalse(store.markFailed(id, DownloadFailures.STORE, NOW));

        DownloadTaskRecord row = store.find(OWNER, id);
        assertEquals(DownloadTaskStatus.SUCCEEDED, row.status());
        assertEquals("b/o", row.objectPath());
        assertEquals(3L, row.fileSize());
        assertEquals(NOW, row.completedAt());
    }

    @Test
    void cleanupTouchesOnlyMatchingStates() {
        long expired = insert(OWNER, NOW.minusSeconds(3600));
        store.markRunning(expired, NOW);
        store.markSucceeded(expired, "a.xlsx", "x", 1, "b/o", NOW);
        long stale = insert(OWNER, NOW.minusSeconds(7200));
        long fresh = insert(OWNER, NOW);

        assertEquals(List.of(expired), store.listExpired(NOW.plusSeconds(7200), 10).stream()
                .map(DownloadTaskRecord::id).toList());
        assertTrue(store.markExpired(expired));
        assertNull(store.find(OWNER, expired).objectPath());
        assertEquals(1, store.failStale(NOW.minusSeconds(60), DownloadFailures.STALE, NOW));
        assertEquals(DownloadTaskStatus.FAILED, store.find(OWNER, stale).status());
        assertEquals(DownloadTaskStatus.PENDING, store.find(OWNER, fresh).status());
        assertEquals(2, store.purge(NOW.minusSeconds(60)));
        assertEquals(1, store.count(OWNER));
    }

    private long insert(DownloadOwner owner, Instant createdAt) {
        long id = store.nextId();
        store.insert(new DownloadTaskRecord(id, owner, "iam.member-workbook", "成员导出", DownloadTaskStatus.PENDING,
                null, null, null, null, null, createdAt, null, null, createdAt.plusSeconds(3600)));
        return id;
    }
}
