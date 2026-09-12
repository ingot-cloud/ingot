package com.ingot.framework.data.mybatis.scope.error;

import com.ingot.framework.commons.model.support.R;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <p>授权快照与数据范围运行时异常的 HTTP 映射。</p>
 *
 * @author jy
 * @since 1.0.0
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 40)
@RestControllerAdvice
public class AuthorizationExceptionHandler {

    /**
     * 授权服务不可用或过期刷新失败。
     *
     * @param ex 快照异常
     * @return 503 错误响应
     */
    @ExceptionHandler(AuthorizationSnapshotException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public R<?> handleSnapshotUnavailable(AuthorizationSnapshotException ex) {
        return R.error(ex.getCode(), ex.getLocalizedMessage());
    }

    /**
     * 数据范围判定失败：未认证 401、明确无权限 403、快照不可用 503。
     *
     * @param ex 数据范围异常
     * @return 对应 HTTP 状态的错误响应
     */
    @ExceptionHandler(DataScopeException.class)
    public ResponseEntity<R<?>> handleDataScope(DataScopeException ex) {
        HttpStatus status = HttpStatus.FORBIDDEN;
        if (DataScopeErrorCode.DS_401.getCode().equals(ex.getCode())) {
            status = HttpStatus.UNAUTHORIZED;
        } else if (DataScopeErrorCode.DS_503.getCode().equals(ex.getCode())) {
            status = HttpStatus.SERVICE_UNAVAILABLE;
        }
        return ResponseEntity.status(status).body(R.error(ex.getCode(), ex.getLocalizedMessage()));
    }
}
