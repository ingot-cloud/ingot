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
     * 加载租户的策略列表
     *
     * @return 策略列表（已按优先级排序）
     */
    List<PasswordPolicy> loadPolicies();
}
