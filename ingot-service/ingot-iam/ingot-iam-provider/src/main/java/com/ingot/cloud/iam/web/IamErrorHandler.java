package com.ingot.cloud.iam.web;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.support.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <p>把 IAM 对外管理面的稳定业务错误码映射为契约约定的 HTTP 状态，不改变其他服务的错误协议。</p>
 *
 * <p>只作用于 {@code /v1} 管理面控制器：命中 {@link IamReasonCode} 时按其声明的状态返回，
 * 其余业务错误码沿用框架全局处理器的既有 500 行为，避免为 IAM 改写全仓库 {@link BizException} 语义。
 * 内部 RPC 不在范围内，仍以 R 信封回传错误码供调用方解析。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see com.ingot.framework.core.error.GlobalExceptionHandlerResolver
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ingot.cloud.iam.web.v1")
public class IamErrorHandler {

    /**
     * 按稳定错误码映射 IAM 业务错误的 HTTP 状态。
     *
     * @param exception 业务异常
     * @return 保持 R 信封的错误响应；非 IAM 错误码沿用 500
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<?>> bizException(BizException exception) {
        IamReasonCode reason = IamReasonCode.find(exception.getCode());
        if (reason == null) {
            log.error("[IamErrorHandler] 非 IAM 业务错误码 code={}", exception.getCode(), exception);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(R.error(exception.getCode(), exception.getLocalizedMessage()));
        }
        log.warn("[IamErrorHandler] code={} status={} message={}",
                reason.getCode(), reason.getHttpStatus(), exception.getLocalizedMessage());
        String message = exception.getLocalizedMessage();
        return ResponseEntity.status(reason.getHttpStatus())
                .body(R.error(reason.getCode(),
                        message == null || message.isBlank() ? reason.getText() : message));
    }
}
