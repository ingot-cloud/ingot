package com.ingot.cloud.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>验证网关异常不会把上游实现细节传给管理端。</p>
 * @author jy
 * @since 1.0.0
 */
class GatewayErrorWebExceptionHandlerTest {
    private final ObjectMapper json = new ObjectMapper();
    private final GatewayErrorWebExceptionHandler handler = new GatewayErrorWebExceptionHandler(json);

    @Test
    void unexpectedGatewayFailureReturnsSafeHttp500() throws Exception {
        assertSafe(new IllegalStateException("fixture-diagnostic"), HttpStatus.INTERNAL_SERVER_ERROR,
                BaseErrorCode.INTERNAL_SERVER_ERROR);
    }

    @Test
    void responseStatusPreservesAccessDenialWithoutDiagnosticReason() throws Exception {
        assertSafe(new ResponseStatusException(HttpStatus.FORBIDDEN, "fixture-diagnostic"),
                HttpStatus.FORBIDDEN, BaseErrorCode.FORBIDDEN);
    }

    private void assertSafe(Throwable failure, HttpStatus status, BaseErrorCode code) throws Exception {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/fixture").build());
        handler.handle(exchange, failure).block();
        assertEquals(status, exchange.getResponse().getStatusCode());
        var body = json.readTree(exchange.getResponse().getBodyAsString().block());
        assertEquals(code.getCode(), body.path("code").asText());
        assertEquals(code.getText(), body.path("message").asText());
        assertFalse(body.toString().contains("fixture-diagnostic"));
    }
}
