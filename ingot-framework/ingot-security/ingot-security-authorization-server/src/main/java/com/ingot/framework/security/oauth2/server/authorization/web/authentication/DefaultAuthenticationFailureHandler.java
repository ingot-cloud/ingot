package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import java.io.IOException;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.constants.InOAuth2ParameterNames;
import com.ingot.framework.commons.model.common.AuthFailureDTO;
import com.ingot.framework.commons.model.event.LoginFailureEvent;
import com.ingot.framework.commons.model.status.BaseErrorCode;
import com.ingot.framework.commons.utils.DateUtil;
import com.ingot.framework.commons.utils.WebUtil;
import com.ingot.framework.core.context.SpringContextHolder;
import com.ingot.framework.security.oauth2.server.authorization.http.converter.CustomOAuth2ErrorConverter;
import com.ingot.framework.security.oauth2.server.authorization.http.converter.CustomOAuth2ErrorParametersConverter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * <p>Description  : 默认认证失败处理器.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/13.</p>
 * <p>Time         : 10:46 上午.</p>
 */
@Slf4j
public class DefaultAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final OAuth2ErrorHttpMessageConverter errorHttpResponseConverter;

    public DefaultAuthenticationFailureHandler() {
        this.errorHttpResponseConverter = new OAuth2ErrorHttpMessageConverter();
        this.errorHttpResponseConverter.setErrorConverter(new CustomOAuth2ErrorConverter());
        this.errorHttpResponseConverter.setErrorParametersConverter(new CustomOAuth2ErrorParametersConverter());
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.info("[DefaultAuthenticationFailureHandler] - onAuthenticationFailure", exception);

        SecurityContextHolder.clearContext();
        OAuth2Error error;
        if (exception instanceof OAuth2AuthenticationException) {
            error = ((OAuth2AuthenticationException) exception).getError();
            if (StrUtil.isEmpty(error.getDescription())) {
                error = new OAuth2Error(error.getErrorCode(), error.getErrorCode(), error.getUri());
            }
        } else {
            error = new OAuth2Error(BaseErrorCode.BAD_REQUEST.getCode(),
                    exception.getLocalizedMessage(), "");
        }

        // 密码认证失败时发布登录失败事件，供各服务异步处理失败计数和锁定逻辑
        String username = request.getParameter(OAuth2ParameterNames.USERNAME);
        if (StrUtil.isNotBlank(username)) {
            AuthFailureDTO payload = new AuthFailureDTO();
            payload.setUsername(username);
            payload.setUserType(request.getParameter(InOAuth2ParameterNames.USER_TYPE));
            payload.setTenantId(request.getParameter(InOAuth2ParameterNames.TENANT));
            payload.setIp(WebUtil.getClientIP(request));
            payload.setTime(DateUtil.now());
            payload.setErrorCode(error.getErrorCode());
            payload.setErrorMessage(error.getDescription());
            SpringContextHolder.publishEvent(new LoginFailureEvent(payload));
        }

        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        if (OAuth2ErrorCodes.INVALID_CLIENT.equals(error.getErrorCode())) {
            httpResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
        } else {
            httpResponse.setStatusCode(HttpStatus.BAD_REQUEST);
        }
        this.errorHttpResponseConverter.write(error, null, httpResponse);
    }
}
