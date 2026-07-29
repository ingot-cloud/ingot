package com.ingot.framework.security.account.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 账号安全事件类型
 *
 * @author jymot
 * @since 2026-02-13
 */
@Getter
@RequiredArgsConstructor
public enum SecurityEventType {
    // ========== 认证事件（AUTH） ==========
    /**
     * 登录成功
     */
    LOGIN_SUCCESS("LOGIN_SUCCESS", "AUTH", "登录成功"),
    
    /**
     * 登录失败
     */
    LOGIN_FAILURE("LOGIN_FAILURE", "AUTH", "登录失败"),
    
    /**
     * 登出
     */
    LOGOUT("LOGOUT", "AUTH", "登出"),
    
    /**
     * Token刷新
     */
    TOKEN_REFRESH("TOKEN_REFRESH", "AUTH", "Token刷新"),
    
    // ========== 账号事件（ACCOUNT） ==========
    /**
     * 账号创建
     */
    ACCOUNT_CREATED("ACCOUNT_CREATED", "ACCOUNT", "账号创建"),
    
    /**
     * 账号启用
     */
    ACCOUNT_ENABLED("ACCOUNT_ENABLED", "ACCOUNT", "账号启用"),
    
    /**
     * 账号禁用
     */
    ACCOUNT_DISABLED("ACCOUNT_DISABLED", "ACCOUNT", "账号禁用"),
    
    /**
     * 账号锁定
     */
    ACCOUNT_LOCKED("ACCOUNT_LOCKED", "ACCOUNT", "账号锁定"),
    
    /**
     * 账号解锁
     */
    ACCOUNT_UNLOCKED("ACCOUNT_UNLOCKED", "ACCOUNT", "账号解锁"),
    
    /**
     * 账号删除
     */
    ACCOUNT_DELETED("ACCOUNT_DELETED", "ACCOUNT", "账号删除"),
    
    // ========== 凭证事件（CREDENTIAL） ==========
    /**
     * 密码修改
     */
    PASSWORD_CHANGED("PASSWORD_CHANGED", "CREDENTIAL", "密码修改"),
    
    /**
     * 密码重置
     */
    PASSWORD_RESET("PASSWORD_RESET", "CREDENTIAL", "密码重置"),
    
    /**
     * 密码过期
     */
    PASSWORD_EXPIRED("PASSWORD_EXPIRED", "CREDENTIAL", "密码过期"),
    
    /**
     * 强制修改密码
     */
    FORCE_CHANGE_PASSWORD("FORCE_CHANGE_PASSWORD", "CREDENTIAL", "强制修改密码");

    private final String code;
    private final String category;
    private final String description;

    public static SecurityEventType fromCode(String code) {
        for (SecurityEventType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
