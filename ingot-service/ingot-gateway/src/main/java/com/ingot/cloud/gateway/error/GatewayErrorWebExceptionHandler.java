package com.ingot.cloud.gateway.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import com.ingot.framework.commons.model.support.R;
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

    /** 保留已知状态与业务码，异常实现细节只写入日志。 */
    @NonNull
    @Override
    public Mono<Void> handle(ServerWebExchange exchange, @NonNull Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        log.debug("[GatewayErrorWebExceptionHandler] - error", ex);

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        R<?> r;
        if (ex instanceof BizException business
                && !BaseErrorCode.INTERNAL_SERVER_ERROR.getCode().equals(business.getCode())) {
            r = R.error(business.getCode(), business.getMessage());
        } else if (ex instanceof ResponseStatusException status) {
            HttpStatusCode statusCode = status.getStatusCode();
            response.setStatusCode(statusCode);
            r = R.error(switch (statusCode.value()) {
                case 400 -> BaseErrorCode.BAD_REQUEST;
                case 401 -> BaseErrorCode.UNAUTHORIZED;
                case 403 -> BaseErrorCode.FORBIDDEN;
                case 404 -> BaseErrorCode.NOT_FOUND;
                case 405 -> BaseErrorCode.METHOD_NOT_ALLOWED;
                case 503 -> BaseErrorCode.REQUEST_FALLBACK;
                default -> BaseErrorCode.INTERNAL_SERVER_ERROR;
            });
        } else {
            log.error("网关处理请求失败", ex);
            response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            r = R.error500();
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
