package com.ingot.cloud.gateway.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.core.error.exception.BizException;
import com.ingot.framework.core.model.status.BaseErrorCode;
import com.ingot.framework.core.model.support.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>Description  : GatewayErrorWebExceptionHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/1/4.</p>
 * <p>Time         : 8:55 下午.</p>
 */
@Slf4j
@Order(-1)
@Component
@RequiredArgsConstructor
public class GatewayErrorWebExceptionHandler implements ErrorWebExceptionHandler {
    private final ObjectMapper objectMapper;

    @NonNull
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, @NonNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        log.debug("GatewayErrorWebExceptionHandler - error", ex);

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        R<?> r = R.error500(ex.getMessage());
        if (ex instanceof BizException) {
            r = R.error(((BizException) ex).getCode(), ex.getMessage());
        } else if (ex instanceof ResponseStatusException) {
            HttpStatusCode statusCode = ((ResponseStatusException) ex).getStatusCode();
            response.setStatusCode(statusCode);
            if (statusCode == HttpStatus.SERVICE_UNAVAILABLE) {
                r = R.error(
                        BaseErrorCode.REQUEST_FALLBACK.getCode(), ex.getMessage());
            }
        }

        R<?> finalResponse = r;
        return response.writeWith(Mono.fromSupplier(() -> {
            DataBufferFactory bufferFactory = response.bufferFactory();
            try {
                return bufferFactory.wrap(objectMapper.writeValueAsBytes(finalResponse));
            } catch (JsonProcessingException e) {
                log.error("GatewayErrorWebExceptionHandler - Error writing response", ex);
                return bufferFactory.wrap(new byte[0]);
            }
        }));
    }
}
