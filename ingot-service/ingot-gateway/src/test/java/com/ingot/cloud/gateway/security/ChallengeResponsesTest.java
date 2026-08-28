package com.ingot.cloud.gateway.security;

import java.util.Map;

import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.vc.common.VCConstants;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * {@link ChallengeResponses} 412 payload 字段。
 *
 * @author jy
 * @since 1.0.0
 */
class ChallengeResponsesTest {

    @Test
    void buildPayload_omitsTtlAndRemaining_exposesDynamicParams() {
        ChallengePolicy policy = ChallengePolicy.builder()
                .challengeType("SLIDER")
                .scope("login")
                .passTokenTtlSec(300)
                .passTokenRemaining(3)
                .build();

        Map<String, Object> data = ChallengeResponses.buildPayload(policy);

        assertEquals("image", data.get(ChallengeResponses.FIELD_VC_TYPE));
        assertEquals("/vc/image/check", data.get(ChallengeResponses.FIELD_CHECK_PATH));
        assertEquals("login", data.get(ChallengeResponses.FIELD_SCOPE));
        assertEquals(VCConstants.HEADER_SCOPE, data.get(ChallengeResponses.FIELD_SCOPE_PARAM));
        assertEquals(VCConstants.HEADER_PASS_TOKEN, data.get(ChallengeResponses.FIELD_PASS_TOKEN_PARAM));
        assertEquals("In-Vc-Scope", VCConstants.HEADER_SCOPE);
        assertEquals("In-Vc-Pass-Token", VCConstants.HEADER_PASS_TOKEN);
        assertFalse(data.containsKey("ttlSec"));
        assertFalse(data.containsKey("remaining"));
        assertEquals(5, data.size());
    }
}
