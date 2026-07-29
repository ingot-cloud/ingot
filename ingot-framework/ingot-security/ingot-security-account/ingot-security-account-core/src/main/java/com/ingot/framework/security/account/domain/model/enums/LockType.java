package com.ingot.framework.security.account.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 锁定类型
 *
 * @author jymot
 * @since 2026-02-13
 */
@Getter
@RequiredArgsConstructor
public enum LockType {
    /**
     * 手动锁定（管理员操作）
     */
    MANUAL("MANUAL", "手动锁定"),
    
    /**
     * 自动锁定（策略触发）
     */
    AUTO("AUTO", "自动锁定");

    private final String code;
    private final String description;
}
