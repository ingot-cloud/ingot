package com.ingot.cloud.gateway.security;

import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.gateway.rule.client.challenge.ChallengePolicyService;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengePolicy;
import com.ingot.framework.gateway.rule.client.challenge.model.ChallengeTrigger;
import com.ingot.framework.vc.common.VCConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * 挑战过滤器。
 *
 * <p>顺序：{@link SecurityPolicyFilterOrder#CHALLENGE}，在 {@link BlacklistFilter} 之后、
 * {@link WhitelistAwareSentinelGatewayFilter} 之前。</p>
 *
 * <p>流程：</p>
 * <ol>
 *     <li>请求带 {@code _vc_pass_token}：消费 token，命中则放行（标记 attribute 让 Sentinel 跳过）</li>
 *     <li>未带 token + 命中 ALWAYS 挑战策略：返回 412 + challenge_required</li>
 *     <li>未命中 ALWAYS：交后续 Sentinel，限流触发时由 {@link SentinelBlockHandler} 检查
 *         {@link ChallengeTrigger#ON_RATE_LIMIT} 挑战策略，命中即返回 412 挑战，否则 429。</li>
 * </ol>
 *
 * <p>临时封禁（403）由 {@link SentinelBlockHandler} 在限流违规累积达阈值后异步写入，
 * 与验码失败次数无关。登录连续失败锁定由 account-domain 处理，不使用
 * {@code on_failure_threshold} 触发器。</p>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChallengeFilter implements GlobalFilter, Ordered {

    public static final String ATTR_PASS_TOKEN_OK = "ingot.security.passToken.ok";
    public static final String CHALLENGE_CODE = "CHALLENGE_REQUIRED";

    private final ObjectProvider<ChallengePolicyService> challengeProvider;
    private final PassTokenStore passTokenStore;
    private final ReactiveResponseWriter responseWriter;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (Boolean.TRUE.equals(exchange.getAttributes().get(BlacklistFilter.ATTR_WHITELISTED))) {
            return chain.filter(exchange);
        }
        ChallengePolicyService service = challengeProvider.getIfAvailable();
        if (service == null) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getURI().getPath();
        ChallengePolicy alwaysPolicy = service.match(path, exchange.getRequest().getMethod(),
                ChallengeTrigger.ALWAYS);

        String token = firstQueryParam(exchange, VCConstants.QUERY_PARAMS_PASS_TOKEN);
        if (token != null) {
            String scope = alwaysPolicy != null && alwaysPolicy.getScope() != null
                    ? alwaysPolicy.getScope() : "default";
            return passTokenStore.consume(scope, token)
                    .flatMap(ok -> {
                        if (Boolean.TRUE.equals(ok)) {
                            exchange.getAttributes().put(ATTR_PASS_TOKEN_OK, Boolean.TRUE);
                            return chain.filter(exchange);
                        }
                        if (alwaysPolicy != null) {
                            return writeChallenge(exchange, alwaysPolicy);
                        }
                        return chain.filter(exchange);
                    });
        }
        if (alwaysPolicy != null) {
            return writeChallenge(exchange, alwaysPolicy);
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return SecurityPolicyFilterOrder.CHALLENGE;
    }

    private Mono<Void> writeChallenge(ServerWebExchange exchange, ChallengePolicy policy) {
        Map<String, Object> data = ChallengeResponses.buildPayload(policy);
        exchange.getResponse().getHeaders().add(HttpHeaders.WWW_AUTHENTICATE,
                "Captcha realm=\"" + data.get("vcType") + "\"");
        return responseWriter.writeJson(exchange.getResponse(), HttpStatus.PRECONDITION_FAILED,
                R.error(data, CHALLENGE_CODE, "Captcha required"));
    }

    static String firstQueryParam(ServerWebExchange exchange, String key) {
        return exchange.getRequest().getQueryParams().getFirst(key);
    }
}
