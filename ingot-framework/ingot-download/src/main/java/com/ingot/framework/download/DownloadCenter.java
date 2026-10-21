package com.ingot.framework.download;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import com.ingot.framework.commons.model.download.DownloadLink;
import com.ingot.framework.commons.model.download.DownloadTask;
import com.ingot.framework.commons.model.download.DownloadTaskStatus;
import com.ingot.framework.commons.oss.OssService;
import com.ingot.framework.download.config.DownloadProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * <p>下载任务门面：登记后异步生成并上传对象存储，按归属列出与签发链接，定期清理到期文件。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
public class DownloadCenter {
    private static final char PATH_SEPARATOR = '/';
    private static final String NO_TENANT = "-";

    private final DownloadTaskStore store;
    private final OssService oss;
    private final DownloadProperties properties;
    private final Executor executor;
    private final Map<String, DownloadSourceAccess> accesses;
    private final Clock clock;

    /**
     * 绑定存储、对象存储与执行器。
     * <p>执行器需在提交时绑定当前认证，来源复核需建索引，Lombok 无法表达，保留显式构造器。</p>
     *
     * @param store 任务存储
     * @param oss 对象存储
     * @param properties 配置
     * @param executor 生成文件的执行器
     * @param accesses 各来源的权限复核
     * @param clock 时钟
     */
    public DownloadCenter(DownloadTaskStore store, OssService oss, DownloadProperties properties, Executor executor,
            List<DownloadSourceAccess> accesses, Clock clock) {
        this.store = store;
        this.oss = oss;
        this.properties = properties;
        this.executor = new DelegatingSecurityContextExecutor(executor);
        Map<String, DownloadSourceAccess> index = new LinkedHashMap<>();
        for (DownloadSourceAccess access : accesses == null ? List.<DownloadSourceAccess>of() : accesses) {
            if (index.putIfAbsent(access.source(), access) != null) {
                throw new IllegalStateException("下载来源重复注册: " + access.source());
            }
        }
        this.accesses = Map.copyOf(index);
        this.clock = clock;
    }

