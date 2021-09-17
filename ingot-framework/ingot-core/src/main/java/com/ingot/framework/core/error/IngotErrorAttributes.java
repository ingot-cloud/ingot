package com.ingot.framework.core.error;

import com.ingot.framework.common.exception.BizException;
import com.ingot.framework.common.status.BaseStatusCode;
import com.ingot.framework.core.wrapper.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.ServletException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * <p>Description  : IngotErrorAttributes.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/1/10.</p>
 * <p>Time         : 5:24 PM.</p>
 */
@Slf4j
@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class IngotErrorAttributes implements ErrorAttributes, Ordered {
    private static final String ERROR_ATTRIBUTE = IngotErrorAttributes.class.getName() + ".ERROR";

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Throwable error = getError(webRequest);

        Map<String, Object> errorAttributes = new LinkedHashMap<>();
        errorAttributes.put("timestamp", new Date());
        addStatus(errorAttributes, webRequest);
        addErrorDetails(errorAttributes, error, webRequest, options.isIncluded(ErrorAttributeOptions.Include.STACK_TRACE));
        addPath(errorAttributes, webRequest);

        Map<String, Object> finalAttributes = new LinkedHashMap<>();
        finalAttributes.put(R.DATA, errorAttributes);

        // 获取真实 exception
        while (error instanceof ServletException && error.getCause() != null) {
            error = ((ServletException) error).getCause();
        }

        if (error instanceof BizException) {
            finalAttributes.put(R.CODE, ((BizException) error).getCode());
            finalAttributes.put(R.MESSAGE, error.getMessage());
        } else {
            finalAttributes.put(R.CODE, BaseStatusCode.INTERNAL_SERVER_ERROR.code());
            finalAttributes.put(R.MESSAGE, BaseStatusCode.INTERNAL_SERVER_ERROR.message());
        }


        return finalAttributes;
    }

    @Override
    public Throwable getError(WebRequest webRequest) {
        Throwable exception = getAttribute(webRequest, ERROR_ATTRIBUTE);
        if (exception == null) {
            exception = getAttribute(webRequest, "javax.servlet.error.exception");
        }
        return exception;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private void addStatus(Map<String, Object> errorAttributes,
                           RequestAttributes requestAttributes) {
        Integer status = getAttribute(requestAttributes,
                "javax.servlet.error.status_code");
        if (status == null) {
            errorAttributes.put("status", 999);
            errorAttributes.put("error", "None");
            return;
        }
        errorAttributes.put("status", status);
        try {
            errorAttributes.put("error", HttpStatus.valueOf(status).getReasonPhrase());
        } catch (Exception ex) {
            // Unable to obtain a reason
            errorAttributes.put("error", "Http Status " + status);
        }
    }

    private void addErrorDetails(Map<String, Object> errorAttributes, Throwable error,
                                 WebRequest webRequest, boolean includeStackTrace) {
        if (error != null) {
            while (error instanceof ServletException && error.getCause() != null) {
                error = ((ServletException) error).getCause();
            }
            errorAttributes.put("exception", error.getClass().getName());
            addErrorMessage(errorAttributes, error);
            if (includeStackTrace) {
                addStackTrace(errorAttributes, error);
            }
        }
        Object message = getAttribute(webRequest, "javax.servlet.error.message");
        if ((!StringUtils.isEmpty(message) || errorAttributes.get("message") == null)
                && !(error instanceof BindingResult)) {
            errorAttributes.put("message",
                    StringUtils.isEmpty(message) ? "No message available" : message);
        }
    }

    private void addErrorMessage(Map<String, Object> errorAttributes, Throwable error) {
        BindingResult result = extractBindingResult(error);
        if (result == null) {
            errorAttributes.put("message", error.getMessage());
            return;
        }
        if (result.getErrorCount() > 0) {
            errorAttributes.put("errors", result.getAllErrors());
            errorAttributes.put("message",
                    "Validation failed for object='" + result.getObjectName()
                            + "'. Error count: " + result.getErrorCount());
        } else {
            errorAttributes.put("message", "No errors");
        }
    }

    private BindingResult extractBindingResult(Throwable error) {
        if (error instanceof BindingResult) {
            return (BindingResult) error;
        }
        if (error instanceof MethodArgumentNotValidException) {
            return ((MethodArgumentNotValidException) error).getBindingResult();
        }
        return null;
    }

    private void addStackTrace(Map<String, Object> errorAttributes, Throwable error) {
        StringWriter stackTrace = new StringWriter();
        error.printStackTrace(new PrintWriter(stackTrace));
        stackTrace.flush();
        errorAttributes.put("trace", stackTrace.toString());
    }

    private void addPath(Map<String, Object> errorAttributes,
                         RequestAttributes requestAttributes) {
        String path = getAttribute(requestAttributes, "javax.servlet.error.request_uri");
        if (path != null) {
            errorAttributes.put("path", path);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getAttribute(RequestAttributes requestAttributes, String name) {
        return (T) requestAttributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
    }
}
