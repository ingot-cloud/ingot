package com.ingot.framework.security.account.domain.model.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 账号安全事件来源
 *
 * @author jymot
 * @since 2026-02-13
 */
@Getter
@RequiredArgsConstructor
public enum EventSource {

    /**
     * 认证服务（登录/登出/Token 相关）
     */
    AUTH("AUTH"),

    /**
     * PMS 管理服务（管理员手动操作）
     */
    PMS("PMS"),

    /**
     * 会员服务
     */
    MEMBER("MEMBER"),

    /**
     * 系统自动（定时任务、策略触发等）
     */
    SYSTEM("SYSTEM");

    private final String value;
}
