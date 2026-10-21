package com.ingot.framework.download;

import java.time.Instant;
import java.util.List;

/**
 * <p>下载任务持久化 SPI，由宿主服务实现。状态更新必须是带前置状态的条件更新，多节点并发时只有一方成功。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public interface DownloadTaskStore {

    /**
     * 分配任务 ID。
     *
     * @return 正数 ID
     */
    long nextId();

    /**
     * 插入 PENDING 任务。
     *
     * @param task 待插入记录
     */
    void insert(DownloadTaskRecord task);

    /**
     * PENDING 改为 RUNNING。
     *
     * @param id 任务 ID
     * @param startedAt 开始时间
     * @return 恰好更新一行时为 true
     */
    boolean markRunning(long id, Instant startedAt);

    /**
     * RUNNING 改为 SUCCEEDED 并记录文件。
     *
     * @param id 任务 ID
     * @param filename 文件名
     * @param contentType MIME 类型
     * @param fileSize 字节数
     * @param objectPath {@code bucket/objectName}
     * @param completedAt 完成时间
     * @return 恰好更新一行时为 true
     */
    boolean markSucceeded(long id, String filename, String contentType, long fileSize, String objectPath,
            Instant completedAt);

    /**
     * PENDING 或 RUNNING 改为 FAILED。
     *
     * @param id 任务 ID
     * @param reason 可展示原因
     * @param completedAt 完成时间
     * @return 恰好更新一行时为 true
     */
    boolean markFailed(long id, String reason, Instant completedAt);

    /**
     * 按归属读取任务。
     *
     * @param owner 归属
     * @param id 任务 ID
     * @return 记录；不存在或不属于该归属时为空
     */
    DownloadTaskRecord find(DownloadOwner owner, long id);

    /**
     * 统计归属下的任务数。
     *
     * @param owner 归属
     * @return 总数
     */
    long count(DownloadOwner owner);

    /**
     * 按创建时间倒序分页读取归属下的任务。
     *
     * @param owner 归属
     * @param offset 偏移
     * @param limit 条数
     * @return 当前页
     */
    List<DownloadTaskRecord> page(DownloadOwner owner, long offset, int limit);

    /**
     * 读取文件已到期的 SUCCEEDED 任务。
     *
     * @param now 当前时间
     * @param limit 批量上限
     * @return 待清理任务
     */
    List<DownloadTaskRecord> listExpired(Instant now, int limit);

    /**
     * SUCCEEDED 改为 EXPIRED 并清除对象路径。
     *
     * @param id 任务 ID
     * @return 恰好更新一行时为 true
     */
    boolean markExpired(long id);

    /**
     * 把登记早于给定时间仍未完成的任务标为失败。
     *
     * @param createdBefore 登记时间上界，不含
     * @param reason 可展示原因
     * @param completedAt 完成时间
     * @return 更新行数
     */
    int failStale(Instant createdBefore, String reason, Instant completedAt);

    /**
     * 物理删除登记早于给定时间的 FAILED、EXPIRED 任务。
     *
     * @param createdBefore 登记时间上界，不含
     * @return 删除行数
     */
    int purge(Instant createdBefore);
}
