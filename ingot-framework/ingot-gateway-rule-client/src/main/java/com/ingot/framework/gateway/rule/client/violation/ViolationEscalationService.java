package com.ingot.framework.gateway.rule.client.violation;

import com.ingot.framework.gateway.rule.client.violation.model.ViolationEscalationConfig;

/**
 * 限流违规升级配置 SPI。
 *
 * <p>local 模式：{@link com.ingot.framework.gateway.rule.client.violation.internal.LocalViolationEscalationService}</p>
 * <p>remote 模式：{@link com.ingot.framework.gateway.rule.client.violation.internal.RemoteViolationEscalationService}</p>
 *
 * <p>装配条件：{@code ingot.security.violation-escalation.enabled=true}。</p>
 *
 * @author jy
 * @since 2026/6/5
 */
public interface ViolationEscalationService {

    /**
     * 获取当前生效的违规升级配置（L1 缓存命中时不重复编译/拉取）。
     */
    ViolationEscalationConfig getConfig();

    /**
     * 清空 L1 缓存；由 {@link com.ingot.framework.gateway.rule.client.internal.SecurityPolicyCacheCoordinator}
     * 在 {@code VIOLATION_ESCALATION} 失效事件时调用。
     */
    void evictAll();
}
