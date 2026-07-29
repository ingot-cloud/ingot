package com.ingot.cloud.security.api.support;

import com.ingot.cloud.security.api.config.SecurityEventProperties;

import java.time.LocalDateTime;

/**
 * 安全事件保留期清理辅助逻辑。
 *
 * @author jy
 * @since 1.0.0
 */
public final class SecurityEventRetentionSupport {

    private SecurityEventRetentionSupport() {
    }

    public static LocalDateTime resolveCutoff(SecurityEventProperties.Retention retention) {
        if (retention == null || retention.getDays() <= 0) {
            return null;
        }
        return LocalDateTime.now().minusDays(retention.getDays());
    }

    public static int resolveBatchSize(SecurityEventProperties.Retention retention) {
        if (retention == null || retention.getBatchSize() <= 0) {
            return 500;
        }
        return retention.getBatchSize();
    }

    public static int resolveMaxRounds(SecurityEventProperties.Retention retention) {
        if (retention == null || retention.getMaxRounds() <= 0) {
            return 100;
        }
        return retention.getMaxRounds();
    }
}
