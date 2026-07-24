package com.ingot.framework.security.credential.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.framework.security.credential.config.CredentialSecurityProperties.InitialPasswordPolicy;
import com.ingot.framework.security.credential.model.CredentialPolicyType;
import com.ingot.framework.security.credential.model.InitialPasswordConfig;
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
 * 数据获取走 {@link CredentialPolicyConfigService}（L1 Caffeine -> L2 Redis -> Resilient
 * 弹性阶梯 remote → LKG → Nacos 地板）。每次调用即时从该链路取 VO 并编译策略实例，
 * 使校验策略与 {@link #getInitialPasswordConfig()} 共用同一新鲜度与降级节奏；VO 层缓存
 * 的 TTL 与跨节点失效由链路自身负责，本加载器不再持有额外的进程内编译缓存。
 * </p>
 *
 * @author jy
 * @since 2026/1/30
 */
@Slf4j
@RequiredArgsConstructor
public class RemoteCredentialPolicyLoader implements CredentialPolicyLoader {

    private final CredentialPolicyConfigService policyConfigService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<PasswordPolicy> loadPolicies() {
        return buildPolicies(policyConfigService.getAll());
    }

    @Override
    public InitialPasswordConfig getInitialPasswordConfig() {
        // 走同一份 getAll()（已含降级阶梯 remote → LKG → Nacos 地板），挑出初始密码行
        List<CredentialPolicyConfigVO> configs = policyConfigService.getAll();
        if (configs == null || configs.isEmpty()) {
            return InitialPasswordConfig.defaults();
        }
        for (CredentialPolicyConfigVO config : configs) {
            if (CredentialPolicyType.INITIAL_PASSWORD.getValue().equals(config.getPolicyType())) {
                return mapInitialPassword(config.getPolicyConfig());
            }
        }
        return InitialPasswordConfig.defaults();
    }

    private InitialPasswordConfig mapInitialPassword(Map<String, Object> config) {
        if (config == null || config.isEmpty()) {
            return InitialPasswordConfig.defaults();
        }
        InitialPasswordConfig defaults = InitialPasswordConfig.defaults();
        InitialPasswordPolicy.Generation generation = defaults.generation();
        Object genValue = config.get("generation");
        if (genValue != null) {
            try {
                generation = InitialPasswordPolicy.Generation.valueOf(StrUtil.toString(genValue).trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("[Credential] 初始密码 generation 非法，使用缺省 - value={}", genValue);
            }
        }
        int length = config.containsKey("length")
                ? NumberUtil.parseInt(StrUtil.toString(config.get("length")), defaults.length()) : defaults.length();
        String fixedPassword = config.containsKey("fixedPassword")
                ? StrUtil.toString(config.get("fixedPassword")) : defaults.fixedPassword();
        int validHours = config.containsKey("validHours")
                ? NumberUtil.parseInt(StrUtil.toString(config.get("validHours")), defaults.validHours()) : defaults.validHours();
        boolean oneTime = config.containsKey("oneTime")
                ? BooleanUtil.toBoolean(StrUtil.toString(config.get("oneTime"))) : defaults.oneTime();
        boolean forceChange = config.containsKey("forceChangeOnFirstLogin")
                ? BooleanUtil.toBoolean(StrUtil.toString(config.get("forceChangeOnFirstLogin"))) : defaults.forceChangeOnFirstLogin();
        return new InitialPasswordConfig(generation, length, fixedPassword, validHours, oneTime, forceChange);
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
        if (policyType == null) {
            return null;
        }
        return switch (policyType) {
            case STRENGTH -> PasswordPolicyUtil.createStrengthPolicy(policyConfig, config.getPriority());
            case HISTORY -> PasswordPolicyUtil.createHistoryPolicy(policyConfig, config.getPriority(), passwordEncoder);
            case EXPIRATION -> PasswordPolicyUtil.createExpirationPolicy(policyConfig, config.getPriority());
            // 初始密码不是校验类策略，不参与 PasswordValidator 的策略列表；由 getInitialPasswordConfig 单独读取。
            case INITIAL_PASSWORD -> null;
        };
    }
}
