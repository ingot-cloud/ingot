package com.ingot.framework.security.credential.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.credential.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.cloud.credential.api.rpc.RemoteCredentialService;
import com.ingot.framework.security.credential.model.CredentialPolicyType;
import com.ingot.framework.security.credential.policy.*;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 远程凭证策略加载器
 *
 * @author jy
 * @since 2026/1/30
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteCredentialPolicyLoader implements CredentialPolicyLoader {
    private final RemoteCredentialService remoteCredentialService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Cacheable(value = CACHE_NAME, key = "#tenantId ?: 'global'", unless = "#result.isEmpty()")
    public List<PasswordPolicy> loadPolicies(Long tenantId) {
        log.debug("加载策略 - 租户ID: {}", tenantId);
        List<CredentialPolicyConfigVO> configs = remoteCredentialService.getPolicyConfigs(tenantId)
                .ifErrorThrow().getData();
        List<PasswordPolicy> policies = loadPolicies(configs);
        log.info("策略加载完成 - 租户ID: {}, 策略数量: {}", tenantId, policies.size());
        return policies;
    }

    private List<PasswordPolicy> loadPolicies(List<CredentialPolicyConfigVO> configs) {
        List<PasswordPolicy> policies = new ArrayList<>();

        for (CredentialPolicyConfigVO config : configs) {
            try {
                PasswordPolicy policy = createPolicy(config);
                if (policy != null) {
                    policies.add(policy);
                    log.debug("RemoteCredentialPolicyLoader - 成功加载策略 - 类型: {}, 优先级: {}",
                            config.getPolicyType(), config.getPriority());
                }
            } catch (Exception e) {
                log.error("RemoteCredentialPolicyLoader - 策略加载失败 - 类型: {}, 错误: {}",
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
    private PasswordPolicy createPolicy(CredentialPolicyConfigVO config) {
        Map<String, Object> policyConfig = config.getPolicyConfig();
        CredentialPolicyType policyType = CredentialPolicyType.getEnum(config.getPolicyType());
        return switch (policyType) {
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
