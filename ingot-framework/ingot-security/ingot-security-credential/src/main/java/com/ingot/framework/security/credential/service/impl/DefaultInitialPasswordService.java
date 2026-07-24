package com.ingot.framework.security.credential.service.impl;

import java.time.LocalDateTime;

import cn.hutool.core.util.RandomUtil;
import com.ingot.framework.security.credential.config.CredentialSecurityProperties.InitialPasswordPolicy;
import com.ingot.framework.security.credential.model.InitialPasswordConfig;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import com.ingot.framework.security.credential.service.InitialPasswordService;
import lombok.RequiredArgsConstructor;

/**
 * 初始密码服务默认实现，经 {@link CredentialPolicyLoader} 获取当前生效的初始密码配置。
 *
 * <p>与 strength / history / expiration 共享同一「安全中心优先、Nacos 兜底」的来源语义：
 * {@code remote} 模式取安全中心下发并享受降级阶梯，{@code local} 模式取 Nacos 属性。
 * 每次调用都取最新生效配置，无需自身缓存。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class DefaultInitialPasswordService implements InitialPasswordService {

    private final CredentialPolicyLoader policyLoader;

    @Override
    public String generate() {
        InitialPasswordConfig config = policyLoader.getInitialPasswordConfig();
        if (config.generation() == InitialPasswordPolicy.Generation.FIXED) {
            return config.fixedPassword();
        }
        int length = Math.max(6, config.length());
        // 保证包含大小写与数字，满足常见强度要求
        String base = RandomUtil.randomString("ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789", length - 2);
        String upper = RandomUtil.randomString("ABCDEFGHJKLMNPQRSTUVWXYZ", 1);
        String digit = RandomUtil.randomString("23456789", 1);
        return upper + base + digit;
    }

    @Override
    public boolean isExpired(LocalDateTime issuedAt) {
        int validHours = policyLoader.getInitialPasswordConfig().validHours();
        if (validHours <= 0 || issuedAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(issuedAt.plusHours(validHours));
    }

    @Override
    public boolean isForceChangeOnFirstLogin() {
        return policyLoader.getInitialPasswordConfig().forceChangeOnFirstLogin();
    }
}
