package com.ingot.cloud.gateway.security;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowException;
import com.ingot.cloud.security.api.model.dto.BlacklistReportDTO;
import com.ingot.cloud.security.api.model.enums.BlacklistEventAction;
import com.ingot.cloud.security.api.model.enums.BlacklistTriggerSource;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.gateway.rule.client.blacklist.model.IpKeyType;
import com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Sentinel 网关限流拒绝时的自定义 {@link BlockRequestHandler}。
 *
 * <h3>处理流程</h3>
 * <ol>
 *     <li>异步累加违规计数（{@link ViolationCounter}，窗口见
 *         {@link GatewaySecurityConstants#VIOLATION_WINDOW_SECONDS}）</li>
 *     <li>窗口内违规 ≥ {@link GatewaySecurityConstants#VIOLATION_BLOCK_THRESHOLD}
 *         → {@link TempBlockStore} 临时封禁
 *         {@link GatewaySecurityConstants#TEMP_BLOCK_TTL_MINUTES} 分钟，
 *         后续由 {@link BlacklistFilter} 返回 403</li>
 *     <li>异步上报审计（{@link BlacklistEventReporter} → security 服务）</li>
 *     <li>匹配 {@link ChallengeTrigger#ON_RATE_LIMIT} 挑战策略 → 412；否则 429</li>
 * </ol>
 *
 * <h3>相关配置</h3>
 * <pre>{@code
 * ingot:
 *   security:
 *     ratelimit:
 *       enabled: true
 *     challenge:
 *       enabled: true
 *       policy:
 *         policies:
 *           - trigger: ON_RATE_LIMIT   # 限流后返回 412 而非 429
 *             challenge-type: SLIDER
 * }</pre>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SentinelBlockHandler implements BlockRequestHandler {

    private static final Duration VIOLATION_WINDOW =
            Duration.ofSeconds(GatewaySecurityConstants.VIOLATION_WINDOW_SECONDS);
    private static final Duration TEMP_BLOCK_TTL =
            Duration.ofMinutes(GatewaySecurityConstants.TEMP_BLOCK_TTL_MINUTES);

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
        ClientIdentity identity = (ClientIdentity) exchange.getAttributes()
                .get(GatewaySecurityConstants.ATTR_CLIENT_IDENTITY);
        String ruleCode = ex instanceof FlowException
                ? GatewaySecurityConstants.RULE_CODE_RATE_LIMIT
                : ex.getClass().getSimpleName();
        accumulateAndMaybeBlock(exchange, identity, ruleCode);

        ChallengePolicy challenge = matchOnRateLimitChallenge(exchange);
        if (challenge != null) {
            return writeChallenge(exchange, challenge).then(Mono.empty());
        }
        return writeRateLimit(exchange).then(Mono.empty());
    }

    private ChallengePolicy matchOnRateLimitChallenge(ServerWebExchange exchange) {
        ChallengePolicyService service = challengeProvider.getIfAvailable();
        if (service == null) {
            return null;
        }
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
                R.error(ChallengeResponses.buildPayload(policy),
                        GatewaySecurityConstants.CODE_CHALLENGE_REQUIRED,
                        GatewaySecurityConstants.MSG_CAPTCHA_REQUIRED));
    }

    private void accumulateAndMaybeBlock(ServerWebExchange exchange, ClientIdentity identity, String ruleCode) {
        if (identity == null || identity.getIp() == null) {
            return;
        }
        String keyType = IpKeyType.IP.dbCode();
        String keyValue = identity.getIp();
        violationCounter.incr(keyType, keyValue, ruleCode, VIOLATION_WINDOW)
                .flatMap(count -> {
                    if (count != null && count >= GatewaySecurityConstants.VIOLATION_BLOCK_THRESHOLD) {
                        log.info("[Sentinel] threshold reached, temp-block ip={} count={}",
                                keyValue, count);
                        BlacklistReportDTO dto = buildReport(exchange, identity, ruleCode,
                                count.intValue(), (int) TEMP_BLOCK_TTL.getSeconds());
                        reporter.report(dto);
                        return tempBlockStore.block(keyType, keyValue, ruleCode, TEMP_BLOCK_TTL);
                    }
                    return Mono.empty();
                })
                .subscribe(v -> {}, e -> log.warn("[Sentinel] accumulateAndMaybeBlock failed", e));
    }

    private static BlacklistReportDTO buildReport(ServerWebExchange exchange, ClientIdentity identity,
                                                  String ruleCode, int count, int ttl) {
        return BlacklistReportDTO.builder()
                .keyType(IpKeyType.IP.dbCode())
                .keyValue(identity.getIp())
                .action(BlacklistEventAction.BLOCK.getCode())
                .triggerSource(BlacklistTriggerSource.AUTO.getCode())
                .ruleCode(ruleCode)
                .countInWindow(count)
                .ttlSec(ttl)
                .realIp(identity.getIp())
                .userAgent(identity.getUserAgent())
                .requestPath(exchange.getRequest().getURI().getPath())
                .build();
    }

    private Mono<Void> writeRateLimit(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add(HttpHeaders.RETRY_AFTER, GatewaySecurityConstants.RETRY_AFTER_SECONDS);
        return responseWriter.writeJson(response, HttpStatus.TOO_MANY_REQUESTS,
                R.error(GatewaySecurityConstants.CODE_LIMIT_TOO_MANY,
                        GatewaySecurityConstants.MSG_TOO_MANY_REQUESTS));
    }
}
