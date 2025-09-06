package com.ingot.framework.core.error;

import java.util.List;
import java.util.Set;

import com.ingot.framework.commons.model.status.BaseErrorCode;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.commons.error.BizException;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
                .map(ConstraintViolation::getMessage)
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

    /**
     * 避免 404 重定向到 /error 导致NPE ,ingot.security.oauth2.resource.publicUrls 需要配置对应端点
     *
     * @return R
     */
    @DeleteMapping("/error")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<?> noHandlerFoundException() {
        return R.error(BaseErrorCode.NOT_FOUND.getCode(), HttpStatus.NOT_FOUND.getReasonPhrase());
    }

    /**
     * 保持和低版本请求路径不存在的行为一致
     * <p>
     * <a href="https://github.com/spring-projects/spring-boot/issues/38733">[Spring Boot
     * 3.2.0] 404 Not Found behavior #38733</a>
     *
     * @param exception {@link NoResourceFoundException}
     * @return R
     */
    @ExceptionHandler({NoResourceFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<?> noResourceFoundException(NoResourceFoundException exception) {
        log.error("[GlobalExceptionHandlerResolver] - noResourceFoundException 404 {}", exception.getLocalizedMessage());
        return R.error(BaseErrorCode.NOT_FOUND.getCode(), exception.getLocalizedMessage());
    }
}
