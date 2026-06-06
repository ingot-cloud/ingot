package com.ingot.cloud.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.model.support.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * 网关侧统一 JSON 响应写出工具。
 *
 * <p>安全策略拒绝路径均通过本类输出 {@link R} 结构体，保证前端与 BFF 解析一致：</p>
 * <ul>
 *     <li>{@link BlacklistFilter} — 403 + {@link GatewaySecurityConstants#CODE_FORBIDDEN_BLOCKED}</li>
 *     <li>{@link ChallengeFilter} — 412 + {@link GatewaySecurityConstants#CODE_CHALLENGE_REQUIRED}</li>
 *     <li>{@link SentinelBlockHandler} — 429 / 412 + 对应业务码</li>
 * </ul>
 *
 * <p>若响应已提交（{@code response.isCommitted()}），直接返回 {@code Mono.empty()} 避免重复写出。</p>
 *
 * <h3>响应示例</h3>
 * <pre>{@code
 * // 403 黑名单
 * { "code": "FORBIDDEN_BLOCKED", "msg": "Request blocked", "data": null }
 * // 412 挑战
 * { "code": "CHALLENGE_REQUIRED", "msg": "Captcha required", "data": { "vcType": "...", ... } }
 * }</pre>
 *
 * @author jy
 * @since 2026/5/26
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReactiveResponseWriter {

    private final ObjectMapper objectMapper;

    /**
     * 直接以指定状态码写出 R 失败响应。
     */
    public Mono<Void> writeJson(ServerHttpResponse response, HttpStatus status, R<?> body) {
        if (response.isCommitted()) {
            return Mono.empty();
        }
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBufferFactory factory = response.bufferFactory();
        return response.writeWith(Mono.fromSupplier(() -> {
            try {
                return factory.wrap(objectMapper.writeValueAsBytes(body));
            } catch (JsonProcessingException e) {
                log.warn("[ReactiveResponseWriter] write json failed", e);
                return factory.wrap(new byte[0]);
            }
        }));
    }
}
