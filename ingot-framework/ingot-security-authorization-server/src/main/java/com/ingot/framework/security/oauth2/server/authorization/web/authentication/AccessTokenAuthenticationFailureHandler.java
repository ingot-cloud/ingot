package com.ingot.framework.security.oauth2.server.authorization.web.authentication;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.model.status.BaseErrorCode;
import com.ingot.framework.security.oauth2.server.authorization.http.converter.IngotOAuth2ErrorConverter;
import com.ingot.framework.security.oauth2.server.authorization.http.converter.IngotOAuth2ErrorParametersConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * <p>Description  : AccessTokenAuthenticationFailureHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2021/9/13.</p>
 * <p>Time         : 10:46 上午.</p>
 */
@Slf4j
public class AccessTokenAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private final OAuth2ErrorHttpMessageConverter errorHttpResponseConverter;

    public AccessTokenAuthenticationFailureHandler() {
        this.errorHttpResponseConverter = new OAuth2ErrorHttpMessageConverter();
        this.errorHttpResponseConverter.setErrorConverter(new IngotOAuth2ErrorConverter());
        this.errorHttpResponseConverter.setErrorParametersConverter(new IngotOAuth2ErrorParametersConverter());
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        log.info("[AccessTokenAuthenticationFailureHandler] - onAuthenticationFailure", exception);

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

        ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
        if (OAuth2ErrorCodes.INVALID_CLIENT.equals(error.getErrorCode())) {
            httpResponse.setStatusCode(HttpStatus.UNAUTHORIZED);
        } else {
            httpResponse.setStatusCode(HttpStatus.BAD_REQUEST);
        }
        this.errorHttpResponseConverter.write(error, null, httpResponse);
    }
}
