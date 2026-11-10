package com.ingot.framework.core.error;

import java.util.LinkedHashMap;
import java.util.Map;

import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import com.ingot.framework.commons.model.support.R;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.core.Ordered;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.WebRequest;

/**
 * <p>将 Servlet 错误分发统一为安全响应，诊断细节只保留在服务端日志。</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/1/10.</p>
 * <p>Time         : 5:24 PM.</p>
 */
@Slf4j
public class InErrorAttributes implements ErrorAttributes, Ordered {
    private static final String ERROR_ATTRIBUTE = InErrorAttributes.class.getName() + ".ERROR";

    /** 错误分发只输出受控的代码与提示。 */
    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Throwable error = getError(webRequest);
        Object status = webRequest.getAttribute(RequestDispatcher.ERROR_STATUS_CODE, RequestAttributes.SCOPE_REQUEST);
        log.error("Servlet 错误分发 status={}", status, error);
        while (error instanceof ServletException && error.getCause() != null) {
            error = error.getCause();
        }

        Map<String, Object> result = new LinkedHashMap<>();
        if (error instanceof BizException business) {
            result.put(R.CODE, business.getCode());
            result.put(R.MESSAGE, BaseErrorCode.INTERNAL_SERVER_ERROR.getCode().equals(business.getCode())
                    ? BaseErrorCode.INTERNAL_SERVER_ERROR.getText() : business.getMessage());
        } else if (error instanceof OAuth2AuthenticationException authentication) {
            var oauth = authentication.getError();
            result.put(R.CODE, oauth.getErrorCode());
            String description = oauth.getDescription();
            if (OAuth2ErrorCodes.SERVER_ERROR.equals(oauth.getErrorCode())) {
                description = BaseErrorCode.INTERNAL_SERVER_ERROR.getText();
            } else if (description == null || description.isBlank() || description.equals(oauth.getErrorCode())) {
                description = "认证未通过，请重新登录后重试";
            }
            result.put(R.MESSAGE, description);
        } else {
            result.put(R.CODE, BaseErrorCode.INTERNAL_SERVER_ERROR.getCode());
            result.put(R.MESSAGE, BaseErrorCode.INTERNAL_SERVER_ERROR.getText());
        }
        return result;
    }

    /** 优先读取统一属性，再回退到 Servlet 错误分发属性。 */
    @Override
    public Throwable getError(WebRequest webRequest) {
        Throwable exception = getAttribute(webRequest, ERROR_ATTRIBUTE);
        if (exception == null) {
            exception = getAttribute(webRequest, RequestDispatcher.ERROR_EXCEPTION);
        }
        return exception;
    }

    /** 在默认属性处理器之前应用受控响应。 */
    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @SuppressWarnings("unchecked")
    private <T> T getAttribute(RequestAttributes requestAttributes, String name) {
        return (T) requestAttributes.getAttribute(name, RequestAttributes.SCOPE_REQUEST);
    }
}
