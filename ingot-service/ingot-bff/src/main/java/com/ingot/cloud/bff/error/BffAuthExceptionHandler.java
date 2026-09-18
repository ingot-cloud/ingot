package com.ingot.cloud.bff.error;

import com.ingot.framework.commons.model.support.R;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将 BFF 登录错误映射为契约 HTTP 状态与稳定 code。
 *
 * @author jy
 * @since 1.0.0
 */
@RestControllerAdvice
public class BffAuthExceptionHandler {

    @ExceptionHandler(BffAuthException.class)
    public ResponseEntity<R<?>> handle(BffAuthException exception) {
        return ResponseEntity.status(exception.getErrorCode().getHttpStatus())
                .body(R.error(exception.getErrorCode()));
    }
}
