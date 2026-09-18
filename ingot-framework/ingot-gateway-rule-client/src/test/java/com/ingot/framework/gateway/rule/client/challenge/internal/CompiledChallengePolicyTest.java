package com.ingot.framework.gateway.rule.client.challenge.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.ingot.cloud.security.api.model.enums.ChallengeCaptchaType;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger;
import com.ingot.framework.gateway.rule.client.model.EndpointPattern;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;

/**
 * {@link CompiledChallengePolicy} 跳过 SMS/EMAIL 与 {@code /vc} 路径，以及 {@code matchByScope}。
 *
 * @author jy
 * @since 1.0.0
 */
class CompiledChallengePolicyTest {

    @Test
    void compile_skipsUnsupportedType() {
        ChallengePolicy sms = ChallengePolicy.builder()
                .code("sms")
                .enabled(true)
                .challengeType("SMS")
                .trigger(ChallengeTrigger.ALWAYS)
                .patternList(List.of(EndpointPattern.of("/bff/auth/platform/login", "POST")))
                .priority(0)
                .build();
        CompiledChallengePolicy compiled = CompiledChallengePolicy.compile(List.of(sms), code -> null);
        assertThat(compiled.match("/bff/auth/platform/login", HttpMethod.POST, ChallengeTrigger.ALWAYS)).isNull();
        assertThat(compiled.all()).isEmpty();
    }

    @Test
    void compile_skipsVcPath() {
        ChallengePolicy vc = ChallengePolicy.builder()
                .code("vc")
                .enabled(true)
                .challengeType(ChallengeCaptchaType.VALUE_SLIDER)
                .trigger(ChallengeTrigger.ALWAYS)
                .patternList(List.of(EndpointPattern.of("/vc/**", "ANY")))
                .priority(0)
                .build();
        CompiledChallengePolicy compiled = CompiledChallengePolicy.compile(List.of(vc), code -> null);
        assertThat(compiled.match("/vc/image/check", HttpMethod.POST, ChallengeTrigger.ALWAYS)).isNull();
        assertThat(compiled.all()).isEmpty();
    }

    @Test
    void compile_matchesSupportedLoginPath() {
        ChallengePolicy login = ChallengePolicy.builder()
                .code("login-always")
                .enabled(true)
                .challengeType(ChallengeCaptchaType.VALUE_SLIDER)
                .trigger(ChallengeTrigger.ALWAYS)
                .scope("login")
                .patternList(List.of(EndpointPattern.of("/bff/auth/platform/login", "POST")))
                .priority(0)
                .build();
        ChallengePolicy anon = ChallengePolicy.builder()
                .code("anon-rl")
                .enabled(true)
                .challengeType(ChallengeCaptchaType.VALUE_SLIDER)
                .trigger(ChallengeTrigger.ON_RATE_LIMIT)
                .scope("e2e-anon")
                .patternList(List.of(EndpointPattern.of("/iam/**", "ANY")))
                .priority(10)
                .build();
        CompiledChallengePolicy compiled = CompiledChallengePolicy.compile(List.of(login, anon), code -> null);
        assertThat(compiled.match("/bff/auth/platform/login", HttpMethod.POST, ChallengeTrigger.ALWAYS))
                .isNotNull()
                .extracting(ChallengePolicy::getCode)
                .isEqualTo("login-always");
        assertThat(compiled.matchByScope("/bff/auth/platform/login", HttpMethod.POST, "login"))
                .isNotNull()
                .extracting(ChallengePolicy::getCode)
                .isEqualTo("login-always");
        assertThat(compiled.matchByScope("/iam/user", HttpMethod.POST, "login")).isNull();
        assertThat(compiled.matchByScope("/iam/user", HttpMethod.POST, "e2e-anon"))
                .isNotNull()
                .extracting(ChallengePolicy::getScope)
                .isEqualTo("e2e-anon");
        assertThat(compiled.matchByScope("/bff/auth/platform/login", HttpMethod.POST, "e2e-anon")).isNull();
        assertThat(compiled.matchByScope("/bff/auth/platform/login", HttpMethod.POST, "")).isNull();
    }
}
