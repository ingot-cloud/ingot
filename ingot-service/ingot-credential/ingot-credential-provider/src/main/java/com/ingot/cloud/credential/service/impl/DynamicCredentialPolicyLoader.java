package com.ingot.cloud.credential.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.credential.model.domain.CredentialPolicyConfig;
import com.ingot.cloud.credential.service.PolicyConfigService;
import com.ingot.framework.security.credential.policy.*;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 动态策略加载器实现
 *
 * @author jymot
 * @since 2026-01-22
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicCredentialPolicyLoader implements CredentialPolicyLoader {
    private final PolicyConfigService policyConfigService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Cacheable(value = CACHE_NAME, key = "#tenantId ?: 'global'", unless = "#result.isEmpty()")
    public List<PasswordPolicy> loadPolicies(Long tenantId) {
        log.debug("加载策略 - 租户ID: {}", tenantId);
        List<CredentialPolicyConfig> configs = policyConfigService.getAllPolicyConfigs(tenantId);
        List<PasswordPolicy> policies = loadPolicies(configs);
        log.info("策略加载完成 - 租户ID: {}, 策略数量: {}", tenantId, policies.size());
        return policies;
    }

    protected List<PasswordPolicy> loadPolicies(List<CredentialPolicyConfig> configs) {
        List<PasswordPolicy> policies = new ArrayList<>();

        for (CredentialPolicyConfig config : configs) {
            try {
                PasswordPolicy policy = createPolicy(config);
                if (policy != null) {
                    policies.add(policy);
                    log.debug("DynamicCredentialPolicyLoader - 成功加载策略 - 类型: {}, 优先级: {}",
                            config.getPolicyType(), config.getPriority());
                }
            } catch (Exception e) {
                log.error("DynamicCredentialPolicyLoader - 策略加载失败 - 类型: {}, 错误: {}",
                        config.getPolicyType(), e.getMessage(), e);
            }
        }

        // 按优先级排序
        policies.sort(Comparator.comparingInt(PasswordPolicy::getPriority));
        return policies;
    }

    /**
     * 根据配置创建策略实例
     */
    private PasswordPolicy createPolicy(CredentialPolicyConfig config) {
        Map<String, Object> policyConfig = config.getPolicyConfig();

        return switch (config.getPolicyType()) {
            case STRENGTH -> PasswordPolicyUtil.createStrengthPolicy(policyConfig, config.getPriority());
            case HISTORY -> PasswordPolicyUtil.createHistoryPolicy(policyConfig, config.getPriority(), passwordEncoder);
            case EXPIRATION -> PasswordPolicyUtil.createExpirationPolicy(policyConfig, config.getPriority());
        };
    }

    @Override
    @CacheEvict(value = CACHE_NAME, key = "#tenantId ?: 'global'")
    public void reloadPolicies(Long tenantId) {
        log.info("重新加载策略 - 租户ID: {}", tenantId);
    }

    @Override
    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public void clearPolicyCache() {
        log.info("清空所有策略缓存");
    }
}