package com.ingot.framework.security.credential.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.cloud.security.api.rpc.RemoteCredentialService;
import com.ingot.framework.core.config.LocalCacheConfig;
import com.ingot.framework.security.credential.model.CredentialPolicyType;
import com.ingot.framework.security.credential.policy.PasswordPolicy;
import com.ingot.framework.security.credential.policy.PasswordPolicyUtil;
import com.ingot.framework.security.credential.service.ClearPasswordPolicyCacheService;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 远程凭证策略加载器
 * <p>采用双层缓存：内存缓存策略实例 + Spring Cache 缓存配置数据</p>
 *
 * @author jy
 * @since 2026/1/30
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteCredentialPolicyLoader implements CredentialPolicyLoader {
    private final RemoteCredentialService remoteCredentialService;
    private final PasswordEncoder passwordEncoder;

    @Cacheable(
            value = ClearPasswordPolicyCacheService.CACHE_NAME,
            key = "'list'",
            unless = "#result.isEmpty()",
            cacheManager = LocalCacheConfig.CACHE_MANAGER
    )
    @Override
    public List<PasswordPolicy> loadPolicies() {
        List<CredentialPolicyConfigVO> configs = remoteCredentialService.getPolicyConfigs()
                .ifErrorThrow().getData();
        return buildPolicies(configs);
    }

    /**
     * 根据配置构建策略实例
     */
    private List<PasswordPolicy> buildPolicies(List<CredentialPolicyConfigVO> configs) {
        List<PasswordPolicy> policies = new ArrayList<>();

        for (CredentialPolicyConfigVO config : configs) {
            try {
                PasswordPolicy policy = createPolicy(config);
                if (policy != null) {
                    policies.add(policy);
                    log.debug("成功加载策略 - 类型: {}, 优先级: {}",
                            config.getPolicyType(), config.getPriority());
                }
            } catch (Exception e) {
                log.error("策略加载失败 - 类型: {}, 错误: {}",
                        config.getPolicyType(), e.getMessage(), e);
            }
        }

        // 按优先级排序
        policies.sort(Comparator.comparingInt(PasswordPolicy::getPriority));
        return List.copyOf(policies);
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
}
