package com.ingot.framework.security.recording.model;

/**
 * <p>审计投递失败策略预留枚举。</p>
 *
 * @author jy
 * @since 1.0.0
 */
public enum AuditFailurePolicy {

    /** 默认：fail-open 并告警。 */
    FAIL_OPEN_ALERT,

    /** 后续高风险操作可显式配置 fail-closed。 */
    FAIL_CLOSED
}
