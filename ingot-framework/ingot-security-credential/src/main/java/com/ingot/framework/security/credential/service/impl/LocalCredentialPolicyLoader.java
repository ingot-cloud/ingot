package com.ingot.framework.security.credential.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.ingot.framework.security.credential.config.CredentialSecurityProperties;
import com.ingot.framework.security.credential.policy.PasswordExpirationPolicy;
import com.ingot.framework.security.credential.policy.PasswordHistoryPolicy;
import com.ingot.framework.security.credential.policy.PasswordPolicy;
import com.ingot.framework.security.credential.policy.PasswordStrengthPolicy;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 本地凭证策略加载器，从配置文件中加载策略
 *
 * @author jy
 * @since 2026/1/30
 */
@Slf4j
@RequiredArgsConstructor
public class LocalCredentialPolicyLoader implements CredentialPolicyLoader {
    private final CredentialSecurityProperties properties;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Cacheable(value = CACHE_NAME, key = "'list'", unless = "#result.isEmpty()")
    public List<PasswordPolicy> loadPolicies() {
        List<PasswordPolicy> policies = new ArrayList<>();

        // 密码强度策略
        if (properties.getPolicy().getStrength() != null) {
            PasswordStrengthPolicy strengthPolicy = createStrengthPolicyFromLocal();
            if (strengthPolicy != null) {
                policies.add(strengthPolicy);
                log.debug("加载本地强度策略");
            }
        }

        // 密码历史策略
        if (properties.getPolicy().getHistory() != null) {
            PasswordHistoryPolicy historyPolicy = createHistoryPolicyFromLocal();
            if (historyPolicy != null) {
                policies.add(historyPolicy);
                log.debug("加载本地历史策略");
            }
        }

        // 密码过期策略
        if (properties.getPolicy().getExpiration() != null) {
            PasswordExpirationPolicy expirationPolicy = createExpirationPolicyFromLocal();
            if (expirationPolicy != null) {
                policies.add(expirationPolicy);
                log.debug("加载本地过期策略");
            }
        }

        policies.sort(Comparator.comparingInt(PasswordPolicy::getPriority));
        log.info("本地兜底策略加载完成，策略数量: {}", policies.size());
        return policies;
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearPolicyCache() {

    }

    /**
     * 从本地配置创建强度策略
     */
    private PasswordStrengthPolicy createStrengthPolicyFromLocal() {
        CredentialSecurityProperties.StrengthPolicy config = properties.getPolicy().getStrength();
        if (!config.isEnabled()) {
            return null;
        }

        PasswordStrengthPolicy policy = new PasswordStrengthPolicy() {
            @Override
            public int getPriority() {
                return 10;
            }
        };

        policy.setMinLength(config.getMinLength());
        policy.setMaxLength(config.getMaxLength());
        policy.setRequireUppercase(config.isRequireUppercase());
        policy.setRequireLowercase(config.isRequireLowercase());
        policy.setRequireDigit(config.isRequireDigit());
        policy.setRequireSpecialChar(config.isRequireSpecialChar());
        policy.setSpecialChars(config.getSpecialChars());
        policy.setForbiddenPatterns(config.getForbiddenPatterns());
        policy.setForbidUserAttributes(config.isForbidUserAttributes());

        return policy;
    }

    /**
     * 从本地配置创建历史策略
     */
    private PasswordHistoryPolicy createHistoryPolicyFromLocal() {
        CredentialSecurityProperties.HistoryPolicy config = properties.getPolicy().getHistory();
        if (!config.isEnabled()) {
            return null;
        }

        PasswordHistoryPolicy policy = new PasswordHistoryPolicy(passwordEncoder) {
            @Override
            public int getPriority() {
                return 30;
            }
        };

        policy.setEnabled(true);
        policy.setCheckCount(config.getCheckCount());

        return policy;
    }

    /**
     * 从本地配置创建过期策略
     */
    private PasswordExpirationPolicy createExpirationPolicyFromLocal() {
        CredentialSecurityProperties.ExpirationPolicy config = properties.getPolicy().getExpiration();
        if (!config.isEnabled()) {
            return null;
        }

        PasswordExpirationPolicy policy = new PasswordExpirationPolicy();
        policy.setEnabled(config.isEnabled());
        policy.setMaxDays(config.getMaxDays());
        policy.setWarningDaysBefore(config.getWarningDaysBefore());
        policy.setGraceLoginCount(config.getGraceLoginCount());
        policy.setForceChangeAfterReset(config.isForceChangeAfterReset());

        return policy;
    }
}
