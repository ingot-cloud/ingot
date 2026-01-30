package com.ingot.framework.security.credential.service;

import java.util.List;

import com.ingot.framework.security.credential.policy.PasswordPolicy;

/**
 * 策略加载器
 *
 * @author jymot
 * @since 2026-01-22
 */
public interface CredentialPolicyLoader {
    /**
     * 缓存 KEY
     */
    String CACHE_NAME = "credential:policies";

    /**
     * 加载租户的策略列表
     *
     * @param tenantId 租户ID（null表示全局）
     * @return 策略列表（已按优先级排序）
     */
    List<PasswordPolicy> loadPolicies(Long tenantId);

    /**
     * 重新加载策略
     *
     * @param tenantId 租户ID
     */
    void reloadPolicies(Long tenantId);

    /**
     * 清空所有策略缓存
     */
    void clearPolicyCache();
}
