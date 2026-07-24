package com.ingot.framework.security.credential.service;

import java.util.List;

import com.ingot.framework.security.credential.model.InitialPasswordConfig;
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

    /**
     * 获取当前生效的初始密码配置。
     * <p>{@code remote} 模式优先取安全中心下发（享受同一降级阶梯：remote → LKG → Nacos 地板），
     * 安全中心未下发或 {@code local} 模式时取本地 Nacos 属性；均缺失时返回缺省。</p>
     *
     * @return 生效的初始密码配置，非 {@code null}
     */
    InitialPasswordConfig getInitialPasswordConfig();
}
