package com.ingot.cloud.gateway.security;

import com.ingot.framework.gateway.rule.client.challenge.internal.ChallengeTypes;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.vc.common.VCConstants;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 412 挑战响应体字段组装。
 *
 * @author jy
 * @since 2026/5/28
 */
public final class ChallengeResponses {

    private ChallengeResponses() {
    }

    public static Map<String, Object> buildPayload(ChallengePolicy policy) {
        Map<String, Object> data = new LinkedHashMap<>();
        String vcType = ChallengeTypes.toVcType(policy.getChallengeType());
        data.put("vcType", vcType);
        data.put("scope", policy.getScope());
        data.put("scopeParam", VCConstants.QUERY_PARAMS_SCOPE);
        data.put("passTokenParam", VCConstants.QUERY_PARAMS_PASS_TOKEN);
        data.put("checkPath", VCConstants.PATH_PREFIX + "/" + vcType + "/check");
        data.put("ttlSec", policy.getPassTokenTtlSec());
        data.put("remaining", policy.getPassTokenRemaining());
        return data;
    }
}
