package com.ingot.framework.security.provider.error;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.common.status.BaseStatusCode;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.core.wrapper.ResponseWrapper;
import com.ingot.framework.security.status.SecurityStatusCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <p>Description  : SecurityExceptionHandlerResolver.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/11/12.</p>
 * <p>Time         : 11:44 AM.</p>
 */
@Slf4j
@Order(value = Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityExceptionHandlerResolver {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public IngotResponse<?> accessDeniedException(AccessDeniedException e) {
        log.error("AccessDeniedException - message={}, e={}", e.getMessage(), e);
        return ResponseWrapper.error500(BaseStatusCode.FORBIDDEN);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public IngotResponse<?> insufficientAuthenticationException(InsufficientAuthenticationException e) {
        log.error("InsufficientAuthenticationException message={}, e={}", e.getMessage(), e);
        if (StrUtil.startWithIgnoreCase(e.getMessage(), "Access token expired")) {
            return ResponseWrapper.error500(SecurityStatusCode.TOKEN_INVALID);
        }
        return ResponseWrapper.error500(BaseStatusCode.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public IngotResponse<?> badCredentialsException(BadCredentialsException e) {
        log.error("BadCredentialsException message={}, e={}", e.getMessage(), e);
        return ResponseWrapper.error500(BaseStatusCode.UNAUTHORIZED.code(), e.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public IngotResponse<?> authenticationException(AuthenticationException e) {
        log.error("AuthenticationException message={}, e={}", e.getMessage(), e);
        return ResponseWrapper.error500(BaseStatusCode.UNAUTHORIZED);
    }
}
