package com.ingot.framework.security.provider;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.base.status.BaseStatusCode;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.core.wrapper.ResponseWrapper;
import com.ingot.framework.security.constants.AcResponseCode;
import lombok.extern.slf4j.Slf4j;
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
public class SecurityExceptionHandlerResolver {

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public IngotResponse unAuthorizedException(AccessDeniedException e) {
        log.error(">>> GlobalExceptionHandler AccessDeniedException - message={}, e={}", e.getMessage(), e);
        return ResponseWrapper.error(BaseStatusCode.UNAUTHORIZED);
    }

    @ExceptionHandler(InsufficientAuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public IngotResponse insufficientAuthenticationExceptionHandler(InsufficientAuthenticationException ex) {
        log.error(">>> AuthExceptionHandler InsufficientAuthenticationException message={}, e={}", ex.getMessage(), ex);
        if (StrUtil.startWithIgnoreCase(ex.getMessage(), "Access token expired")){
            return ResponseWrapper.error(AcResponseCode.AC10010004);
        }
        return ResponseWrapper.error(BaseStatusCode.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public IngotResponse badCredentialsExceptionExceptionHandler(BadCredentialsException ex) {
        log.error(">>> AuthExceptionHandler BadCredentialsException message={}, e={}", ex.getMessage(), ex);
        return ResponseWrapper.error(BaseStatusCode.UNAUTHORIZED.code(), ex.getMessage());
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ResponseBody
    public IngotResponse authenticationExceptionHandler(AuthenticationException ex) {
        log.error(">>> AuthExceptionHandler AuthenticationException message={}, e={}", ex.getMessage(), ex);
        return ResponseWrapper.error(BaseStatusCode.UNAUTHORIZED);
    }
}
