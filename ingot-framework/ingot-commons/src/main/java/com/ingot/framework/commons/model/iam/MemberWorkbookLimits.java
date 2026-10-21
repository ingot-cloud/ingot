package com.ingot.framework.commons.model.iam;

import java.time.Duration;

/**
 * <p>集中成员表格任务的行数、历史条数与保留期。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public final class MemberWorkbookLimits {
    /**
     * 单份文件允许的最大数据行。
     */
    public static final int MAX_DATA_ROWS = 1000;
    /**
     * 导入历史返回的最近条数。
     */
    public static final int HISTORY_SIZE = 10;
    /**
     * 任务与产物保留期。
     */
    public static final Duration RETENTION = Duration.ofHours(24);

    private MemberWorkbookLimits() {
    }
}
