package com.ingot.framework.gateway.rule.client.challenge;

import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeSnapshot;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger;
import org.springframework.http.HttpMethod;

/**
 * 挑战策略查询 SPI。
 *
 * @author jy
 * @since 2026/5/26
 */
public interface ChallengePolicyService {

    /**
     * 按请求路径 + 触发类型查找匹配的策略；找不到返回 null。
     */
    ChallengePolicy match(String requestPath, HttpMethod method, ChallengeTrigger trigger);

    ChallengeSnapshot getSnapshot();

    void evictAll();

    /**
     * 按 PassToken scope 查找启用中的策略；未找到返回 null。
     */
    default ChallengePolicy findByScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return null;
        }
        ChallengeSnapshot snapshot = getSnapshot();
        if (snapshot == null || snapshot.getPolicies() == null) {
            return null;
        }
        return snapshot.getPolicies().stream()
                .filter(p -> p.isEnabled() && scope.equals(p.getScope()))
                .findFirst()
                .orElse(null);
    }
}
