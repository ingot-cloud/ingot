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
 * 网关侧统一 JSON 响应工具：拒绝 / 限流 / 挑战均通过本类输出 {@link R} 结构体。
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
