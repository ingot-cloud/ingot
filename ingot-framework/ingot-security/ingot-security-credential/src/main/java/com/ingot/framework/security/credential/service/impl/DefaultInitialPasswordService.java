package com.ingot.framework.security.credential.service.impl;

import java.time.LocalDateTime;

import cn.hutool.core.util.RandomUtil;
import com.ingot.framework.security.credential.config.CredentialSecurityProperties;
import com.ingot.framework.security.credential.config.CredentialSecurityProperties.InitialPasswordPolicy;
import com.ingot.framework.security.credential.service.InitialPasswordService;
import lombok.RequiredArgsConstructor;

/**
 * 初始密码服务默认实现，直接读取 {@link CredentialSecurityProperties} 当前策略值。
 *
 * <p>依赖 {@code local} 模式下 {@link CredentialSecurityProperties} 随 Nacos 刷新即时更新的特性，
 * 每次调用都读取最新配置，无需自身缓存。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@RequiredArgsConstructor
public class DefaultInitialPasswordService implements InitialPasswordService {

    private final CredentialSecurityProperties properties;

    @Override
    public String generate() {
        InitialPasswordPolicy policy = policy();
        if (policy.getGeneration() == InitialPasswordPolicy.Generation.FIXED) {
            return policy.getFixedPassword();
        }
        int length = Math.max(6, policy.getLength());
        // 保证包含大小写与数字，满足常见强度要求
        String base = RandomUtil.randomString("ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789", length - 2);
        String upper = RandomUtil.randomString("ABCDEFGHJKLMNPQRSTUVWXYZ", 1);
        String digit = RandomUtil.randomString("23456789", 1);
        return upper + base + digit;
    }

    @Override
    public boolean isExpired(LocalDateTime issuedAt) {
        int validHours = policy().getValidHours();
        if (validHours <= 0 || issuedAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(issuedAt.plusHours(validHours));
    }

    @Override
    public boolean isForceChangeOnFirstLogin() {
        return policy().isForceChangeOnFirstLogin();
    }

    private InitialPasswordPolicy policy() {
        return properties.getPolicy().getInitialPassword();
    }
}
