package com.ingot.framework.security.credential.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.framework.security.credential.internal.LocalCompiledPolicyCache;
import com.ingot.framework.security.credential.model.CredentialPolicyType;
import com.ingot.framework.security.credential.policy.PasswordPolicy;
import com.ingot.framework.security.credential.policy.PasswordPolicyUtil;
import com.ingot.framework.security.credential.service.CredentialPolicyConfigService;
import com.ingot.framework.security.credential.service.CredentialPolicyLoader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 远程凭证策略加载器。
 * <p>
 * 数据获取走 {@link CredentialPolicyConfigService}（L1 Caffeine -> L2 Redis -> Feign），
 * 编译后的策略实例只在本进程通过 {@link LocalCompiledPolicyCache} 缓存（不可序列化）。
 * 跨节点变更通过 {@code CredentialInvalidationEvent} 同步清空。
 * </p>
 *
 * @author jy
 * @since 2026/1/30
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteCredentialPolicyLoader implements CredentialPolicyLoader {

    private final CredentialPolicyConfigService policyConfigService;
    private final LocalCompiledPolicyCache compiledPolicyCache;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<PasswordPolicy> loadPolicies() {
        return compiledPolicyCache.get(() -> buildPolicies(policyConfigService.getAll()));
    }

    /**
     * 根据配置构建策略实例
     */
    private List<PasswordPolicy> buildPolicies(List<CredentialPolicyConfigVO> configs) {
        if (configs == null || configs.isEmpty()) {
            return List.of();
        }
        List<PasswordPolicy> policies = new ArrayList<>(configs.size());
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
