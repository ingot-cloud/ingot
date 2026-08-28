package com.ingot.framework.gateway.rule.client.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import com.ingot.cloud.security.api.model.enums.ChallengeCaptchaType;
import org.junit.jupiter.api.Test;

/**
 * {@link ChallengeTypes} 与 {@link ChallengeCaptchaType} 映射。
 *
 * @author jy
 * @since 1.0.0
 */
class ChallengeTypesTest {

    @Test
    void toVcType_mapsImageAndSliderOnly() {
        assertThat(ChallengeTypes.toVcType(ChallengeCaptchaType.VALUE_IMAGE))
                .isEqualTo(ChallengeTypes.VC_IMAGE);
        assertThat(ChallengeTypes.toVcType(ChallengeCaptchaType.VALUE_SLIDER))
                .isEqualTo(ChallengeTypes.VC_IMAGE);
        assertThat(ChallengeTypes.toVcType("slider")).isEqualTo(ChallengeTypes.VC_IMAGE);
        assertThat(ChallengeTypes.toVcType("SMS")).isNull();
        assertThat(ChallengeTypes.toVcType("EMAIL")).isNull();
        assertThat(ChallengeTypes.toVcType(null)).isNull();
        assertThat(ChallengeTypes.toVcType(" ")).isNull();
    }

    @Test
    void isVcPath_coversPrefix() {
        assertThat(ChallengeTypes.isVcPath("/vc")).isTrue();
        assertThat(ChallengeTypes.isVcPath("/vc/image/check")).isTrue();
        assertThat(ChallengeTypes.isVcPath("/vc/**")).isTrue();
        assertThat(ChallengeTypes.isVcPath("/bff/auth/login")).isFalse();
        assertThat(ChallengeTypes.isVcPath(null)).isFalse();
    }
}
