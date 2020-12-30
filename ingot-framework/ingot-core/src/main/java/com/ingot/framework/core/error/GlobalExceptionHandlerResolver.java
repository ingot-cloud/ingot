package com.ingot.framework.core.error;

import com.ingot.framework.base.exception.BaseException;
import com.ingot.framework.base.status.BaseStatusCode;
import com.ingot.framework.core.wrapper.IngotResponse;
import com.ingot.framework.core.wrapper.ResponseWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

import static com.ingot.framework.base.status.BaseStatusCode.ILLEGAL_OPERATION;

/**
 * <p>Description  : GlobalExceptionHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/5/2.</p>
 * <p>Time         : 下午2:33.</p>
 */
@Slf4j
@Order
@RestControllerAdvice
public class GlobalExceptionHandlerResolver {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public IngotResponse<?> Exception(Exception e) {
        log.error("Exception - message={}, e={}", e.getLocalizedMessage(), e);
        return ResponseWrapper.error(BaseStatusCode.ILLEGAL_REQUEST_PARAMS.code(),
                String.format(BaseStatusCode.ILLEGAL_REQUEST_PARAMS.message(), e.getLocalizedMessage()));
    }

    @ExceptionHandler(BaseException.class)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public IngotResponse<?> baseExceptionHandler(BaseException e) {
        log.error("BaseException - message={}, e={}", e.getLocalizedMessage(), e);
        return ResponseWrapper.error(e.getCode(), e.getLocalizedMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public IngotResponse<?> illegalArgumentException(IllegalArgumentException e) {
        log.error("IllegalArgumentException - message={}, e={}", e.getLocalizedMessage(), e);
        return ResponseWrapper.error(BaseStatusCode.ILLEGAL_REQUEST_PARAMS.code(),
                String.format(BaseStatusCode.ILLEGAL_REQUEST_PARAMS.message(), e.getLocalizedMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public IngotResponse<?> methodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("MethodArgumentNotValidException - message={}, e={}", e.getLocalizedMessage(), e);
        List<ObjectError> list = e.getBindingResult().getAllErrors();
        String message = list.stream().map(ObjectError::getDefaultMessage)
                .reduce((l, r) -> l + ";" + r)
                .orElse("");
        return ResponseWrapper.error(ILLEGAL_OPERATION, message);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    @ResponseBody
    public IngotResponse<?> httpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("HttpRequestMethodNotSupportedException - message={}, e={}", e.getLocalizedMessage(), e);
        return ResponseWrapper.error(BaseStatusCode.METHOD_NOT_ALLOWED, e.getLocalizedMessage());
    }
}
