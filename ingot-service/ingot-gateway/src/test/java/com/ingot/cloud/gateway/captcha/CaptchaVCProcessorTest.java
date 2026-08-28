package com.ingot.cloud.gateway.captcha;

import java.util.List;

import com.anji.captcha.model.common.ResponseModel;
import com.anji.captcha.service.CaptchaService;
import com.ingot.cloud.gateway.security.PassTokenStore;
import com.ingot.cloud.security.api.model.enums.ChallengeCaptchaType;
import com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.vc.common.VCConstants;
import com.ingot.framework.vc.common.VCException;
import com.ingot.framework.vc.common.VCType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.reactive.function.server.MockServerRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.reactive.result.view.ViewResolver;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CaptchaVCProcessor#check} PassToken 签发与 fail-closed。
 *
 * @author jy
 * @since 1.0.0
 */
class CaptchaVCProcessorTest {

    private final CaptchaService captchaService = mock(CaptchaService.class);
    private final ChallengePolicyService challengeService = mock(ChallengePolicyService.class);
    private final PassTokenStore passTokenStore = mock(PassTokenStore.class);
    private CaptchaVCProcessor processor;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<ChallengePolicyService> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(challengeService);
        processor = new CaptchaVCProcessor(captchaService, provider, passTokenStore);
        ResponseModel ok = mock(ResponseModel.class);
        when(ok.isSuccess()).thenReturn(true);
        when(captchaService.check(any())).thenReturn(ok);
    }

    @Test
    void check_withoutScope_doesNotIssue() {
        ServerRequest request = MockServerRequest.builder().build();

        assertDoesNotThrow(() -> processor.check(VCType.IMAGE, request).block());
        verify(passTokenStore, never()).issue(anyString(), anyInt(), anyInt());
    }

    @Test
    void check_queryScopeWithoutHeader_doesNotIssue() {
        ServerRequest request = MockServerRequest.builder()
                .queryParam(VCConstants.HEADER_SCOPE, "login")
                .build();

        assertDoesNotThrow(() -> processor.check(VCType.IMAGE, request).block());
        verify(passTokenStore, never()).issue(anyString(), anyInt(), anyInt());
    }

    @Test
    void check_withScope_issuesPassToken() {
        ServerRequest request = MockServerRequest.builder()
                .header(VCConstants.HEADER_SCOPE, "login")
                .build();
        when(challengeService.findByScope("login")).thenReturn(loginPolicy());
        when(passTokenStore.issue("login", 300, 3)).thenReturn(Mono.just("tok"));

        assertDoesNotThrow(() -> processor.check(VCType.IMAGE, request).block());
        verify(passTokenStore).issue("login", 300, 3);
    }

    @Test
    void check_withScope_responseOmitsCaptcha() {
        ServerRequest request = MockServerRequest.builder()
                .header(VCConstants.HEADER_SCOPE, "login")
                .build();
        when(challengeService.findByScope("login")).thenReturn(loginPolicy());
        when(passTokenStore.issue("login", 300, 3)).thenReturn(Mono.just("tok"));

        ServerResponse response = processor.check(VCType.IMAGE, request).block();
        assertNotNull(response);
        String body = writeBody(response);
        assertFalse(body.contains("\"captcha\""));
        assertTrue(body.contains(VCConstants.HEADER_PASS_TOKEN));
        assertTrue(body.contains("tok"));
        assertTrue(body.contains(VCConstants.HEADER_SCOPE));
    }

    @Test
    void check_redisUnavailable_failsClosed() {
        ServerRequest request = MockServerRequest.builder()
                .header(VCConstants.HEADER_SCOPE, "login")
                .build();
        when(challengeService.findByScope("login")).thenReturn(loginPolicy());
        when(passTokenStore.issue(eq("login"), eq(300), eq(3))).thenReturn(Mono.empty());

        assertThrows(VCException.class, () -> processor.check(VCType.IMAGE, request).block());
    }

    @Test
    void check_unknownScope_failsClosed() {
        ServerRequest request = MockServerRequest.builder()
                .header(VCConstants.HEADER_SCOPE, "missing")
                .build();
        when(challengeService.findByScope("missing")).thenReturn(null);

        assertThrows(VCException.class, () -> processor.check(VCType.IMAGE, request).block());
        verify(passTokenStore, never()).issue(anyString(), anyInt(), anyInt());
    }

    private static String writeBody(ServerResponse response) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/vc/image/check").build());
        HandlerStrategies strategies = HandlerStrategies.withDefaults();
        response.writeTo(exchange, new ServerResponse.Context() {
            @Override
            public List<HttpMessageWriter<?>> messageWriters() {
                return strategies.messageWriters();
            }

            @Override
            public List<ViewResolver> viewResolvers() {
                return strategies.viewResolvers();
            }
        }).block();
        String body = exchange.getResponse().getBodyAsString().block();
        assertNotNull(body);
        return body;
    }

    private static ChallengePolicy loginPolicy() {
        return ChallengePolicy.builder()
                .code("login-always")
                .challengeType(ChallengeCaptchaType.VALUE_SLIDER)
                .scope("login")
                .passTokenTtlSec(300)
                .passTokenRemaining(3)
                .enabled(true)
                .build();
    }
}
