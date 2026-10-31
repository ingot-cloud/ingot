package com.ingot.cloud.bff.error;

import com.ingot.framework.commons.model.bff.BffErrorCode;
import com.ingot.framework.commons.model.support.R;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <p>
 * 仅映射BFF认证协议异常，不把安全存储失败伪装成过期或登录成功。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ingot.cloud.bff.web")
@lombok.RequiredArgsConstructor
public class BffAuthExceptionHandler {

    private final com.ingot.cloud.bff.service.BffMetrics metrics;

    /** 稳定协议错误及对应 HTTP 状态。 */
    @ExceptionHandler(BffAuthException.class)
    public ResponseEntity<R<?>> handle(BffAuthException exception) {
        return response(exception.getErrorCode());
    }

    /** 非法或未声明的JSON字段统一拒绝，不影响其他模块异常策略。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<?>> badJson(HttpMessageNotReadableException exception) {
        return response(BffErrorCode.INVALID_REQUEST);
    }

    /** Redis不可用失败关闭，并保留服务不可用语义。 */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<R<?>> storageUnavailable(DataAccessException exception) {
        return response(BffErrorCode.AUTH_UNAVAILABLE);
    }

    private ResponseEntity<R<?>> response(BffErrorCode error) {
        metrics.event("failure", "protocol", error.getCode());
        return ResponseEntity.status(error.getHttpStatus()).body(R.error(error));
    }

}
