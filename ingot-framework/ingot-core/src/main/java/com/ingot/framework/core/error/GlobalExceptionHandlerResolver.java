package com.ingot.framework.core.error;

import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import com.ingot.framework.common.exception.BizException;
import com.ingot.framework.common.status.BaseStatusCode;
import com.ingot.framework.core.wrapper.R;
import com.ingot.framework.core.wrapper.ResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * <p>Description  : GlobalExceptionHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/5/2.</p>
 * <p>Time         : 下午2:33.</p>
 */
@Slf4j
@Order
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalExceptionHandlerResolver {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public R<?> exception(Exception e) {
        log.error("[GlobalExceptionHandlerResolver] - Exception - message={}, e={}",
                e.getLocalizedMessage(), e);
        return ResponseWrapper.error(BaseStatusCode.ILLEGAL_REQUEST_PARAMS.getCode(),
                e.getLocalizedMessage());
    }

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public R<?> baseExceptionHandler(BizException e) {
        log.error("[GlobalExceptionHandlerResolver] - BizException - message={}, e={}",
                e.getLocalizedMessage(), e);
        return ResponseWrapper.error(e.getCode(), e.getLocalizedMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<?> illegalArgumentException(IllegalArgumentException e) {
        log.error("[GlobalExceptionHandlerResolver] - IllegalArgumentException - message={}, e={}",
                e.getLocalizedMessage(), e);
        return ResponseWrapper.error(BaseStatusCode.ILLEGAL_REQUEST_PARAMS.getCode(),
                e.getLocalizedMessage());
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<?> bindException(BindException e) {
        log.error("[GlobalExceptionHandlerResolver] - BindException - message={}, e={}",
                e.getLocalizedMessage(), e);
        List<ObjectError> list = e.getBindingResult().getAllErrors();
        String message = list.stream().map(ObjectError::getDefaultMessage)
                .reduce((l, r) -> l + ";" + r)
                .orElse("");
        return ResponseWrapper.errorF(BaseStatusCode.ILLEGAL_REQUEST_PARAMS, message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<?> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("[GlobalExceptionHandlerResolver] - MethodArgumentNotValidException - message={}, e={}",
                e.getLocalizedMessage(), e);
        List<ObjectError> list = e.getBindingResult().getAllErrors();
        String message = list.stream().map(ObjectError::getDefaultMessage)
                .reduce((l, r) -> l + ";" + r)
                .orElse("");
        return ResponseWrapper.errorF(BaseStatusCode.ILLEGAL_REQUEST_PARAMS, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<?> constraintViolationException(ConstraintViolationException e) {
        log.error("[GlobalExceptionHandlerResolver] - ConstraintViolationException - message={}, e={}",
                e.getLocalizedMessage(), e);
        Set<ConstraintViolation<?>> constraintViolations = e.getConstraintViolations();
        String message = constraintViolations.stream()
                .map(o -> o.getMessage())
                .reduce((l, r) -> l + ";" + r)
                .orElse("");
        return ResponseWrapper.errorF(BaseStatusCode.ILLEGAL_REQUEST_PARAMS, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ResponseBody
    public R<?> httpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("[GlobalExceptionHandlerResolver] - HttpRequestMethodNotSupportedException - message={}, e={}",
                e.getLocalizedMessage(), e);
        return ResponseWrapper.errorF(BaseStatusCode.METHOD_NOT_ALLOWED, e.getLocalizedMessage());
    }
}
