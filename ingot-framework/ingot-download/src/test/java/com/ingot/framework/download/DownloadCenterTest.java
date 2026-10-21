package com.ingot.framework.download;

import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import com.ingot.framework.commons.model.download.DownloadLink;
import com.ingot.framework.commons.model.download.DownloadTask;
import com.ingot.framework.commons.model.download.DownloadTaskStatus;
import com.ingot.framework.commons.model.iam.AuthorizationDomain;
import com.ingot.framework.commons.oss.OssService;
import com.ingot.framework.download.config.DownloadProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DownloadCenterTest {
    private static final String SOURCE = "test.export";
    private static final DownloadOwner OWNER = new DownloadOwner(AuthorizationDomain.TENANT, 10L, 20L);
    private static final DownloadOwner OTHER = new DownloadOwner(AuthorizationDomain.TENANT, 10L, 21L);
    private static final Instant NOW = Instant.parse("2026-10-21T01:00:00Z");
    private static final DownloadFile FILE = new DownloadFile("成员导出.xlsx", "application/xlsx", new byte[]{1, 2, 3});

    private final MemoryStore store = new MemoryStore();
    private final OssService oss = mock(OssService.class);
    private final DownloadProperties properties = new DownloadProperties();
    private final List<DownloadOwner> checked = new ArrayList<>();
    private final DownloadSourceAccess access = new DownloadSourceAccess() {
        @Override
        public String source() {
            return SOURCE;
        }

        @Override
        public void check(DownloadOwner owner) {
            checked.add(owner);
        }
    };

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void submitUploadsFileAndSucceeds() {
        long id = center(Runnable::run, NOW).submit(request(), () -> FILE);

        DownloadTaskRecord row = store.rows.get(id);
        assertThat(row.status()).isEqualTo(DownloadTaskStatus.SUCCEEDED);
        assertThat(row.objectPath()).isEqualTo("ingot-download/tenant/10/20/" + id + "/成员导出.xlsx");
        assertThat(row.fileSize()).isEqualTo(3L);
        assertThat(row.expiresAt()).isEqualTo(NOW.plus(properties.getRetention()));
        verify(oss).uploadFile(eq("ingot-download"), eq("tenant/10/20/" + id + "/成员导出.xlsx"), any(InputStream.class));
    }

    @Test
    void jobFailureKeepsDisplayReason() {
        DownloadCenter center = center(Runnable::run, NOW);
        long shown = center.submit(request(), () -> {
            throw new DownloadJobException("没有导出权限");
        });
        long hidden = center.submit(request(), () -> {
            throw new IllegalStateException("boom");
        });

        assertThat(store.rows.get(shown).failureReason()).isEqualTo("没有导出权限");
        assertThat(store.rows.get(hidden).failureReason()).isEqualTo(DownloadFailures.GENERATE);
        assertThat(center.get(OWNER, hidden).failureReason()).isEqualTo(DownloadFailures.GENERATE);
    }

    @Test
    void uploadFailureMarksStoreFailure() {
        when(oss.uploadFile(anyString(), anyString(), any())).thenThrow(new RuntimeException("down"));

        long id = center(Runnable::run, NOW).submit(request(), () -> FILE);

        assertThat(store.rows.get(id).status()).isEqualTo(DownloadTaskStatus.FAILED);
        assertThat(store.rows.get(id).failureReason()).isEqualTo(DownloadFailures.STORE);
    }

    @Test
    void lostResultRemovesUploadedObject() {
        store.rejectSucceeded = true;

        long id = center(Runnable::run, NOW).submit(request(), () -> FILE);

        verify(oss).removeFile("ingot-download", "tenant/10/20/" + id + "/成员导出.xlsx");
        assertThat(store.rows.get(id).failureReason()).isEqualTo(DownloadFailures.STORE);
    }

    @Test
    void rejectedExecutionMarksBusy() {
        long id = center(task -> {
            throw new RejectedExecutionException();
        }, NOW).submit(request(), () -> FILE);

        assertThat(store.rows.get(id).failureReason()).isEqualTo(DownloadFailures.BUSY);
    }

    @Test
    void submitInsideTransactionWaitsForCommit() {
        TransactionSynchronizationManager.initSynchronization();
        List<Runnable> queued = new ArrayList<>();

        long id = center(queued::add, NOW).submit(request(), () -> FILE);

        assertThat(queued).isEmpty();
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            synchronization.afterCommit();
        }
        assertThat(queued).hasSize(1);
        queued.getFirst().run();
        assertThat(store.rows.get(id).status()).isEqualTo(DownloadTaskStatus.SUCCEEDED);
    }

    @Test
    void unregisteredSourceCannotSubmit() {
        DownloadCenter center = center(Runnable::run, NOW);

        assertThatThrownBy(() -> center.submit(new DownloadRequest(OWNER, "unknown", "x"), () -> FILE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void linkChecksOwnerStatusAndAccess() {
        long id = center(Runnable::run, NOW).submit(request(), () -> FILE);
        when(oss.getObjectURL(anyString(), anyInt())).thenReturn("https://oss/signed");
        DownloadCenter center = center(Runnable::run, NOW);

        assertError(() -> center.link(OTHER, id), DownloadCenterError.NOT_FOUND);
        DownloadLink link = center.link(OWNER, id);

        assertThat(link.url()).isEqualTo("https://oss/signed");
        assertThat(link.filename()).isEqualTo("成员导出.xlsx");
        assertThat(link.expiresAt()).isEqualTo(NOW.plus(properties.getLinkTtl()));
        assertThat(checked).containsExactly(OWNER);
        verify(oss).getObjectURL(store.rows.get(id).objectPath(), (int) properties.getLinkTtl().toSeconds());
    }

    @Test
    void linkRejectsActiveTasksAndUnsignedPaths() {
        List<Runnable> queued = new ArrayList<>();
        DownloadCenter center = center(queued::add, NOW);
        long pending = center.submit(request(), () -> FILE);
        assertError(() -> center.link(OWNER, pending), DownloadCenterError.NOT_READY);

        queued.getFirst().run();
        when(oss.getObjectURL(anyString(), anyInt())).thenAnswer(call -> call.getArgument(0));
        assertError(() -> center.link(OWNER, pending), DownloadCenterError.LINK_UNAVAILABLE);
    }

    @Test
    void accessDenialPropagates() {
        long id = center(Runnable::run, NOW).submit(request(), () -> FILE);
        DownloadSourceAccess denied = new DownloadSourceAccess() {
            @Override
            public String source() {
                return SOURCE;
            }

            @Override
            public void check(DownloadOwner owner) {
                throw new IllegalArgumentException("denied");
            }
        };
        DownloadCenter center = new DownloadCenter(store, oss, properties, Runnable::run, List.of(denied),
                Clock.fixed(NOW, ZoneOffset.UTC));

        assertThatThrownBy(() -> center.link(OWNER, id)).hasMessage("denied");
        verify(oss, never()).getObjectURL(anyString(), anyInt());
    }

    @Test
    void expiredFileShowsExpiredAndCannotDownload() {
        long id = center(Runnable::run, NOW).submit(request(), () -> FILE);
        DownloadCenter later = center(Runnable::run, NOW.plus(properties.getRetention()));

        DownloadTask task = later.get(OWNER, id);

        assertThat(task.status()).isEqualTo(DownloadTaskStatus.EXPIRED);
        assertError(() -> later.link(OWNER, id), DownloadCenterError.NOT_FOUND);
    }

    @Test
    void pageListsOwnTasksNewestFirst() {
        DownloadCenter center = center(Runnable::run, NOW);
        long first = center.submit(request(), () -> FILE);
        long second = center.submit(request(), () -> FILE);
        center.submit(new DownloadRequest(OTHER, SOURCE, "成员导出"), () -> FILE);

        DownloadTaskPage page = center.page(OWNER, 1, 20);

        assertThat(page.total()).isEqualTo(2);
        assertThat(page.items()).extracting(DownloadTask::id)
                .containsExactly(String.valueOf(second), String.valueOf(first));
    }

    @Test
    void cleanupRemovesExpiredFailsStaleAndPurges() {
        long id = center(Runnable::run, NOW).submit(request(), () -> FILE);
        String path = store.rows.get(id).objectPath();
        DownloadCenter later = center(Runnable::run, NOW.plus(properties.getRetention()));
        doThrow(new RuntimeException("retry")).doNothing().when(oss).removeFile(anyString(), anyString());

        later.cleanup();
        assertThat(store.rows.get(id).status()).isEqualTo(DownloadTaskStatus.SUCCEEDED);
        later.cleanup();

        assertThat(store.rows.get(id).status()).isEqualTo(DownloadTaskStatus.EXPIRED);
        assertThat(store.rows.get(id).objectPath()).isNull();
        verify(oss, org.mockito.Mockito.times(2)).removeFile("ingot-download", path.substring(path.indexOf('/') + 1));
        assertThat(store.staleBefore).isEqualTo(NOW.plus(properties.getRetention()).minus(properties.getRunTimeout()));
        assertThat(store.purgeBefore).isEqualTo(NOW.plus(properties.getRetention()).minus(properties.getRecordRetention()));
    }

    private DownloadCenter center(Executor executor, Instant now) {
        return new DownloadCenter(store, oss, properties, executor, List.of(access), Clock.fixed(now, ZoneOffset.UTC));
    }

    private static DownloadRequest request() {
        return new DownloadRequest(OWNER, SOURCE, "成员导出");
    }

    private static void assertError(Runnable call, DownloadCenterError error) {
        assertThatThrownBy(call::run).isInstanceOfSatisfying(DownloadCenterException.class,
                exception -> assertThat(exception.getError()).isEqualTo(error));
    }

    private static final class MemoryStore implements DownloadTaskStore {
        private final Map<Long, DownloadTaskRecord> rows = new LinkedHashMap<>();
        private final AtomicLong ids = new AtomicLong();
        private boolean rejectSucceeded;
        private Instant staleBefore;
        private Instant purgeBefore;

        @Override
        public long nextId() {
            return ids.incrementAndGet();
        }

        @Override
        public void insert(DownloadTaskRecord task) {
            rows.put(task.id(), task);
        }

        @Override
        public boolean markRunning(long id, Instant startedAt) {
            DownloadTaskRecord row = rows.get(id);
            if (row == null || row.status() != DownloadTaskStatus.PENDING) {
                return false;
            }
            rows.put(id, copy(row, DownloadTaskStatus.RUNNING, null, null, null, null, null, startedAt, null));
            return true;
        }

        @Override
        public boolean markSucceeded(long id, String filename, String contentType, long fileSize, String objectPath,
                Instant completedAt) {
            DownloadTaskRecord row = rows.get(id);
            if (rejectSucceeded || row == null || row.status() != DownloadTaskStatus.RUNNING) {
                return false;
            }
            rows.put(id, copy(row, DownloadTaskStatus.SUCCEEDED, filename, contentType, fileSize, objectPath, null,
                    row.startedAt(), completedAt));
            return true;
        }

        @Override
        public boolean markFailed(long id, String reason, Instant completedAt) {
            DownloadTaskRecord row = rows.get(id);
            if (row == null || !row.status().active()) {
                return false;
            }
            rows.put(id, copy(row, DownloadTaskStatus.FAILED, null, null, null, null, reason, row.startedAt(),
                    completedAt));
            return true;
        }

        @Override
        public DownloadTaskRecord find(DownloadOwner owner, long id) {
            DownloadTaskRecord row = rows.get(id);
            return row != null && row.owner().equals(owner) ? row : null;
        }

        @Override
        public long count(DownloadOwner owner) {
            return rows.values().stream().filter(row -> row.owner().equals(owner)).count();
        }

        @Override
        public List<DownloadTaskRecord> page(DownloadOwner owner, long offset, int limit) {
            return rows.values().stream().filter(row -> row.owner().equals(owner))
                    .sorted(Comparator.comparingLong(DownloadTaskRecord::id).reversed())
                    .skip(offset).limit(limit).toList();
        }

        @Override
        public List<DownloadTaskRecord> listExpired(Instant now, int limit) {
            return rows.values().stream().filter(row -> row.status() == DownloadTaskStatus.SUCCEEDED
                    && !row.expiresAt().isAfter(now)).limit(limit).toList();
        }

        @Override
        public boolean markExpired(long id) {
            DownloadTaskRecord row = rows.get(id);
            rows.put(id, copy(row, DownloadTaskStatus.EXPIRED, row.filename(), row.contentType(), row.fileSize(), null,
                    null, row.startedAt(), row.completedAt()));
            return true;
        }

        @Override
        public int failStale(Instant createdBefore, String reason, Instant completedAt) {
            staleBefore = createdBefore;
            return 0;
        }

        @Override
        public int purge(Instant createdBefore) {
            purgeBefore = createdBefore;
            return 0;
        }

        private static DownloadTaskRecord copy(DownloadTaskRecord row, DownloadTaskStatus status, String filename,
                String contentType, Long size, String path, String reason, Instant startedAt, Instant completedAt) {
            return new DownloadTaskRecord(row.id(), row.owner(), row.source(), row.title(), status, filename,
                    contentType, size, path, reason, row.createdAt(), startedAt, completedAt, row.expiresAt());
        }
    }
}
