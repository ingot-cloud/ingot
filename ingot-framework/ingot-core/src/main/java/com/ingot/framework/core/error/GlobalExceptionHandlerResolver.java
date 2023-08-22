package com.ingot.framework.core.error;

import com.ingot.framework.core.error.exception.BizException;
import com.ingot.framework.core.model.status.BaseErrorCode;
import com.ingot.framework.core.model.support.R;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
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

import java.util.List;
import java.util.Set;

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
        log.error("[GlobalExceptionHandlerResolver] - Exception - message={}",
                e.getLocalizedMessage(), e);
        return R.error(BaseErrorCode.ILLEGAL_REQUEST_PARAMS.getCode(),
                e.getLocalizedMessage());
    }

    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public R<?> baseExceptionHandler(BizException e) {
        log.error("[GlobalExceptionHandlerResolver] - BizException - message={}",
                e.getLocalizedMessage(), e);
        return R.error(e.getCode(), e.getLocalizedMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<?> illegalArgumentException(IllegalArgumentException e) {
        log.error("[GlobalExceptionHandlerResolver] - IllegalArgumentException - message={}",
                e.getLocalizedMessage(), e);
        return R.error(BaseErrorCode.ILLEGAL_REQUEST_PARAMS.getCode(),
                e.getLocalizedMessage());
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<?> bindException(BindException e) {
        log.error("[GlobalExceptionHandlerResolver] - BindException - message={}",
                e.getLocalizedMessage(), e);
        List<ObjectError> list = e.getBindingResult().getAllErrors();
        String message = list.stream().map(ObjectError::getDefaultMessage)
                .reduce((l, r) -> l + ";" + r)
                .orElse("");
        return R.errorF(BaseErrorCode.ILLEGAL_REQUEST_PARAMS, message);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<?> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("[GlobalExceptionHandlerResolver] - MethodArgumentNotValidException - message={}",
                e.getLocalizedMessage(), e);
        List<ObjectError> list = e.getBindingResult().getAllErrors();
        String message = list.stream().map(ObjectError::getDefaultMessage)
                .reduce((l, r) -> l + ";" + r)
                .orElse("");
        return R.errorF(BaseErrorCode.ILLEGAL_REQUEST_PARAMS, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<?> constraintViolationException(ConstraintViolationException e) {
        log.error("[GlobalExceptionHandlerResolver] - ConstraintViolationException - message={}",
                e.getLocalizedMessage(), e);
        Set<ConstraintViolation<?>> constraintViolations = e.getConstraintViolations();
        String message = constraintViolations.stream()
                .map(o -> o.getMessage())
                .reduce((l, r) -> l + ";" + r)
                .orElse("");
        return R.errorF(BaseErrorCode.ILLEGAL_REQUEST_PARAMS, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ResponseBody
    public R<?> httpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("[GlobalExceptionHandlerResolver] - HttpRequestMethodNotSupportedException - message={}",
                e.getLocalizedMessage(), e);
        return R.errorF(BaseErrorCode.METHOD_NOT_ALLOWED, e.getLocalizedMessage());
    }
}