    /**
     * 登记任务并在当前事务提交后开始生成；无事务时立即开始。
     *
     * @param request 归属与展示信息
     * @param job 生成文件
     * @return 任务 ID
     * @throws IllegalStateException 来源未注册权限复核
     */
    public long submit(DownloadRequest request, DownloadJob job) {
        if (!accesses.containsKey(request.source())) {
            throw new IllegalStateException("下载来源未注册权限复核: " + request.source());
        }
        Instant now = clock.instant();
        long id = store.nextId();
        store.insert(new DownloadTaskRecord(id, request.owner(), request.source(), request.title(),
                DownloadTaskStatus.PENDING, null, null, null, null, null, now, null, null,
                now.plus(properties.getRetention())));
        Runnable dispatch = () -> dispatch(id, request.owner(), job);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatch.run();
                }
            });
        } else {
            dispatch.run();
        }
        return id;
    }

    /**
     * 按创建时间倒序分页列出归属下的任务。
     *
     * @param owner 归属
     * @param page 从 1 开始的页码
     * @param size 每页条数
     * @return 当前页
     */
    public DownloadTaskPage page(DownloadOwner owner, int page, int size) {
        long total = store.count(owner);
        if (total == 0) {
            return new DownloadTaskPage(List.of(), 0);
        }
        Instant now = clock.instant();
        List<DownloadTask> items = store.page(owner, (long) (page - 1) * size, size).stream()
                .map(row -> view(row, now))
                .toList();
        return new DownloadTaskPage(items, total);
    }

    /**
     * 读取归属下的单个任务。
     *
     * @param owner 归属
     * @param id 任务 ID
     * @return 任务
     * @throws DownloadCenterException 不存在或不属于该归属
     */
    public DownloadTask get(DownloadOwner owner, long id) {
        return view(require(owner, id), clock.instant());
    }

    /**
     * 复核来源权限后签发下载链接，签名失败不回退原始路径。
     *
     * @param owner 归属
     * @param id 任务 ID
     * @return 链接
     * @throws DownloadCenterException 不可下载
     */
    public DownloadLink link(DownloadOwner owner, long id) {
        DownloadTaskRecord row = require(owner, id);
        Instant now = clock.instant();
        if (row.status().active()) {
            throw new DownloadCenterException(DownloadCenterError.NOT_READY);
        }
        if (row.status() != DownloadTaskStatus.SUCCEEDED || expired(row, now) || isBlank(row.objectPath())) {
            throw new DownloadCenterException(DownloadCenterError.NOT_FOUND);
        }
        DownloadSourceAccess access = accesses.get(row.source());
        if (access == null) {
            throw new DownloadCenterException(DownloadCenterError.ACCESS_DENIED);
        }
        access.check(owner);
        Duration ttl = properties.getLinkTtl();
        String url;
        try {
            url = oss.getObjectURL(row.objectPath(), Math.toIntExact(ttl.toSeconds()));
        } catch (RuntimeException exception) {
            log.warn("[Download] 签发链接失败 task={}", id, exception);
            throw new DownloadCenterException(DownloadCenterError.LINK_UNAVAILABLE);
        }
        if (isBlank(url) || url.equals(row.objectPath())) {
            throw new DownloadCenterException(DownloadCenterError.LINK_UNAVAILABLE);
        }
        return new DownloadLink(url, row.filename(), now.plus(ttl));
    }

    /**
     * 删除到期文件、结束超时任务并清除过旧记录；多节点重复执行幂等。
     */
    public void cleanup() {
        Instant now = clock.instant();
        for (DownloadTaskRecord row : store.listExpired(now, properties.getCleanupBatchSize())) {
            try {
                removeObject(row.objectPath());
                store.markExpired(row.id());
            } catch (RuntimeException exception) {
                log.warn("[Download] 清理到期文件失败 task={}", row.id(), exception);
            }
        }
        int stale = store.failStale(now.minus(properties.getRunTimeout()), DownloadFailures.STALE, now);
        int purged = store.purge(now.minus(properties.getRecordRetention()));
        if (stale > 0 || purged > 0) {
            log.info("[Download] 清理完成 stale={} purged={}", stale, purged);
        }
    }

    private void dispatch(long id, DownloadOwner owner, DownloadJob job) {
        try {
            executor.execute(() -> run(id, owner, job));
        } catch (RejectedExecutionException exception) {
            log.warn("[Download] 执行队列已满 task={}", id);
            store.markFailed(id, DownloadFailures.BUSY, clock.instant());
        }
    }

    private void run(long id, DownloadOwner owner, DownloadJob job) {
        if (!store.markRunning(id, clock.instant())) {
            return;
        }
        DownloadFile file;
        try {
            file = job.produce();
        } catch (DownloadJobException exception) {
            store.markFailed(id, exception.getMessage(), clock.instant());
            return;
        } catch (RuntimeException exception) {
            log.warn("[Download] 生成文件失败 task={}", id, exception);
            store.markFailed(id, DownloadFailures.GENERATE, clock.instant());
            return;
        }
        if (file == null || file.content() == null || isBlank(file.filename())) {
            store.markFailed(id, DownloadFailures.GENERATE, clock.instant());
            return;
        }
        String bucket = properties.getBucket();
        String objectName = objectName(owner, id, file.filename());
        try {
            oss.uploadFile(bucket, objectName, new ByteArrayInputStream(file.content()));
        } catch (RuntimeException exception) {
            log.warn("[Download] 上传文件失败 task={}", id, exception);
            store.markFailed(id, DownloadFailures.STORE, clock.instant());
            return;
        }
        boolean saved;
        try {
            saved = store.markSucceeded(id, file.filename(), file.contentType(), file.content().length,
                    bucket + PATH_SEPARATOR + objectName, clock.instant());
        } catch (RuntimeException exception) {
            log.warn("[Download] 记录结果失败 task={}", id, exception);
            saved = false;
        }
        if (!saved) {
            removeQuietly(bucket, objectName, id);
            store.markFailed(id, DownloadFailures.STORE, clock.instant());
        }
    }

    private DownloadTaskRecord require(DownloadOwner owner, long id) {
        DownloadTaskRecord row = store.find(owner, id);
        if (row == null) {
            throw new DownloadCenterException(DownloadCenterError.NOT_FOUND);
        }
        return row;
    }

    private void removeObject(String objectPath) {
        if (isBlank(objectPath)) {
            return;
        }
        int split = objectPath.indexOf(PATH_SEPARATOR);
        if (split <= 0 || split == objectPath.length() - 1) {
            return;
        }
        oss.removeFile(objectPath.substring(0, split), objectPath.substring(split + 1));
    }

    private void removeQuietly(String bucket, String objectName, long id) {
        try {
            oss.removeFile(bucket, objectName);
        } catch (RuntimeException exception) {
            log.warn("[Download] 删除孤立文件失败 task={}", id, exception);
        }
    }

    private static DownloadTask view(DownloadTaskRecord row, Instant now) {
        DownloadTaskStatus status = row.status() == DownloadTaskStatus.SUCCEEDED && expired(row, now)
                ? DownloadTaskStatus.EXPIRED : row.status();
        boolean finished = status == DownloadTaskStatus.SUCCEEDED || status == DownloadTaskStatus.EXPIRED;
        return new DownloadTask(String.valueOf(row.id()), row.source(), row.title(), status,
                finished ? row.filename() : null, finished ? row.fileSize() : null,
                status == DownloadTaskStatus.FAILED ? row.failureReason() : null,
                row.createdAt(), row.completedAt(), row.expiresAt());
    }

    private static boolean expired(DownloadTaskRecord row, Instant now) {
        return row.expiresAt() != null && !row.expiresAt().isAfter(now);
    }

    private static String objectName(DownloadOwner owner, long id, String filename) {
        String tenant = owner.tenantId() == null ? NO_TENANT : String.valueOf(owner.tenantId());
        return owner.domain().getValue().toLowerCase(Locale.ROOT) + PATH_SEPARATOR + tenant + PATH_SEPARATOR
                + owner.memberId() + PATH_SEPARATOR + id + PATH_SEPARATOR + safeName(filename);
    }

    private static String safeName(String filename) {
        return filename.replace('/', '_').replace('\\', '_');
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
