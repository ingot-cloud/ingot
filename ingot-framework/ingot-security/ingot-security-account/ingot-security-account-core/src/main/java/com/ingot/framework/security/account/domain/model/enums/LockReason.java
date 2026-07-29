package com.ingot.framework.security.account.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 账号锁定原因
 *
 * @author jymot
 * @since 2026-02-13
 */
@Getter
@RequiredArgsConstructor
public enum LockReason {
    /**
     * 管理员手动锁定
     */
    MANUAL_LOCK("MANUAL", "管理员手动锁定"),
    
    /**
     * 登录失败次数超限
     */
    LOGIN_FAIL_EXCEED("LOGIN_FAIL", "登录失败次数超限"),
    
    /**
     * 检测到可疑活动
     */
    SUSPICIOUS_ACTIVITY("SUSPICIOUS", "检测到可疑活动"),
    
    /**
     * 密码过期未更新
     */
    PASSWORD_EXPIRED("PWD_EXPIRED", "密码过期未更新"),
    
    /**
     * 违反安全策略
     */
    SECURITY_POLICY("SECURITY", "违反安全策略"),
    
    /**
     * 用户举报
     */
    USER_REPORT("USER_REPORT", "用户举报"),
    
    /**
     * 系统自动锁定
     */
    SYSTEM_AUTO("SYSTEM", "系统自动锁定"),
    
    /**
     * 遗留数据（从旧status=9迁移）
     */
    LEGACY_DISABLED("LEGACY", "遗留禁用状态");

    private final String code;
    private final String description;

    public static LockReason fromCode(String code) {
        for (LockReason reason : values()) {
            if (reason.getCode().equals(code)) {
                return reason;
            }
        }
        return null;
    }
}
