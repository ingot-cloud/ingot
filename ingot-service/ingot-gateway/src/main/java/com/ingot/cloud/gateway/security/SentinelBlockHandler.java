package com.ingot.cloud.gateway.security;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import org.springframework.beans.factory.ObjectProvider;
import com.ingot.cloud.security.api.model.dto.BlacklistReportDTO;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 网关被 Sentinel 拒绝时的自定义响应：
 *
 * <ol>
 *     <li>异步累加违规计数（{@link ViolationCounter}，60s 滑动窗口，按 IP 维度）</li>
 *     <li>窗口内违规 ≥ {@link #DEFAULT_BLOCK_THRESHOLD}（30 次）→ {@link TempBlockStore} 临时封禁
 *         {@link #DEFAULT_BLOCK_TTL}（15 分钟），后续请求由 {@link BlacklistFilter} 返回 403</li>
 *     <li>异步上报审计到 ingot-service-security（{@link BlacklistEventReporter}）</li>
 *     <li>匹配 ON_RATE_LIMIT 挑战策略 → 412；否则 429 + {@code Retry-After: 1}</li>
 * </ol>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SentinelBlockHandler implements BlockRequestHandler {

    /**
     * 滑动窗口 60s 内被拒超过该次数即临时封禁。
     */
    static final long DEFAULT_BLOCK_THRESHOLD = 30L;
    static final Duration DEFAULT_WINDOW = Duration.ofSeconds(60);
    static final Duration DEFAULT_BLOCK_TTL = Duration.ofMinutes(15);
    private static final String LIMIT_CODE = "LIMIT_TOO_MANY";

    private final ViolationCounter violationCounter;
    private final TempBlockStore tempBlockStore;
    private final BlacklistEventReporter reporter;
    private final ReactiveResponseWriter responseWriter;
    private final ObjectProvider<ChallengePolicyService> challengeProvider;

    @PostConstruct
    public void register() {
        GatewayCallbackManager.setBlockHandler(this);
        log.info("[Sentinel] custom BlockRequestHandler registered");
    }

    @Override
    public Mono<ServerResponse> handleRequest(ServerWebExchange exchange, Throwable ex) {
        ClientIdentity identity = (ClientIdentity) exchange.getAttributes().get(ClientIdentity.ATTR_KEY);
        String ruleCode = ex instanceof FlowException ? "RATE_LIMIT" : ex.getClass().getSimpleName();
        accumulateAndMaybeBlock(exchange, identity, ruleCode);

        ChallengePolicy challenge = matchOnRateLimitChallenge(exchange);
        if (challenge != null) {
            return writeChallenge(exchange, challenge).then(Mono.empty());
        }
        return writeRateLimit(exchange).then(Mono.empty());
    }

    private ChallengePolicy matchOnRateLimitChallenge(ServerWebExchange exchange) {
        ChallengePolicyService service = challengeProvider.getIfAvailable();
        if (service == null) return null;
        try {
            return service.match(exchange.getRequest().getURI().getPath(),
                    exchange.getRequest().getMethod(), ChallengeTrigger.ON_RATE_LIMIT);
        } catch (Exception e) {
            log.warn("[Sentinel] match challenge failed", e);
            return null;
        }
    }

    private Mono<Void> writeChallenge(ServerWebExchange exchange, ChallengePolicy policy) {
        return responseWriter.writeJson(exchange.getResponse(), HttpStatus.PRECONDITION_FAILED,
                R.error(ChallengeResponses.buildPayload(policy), ChallengeFilter.CHALLENGE_CODE,
                        "Captcha required"));
    }

    private void accumulateAndMaybeBlock(ServerWebExchange exchange, ClientIdentity identity, String ruleCode) {
        if (identity == null) return;
        String keyType = "IP";
        String keyValue = identity.getIp();
        if (keyValue == null) return;
        violationCounter.incr(keyType, keyValue, ruleCode, DEFAULT_WINDOW)
                .flatMap(count -> {
                    if (count != null && count >= DEFAULT_BLOCK_THRESHOLD) {
                        log.info("[Sentinel] threshold reached, temp-block ip={} count={}",
                                keyValue, count);
                        BlacklistReportDTO dto = buildReport(exchange, identity, ruleCode,
                                count.intValue(), (int) DEFAULT_BLOCK_TTL.getSeconds());
                        reporter.report(dto);
                        return tempBlockStore.block(keyType, keyValue, ruleCode, DEFAULT_BLOCK_TTL);
                    }
                    return Mono.empty();
                })
                .subscribe(v -> {}, e -> log.warn("[Sentinel] accumulateAndMaybeBlock failed", e));
    }

    private static BlacklistReportDTO buildReport(ServerWebExchange exchange, ClientIdentity identity,
                                                  String ruleCode, int count, int ttl) {
        BlacklistReportDTO dto = new BlacklistReportDTO();
        dto.setKeyType("IP");
        dto.setKeyValue(identity.getIp());
        dto.setAction("B");
        dto.setTriggerSource("A");
        dto.setRuleCode(ruleCode);
        dto.setCountInWindow(count);
        dto.setTtlSec(ttl);
        dto.setRealIp(identity.getIp());
        dto.setUserAgent(identity.getUserAgent());
        dto.setRequestPath(exchange.getRequest().getURI().getPath());
        return dto;
    }

    private Mono<Void> writeRateLimit(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(HttpHeaders.RETRY_AFTER, "1");
        return responseWriter.writeJson(response, HttpStatus.TOO_MANY_REQUESTS,
                R.error(LIMIT_CODE, "Too many requests"));
    }

}
