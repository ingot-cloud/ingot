package com.ingot.framework.security.access.model;

import com.ingot.cloud.security.api.model.enums.LoginFailureDimension;

/**
 * 登录失败保护策略（领域模型）。
 *
 * @author jy
 * @since 1.0.0
 */
public record LoginFailurePolicy(
        LoginFailureDimension dimension,
        boolean enabled,
        int maxAttempts,
        int windowMinutes,
        int blockTtlSec,
        String blockKeyType
) {
}
