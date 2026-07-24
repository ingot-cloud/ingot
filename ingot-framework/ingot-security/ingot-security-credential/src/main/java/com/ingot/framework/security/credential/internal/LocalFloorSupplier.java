package com.ingot.framework.security.credential.internal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.ingot.cloud.security.api.model.vo.CredentialPolicyConfigVO;
import com.ingot.framework.security.credential.config.CredentialSecurityProperties;
import com.ingot.framework.security.credential.config.CredentialSecurityProperties.ExpirationPolicy;
import com.ingot.framework.security.credential.config.CredentialSecurityProperties.HistoryPolicy;
import com.ingot.framework.security.credential.config.CredentialSecurityProperties.InitialPasswordPolicy;
import com.ingot.framework.security.credential.config.CredentialSecurityProperties.StrengthPolicy;
import com.ingot.framework.security.credential.model.CredentialPolicyType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Nacos 本地地板供给器：将 {@link CredentialSecurityProperties} 的本地策略配置映射为
 * {@code List<CredentialPolicyConfigVO>}，作为 {@code remote} 模式下远程不可用且无 LKG 时的最终兜底来源。
 *
 * <p>输出的 VO 结构与安全中心下发的一致，可直接经 {@code RemoteCredentialPolicyLoader} 编译，
 * 从而保证降级路径与正常路径共用同一套编译逻辑。为满足「永不 fail-open」，当本地配置把可校验策略
 * 全部关闭而导致映射为空时，兜底补一个最小强度基线。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class LocalFloorSupplier {

    private final CredentialSecurityProperties properties;

    /**
     * 生成本地地板策略 VO 列表（安全基线，非空）。
     */
    public List<CredentialPolicyConfigVO> get() {
        CredentialSecurityProperties.PolicyConfig policy = properties.getPolicy();
        List<CredentialPolicyConfigVO> list = new ArrayList<>(4);

        StrengthPolicy strength = policy.getStrength();
        if (strength != null && strength.isEnabled()) {
            list.add(strengthVO(strength));
        }
        ExpirationPolicy expiration = policy.getExpiration();
        if (expiration != null && expiration.isEnabled()) {
            list.add(expirationVO(expiration));
        }
        HistoryPolicy history = policy.getHistory();
        if (history != null && history.isEnabled()) {
            list.add(historyVO(history));
        }
        // 初始密码无启用开关，始终纳入地板，供 getInitialPasswordConfig 在降级时读取。
        InitialPasswordPolicy initialPassword = policy.getInitialPassword();
        if (initialPassword != null) {
            list.add(initialPasswordVO(initialPassword));
        }

        boolean hasCheckablePolicy = list.stream()
                .anyMatch(vo -> !CredentialPolicyType.INITIAL_PASSWORD.getValue().equals(vo.getPolicyType()));
        if (!hasCheckablePolicy) {
            log.warn("[Credential] 本地地板缺少可校验策略，补最小强度基线以避免 fail-open");
            list.add(strengthVO(new StrengthPolicy()));
        }
        return List.copyOf(list);
    }

    private CredentialPolicyConfigVO strengthVO(StrengthPolicy config) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("minLength", config.getMinLength());
        map.put("maxLength", config.getMaxLength());
        map.put("requireUppercase", config.isRequireUppercase());
        map.put("requireLowercase", config.isRequireLowercase());
        map.put("requireDigit", config.isRequireDigit());
        map.put("requireSpecialChar", config.isRequireSpecialChar());
        map.put("specialChars", config.getSpecialChars());
        map.put("forbiddenPatterns", config.getForbiddenPatterns());
        map.put("forbidUserAttributes", config.isForbidUserAttributes());
        return vo(CredentialPolicyType.STRENGTH, map, 10);
    }

    private CredentialPolicyConfigVO expirationVO(ExpirationPolicy config) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enabled", config.isEnabled());
        map.put("maxDays", config.getMaxDays());
        map.put("warningDaysBefore", config.getWarningDaysBefore());
        map.put("graceLoginCount", config.getGraceLoginCount());
        return vo(CredentialPolicyType.EXPIRATION, map, 20);
    }

    private CredentialPolicyConfigVO historyVO(HistoryPolicy config) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enabled", config.isEnabled());
        map.put("checkCount", config.getCheckCount());
        return vo(CredentialPolicyType.HISTORY, map, 30);
    }

    private CredentialPolicyConfigVO initialPasswordVO(InitialPasswordPolicy config) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("generation", config.getGeneration() == null ? null : config.getGeneration().name());
        map.put("length", config.getLength());
        map.put("fixedPassword", config.getFixedPassword());
        map.put("validHours", config.getValidHours());
        map.put("oneTime", config.isOneTime());
        map.put("forceChangeOnFirstLogin", config.isForceChangeOnFirstLogin());
        return vo(CredentialPolicyType.INITIAL_PASSWORD, map, 40);
    }

    private CredentialPolicyConfigVO vo(CredentialPolicyType type, Map<String, Object> config, int priority) {
        CredentialPolicyConfigVO vo = new CredentialPolicyConfigVO();
        vo.setPolicyType(type.getValue());
        vo.setPolicyConfig(config);
        vo.setPriority(priority);
        vo.setEnabled(Boolean.TRUE);
        return vo;
    }
}
