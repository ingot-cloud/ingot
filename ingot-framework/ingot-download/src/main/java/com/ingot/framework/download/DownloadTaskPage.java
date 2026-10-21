package com.ingot.framework.download;

import java.util.List;

import com.ingot.framework.commons.model.download.DownloadTask;

/**
 * <p>归属下的一页下载任务。</p>
 *
 * @author jy
 * @since 1.0.0
 * @param items 当前页
 * @param total 总数
 */
public record DownloadTaskPage(List<DownloadTask> items, long total) {

    /**
     * 固定当前页快照。
     */
    public DownloadTaskPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
