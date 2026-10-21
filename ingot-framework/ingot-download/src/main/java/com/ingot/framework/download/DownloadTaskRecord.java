package com.ingot.framework.download;

import java.time.Instant;

import com.ingot.framework.commons.model.download.DownloadTaskStatus;

/**
 * <p>存储层的下载任务完整记录，含对象路径，只在框架与宿主存储之间传递。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param id 任务 ID
 * @param owner 归属
 * @param source 来源编码
 * @param title 展示名称
 * @param status 状态
 * @param filename 文件名
 * @param contentType MIME 类型
 * @param fileSize 字节数
 * @param objectPath {@code bucket/objectName}
 * @param failureReason 可展示失败原因
 * @param createdAt 登记时间
 * @param startedAt 开始生成时间
 * @param completedAt 完成时间
 * @param expiresAt 文件过期时间
 */
public record DownloadTaskRecord(long id, DownloadOwner owner, String source, String title,
        DownloadTaskStatus status, String filename, String contentType, Long fileSize, String objectPath,
        String failureReason, Instant createdAt, Instant startedAt, Instant completedAt, Instant expiresAt) {
}
