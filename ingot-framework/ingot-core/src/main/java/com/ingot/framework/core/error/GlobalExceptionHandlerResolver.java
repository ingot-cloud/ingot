package com.ingot.framework.core.error;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import com.ingot.framework.commons.model.support.R;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
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

    private static final String INVALID_FORMAT = "请检查请求参数的格式或选项";
    private static final String INVALID_TIME = "时间格式不正确，请使用带时区的时间";

    /** 记录意外异常，对外仅返回固定的服务错误。 */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public R<?> exception(Exception e) {
        log.error("[GlobalExceptionHandlerResolver] - Exception - message={}",
                e.getLocalizedMessage(), e);
        return R.error500();
    }

    /** 保留已声明的业务原因，通用内部错误只返回固定提示。 */
    @ExceptionHandler(BizException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public R<?> baseExceptionHandler(BizException e) {
        log.error("[GlobalExceptionHandlerResolver] - BizException - message={}",
                e.getLocalizedMessage(), e);
        return businessResponse(e);
    }

    /** 无法读取的请求体返回参数错误；业务解码异常保留原协议。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public R<?> httpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletResponse response) {
        response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        // 请求体读取/反序列化阶段抛出的业务异常（如字段级 HYBRID 完整性校验失败）会被
        // Jackson 包装，这里还原根因中的 BizException 以透出其错误码
        BizException biz = findBizException(e);
        if (biz != null) {
            log.error("[GlobalExceptionHandlerResolver] - HttpMessageNotReadableException - biz={}",
                    biz.getLocalizedMessage());
            return businessResponse(biz);
        }
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof MismatchedInputException input && isTimePoint(input.getTargetType())) {
                response.setStatus(HttpStatus.BAD_REQUEST.value());
                return R.errorF(BaseErrorCode.ILLEGAL_REQUEST_PARAMS, INVALID_TIME);
            }
        }
        log.error("[GlobalExceptionHandlerResolver] - HttpMessageNotReadableException - message={}",
                e.getLocalizedMessage(), e);
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        return R.errorF(BaseErrorCode.ILLEGAL_REQUEST_PARAMS, "请求内容格式不正确，请检查后重试");
    }

    /** 参数类型转换失败按请求错误处理，时间点使用专用提示。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public R<?> parameterTypeMismatch(MethodArgumentTypeMismatchException exception,
                                           HttpServletResponse response) {
        log.warn("请求参数类型错误", exception);
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        boolean time = isTimePoint(exception.getRequiredType()) || hasTimePointConversionFailure(exception);
        return R.errorF(BaseErrorCode.ILLEGAL_REQUEST_PARAMS, time ? INVALID_TIME : INVALID_FORMAT);
    }

    private static boolean isTimePoint(Class<?> type) {
        return type != null && (type == Instant.class || type == LocalDateTime.class
                || type == OffsetDateTime.class || type == ZonedDateTime.class || Date.class.isAssignableFrom(type));
    }

    private static boolean hasTimePointConversionFailure(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConversionFailedException conversion
                    && isTimePoint(conversion.getTargetType().getObjectType())) {
                return true;
            }
        }
        return false;
    }

    private BizException findBizException(Throwable throwable) {
        Throwable cause = throwable;
        while (cause != null) {
            if (cause instanceof BizException biz) {
                return biz;
            }
            cause = cause.getCause();
        }
        return null;
    }

    /** 参数非法时返回稳定业务码和可操作提示。 */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<?> illegalArgumentException(IllegalArgumentException e) {
        log.error("[GlobalExceptionHandlerResolver] - IllegalArgumentException - message={}",
                e.getLocalizedMessage(), e);
        return R.errorF(BaseErrorCode.ILLEGAL_REQUEST_PARAMS, INVALID_FORMAT);
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public R<?> bindException(BindException e) {
        log.error("[GlobalExceptionHandlerResolver] - BindException - message={}",
                e.getLocalizedMessage(), e);
        List<ObjectError> list = e.getBindingResult().getAllErrors();
        String message = list.stream().map(GlobalExceptionHandlerResolver::validationMessage)
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
        String message = list.stream().map(GlobalExceptionHandlerResolver::validationMessage)
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
        return R.error(BaseErrorCode.METHOD_NOT_ALLOWED);
    }

    /**
     * 避免 404 重定向到 /error 导致NPE ,ingot.security.oauth2.resource.publicUrls 需要配置对应端点
     *
     * @return R
     */
    @DeleteMapping("/error")
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public R<?> noHandlerFoundException() {
        return R.error(BaseErrorCode.NOT_FOUND);
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
        return R.error(BaseErrorCode.NOT_FOUND);
    }

    /** 拒绝访问时保留 403，不返回授权实现细节。 */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public R<?> handleAccessDenied(AccessDeniedException exception) {
        log.warn("请求被权限检查拒绝", exception);
        return R.error(BaseErrorCode.FORBIDDEN);
    }

    private static R<?> businessResponse(BizException exception) {
        return BaseErrorCode.INTERNAL_SERVER_ERROR.getCode().equals(exception.getCode())
                ? R.error500() : R.error(exception.getCode(), exception.getLocalizedMessage());
    }

    private static String validationMessage(ObjectError error) {
        return error instanceof FieldError field && field.isBindingFailure()
                ? INVALID_FORMAT : error.getDefaultMessage();
    }
}
