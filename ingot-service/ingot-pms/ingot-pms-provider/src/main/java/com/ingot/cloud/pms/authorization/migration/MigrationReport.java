package com.ingot.cloud.pms.authorization.migration;

import java.util.List;

/**
 * <p>迁移分析结果。{@link #canApply()} 为假时禁止执行数据改写。</p>
 *
 * @param blocking 阻断清单
 * @param changes  批准的映射动作
 * @author jy
 * @since 1.0.0
 */
public record MigrationReport(
        List<MigrationBlockingIssue> blocking,
        List<MigrationMappedChange> changes
) {
    /**
     * 是否允许 apply。存在阻断时必须先人工处理。
     *
     * @return 阻断清单为空时返回 {@code true}
     */
    public boolean canApply() {
        return blocking == null || blocking.isEmpty();
    }
}
