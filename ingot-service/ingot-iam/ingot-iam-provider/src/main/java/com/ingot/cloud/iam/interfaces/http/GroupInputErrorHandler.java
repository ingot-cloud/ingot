package com.ingot.cloud.iam.interfaces.http;

import com.ingot.cloud.iam.interfaces.http.platform.PlatformGroupAPI;
import com.ingot.cloud.iam.interfaces.http.tenant.TenantGroupAPI;
import com.ingot.framework.commons.model.iam.IamReasonCode;
import com.ingot.framework.commons.model.support.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <p>为两端组管理的请求体解析失败返回场景提示。</p>
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {PlatformGroupAPI.class, TenantGroupAPI.class})
public class GroupInputErrorHandler {
    /**
     * 将请求体解析失败映射为业务参数错误。
     * @param failure 原始解析异常
     * @return 用户组场景的 400 响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<?>> unreadable(HttpMessageNotReadableException failure) {
        log.debug("用户组请求体解析失败", failure);
        var reason = IamReasonCode.INVALID_ARGUMENT;
        return ResponseEntity.status(reason.getHttpStatus())
                .body(R.error(reason.getCode(), "用户组请求格式有误，请检查后重试"));
    }
}
