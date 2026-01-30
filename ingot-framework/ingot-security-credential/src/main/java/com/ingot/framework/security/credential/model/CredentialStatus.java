package com.ingot.framework.security.credential.model;

/**
 * 凭证状态枚举
 *
 * @author jymot
 * @since 2026-01-21
 */
public enum CredentialStatus {

    /**
     * 正常
     */
    ACTIVE("正常"),

    /**
     * 已过期
     */
    EXPIRED("已过期"),

    /**
     * 需强制修改
     */
    FORCE_CHANGE("需强制修改"),

    /**
     * 已锁定
     */
    LOCKED("已锁定"),

    /**
     * 已禁用
     */
    DISABLED("已禁用");

    private final String description;

    CredentialStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
