package com.ingot.framework.authorization;

import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.support.R;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <p>仅映射 SDK 异常为 401/403/404/503 等 IAM 状态，其他业务异常仍交给原处理器。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SdkAuthorizationErrorHandler {

    /**
     * 输出不包含敏感数据的稳定错误信封。
     * @param exception SDK异常
     * @return 对应 HTTP 状态
     */
    @ExceptionHandler(SdkAuthorizationException.class)
    public ResponseEntity<R<?>> handle(SdkAuthorizationException exception) {
        var reason = IamReasonCode.find(exception.getCode());
        return ResponseEntity.status(reason.getHttpStatus()).body(R.error(reason));
    }

}
