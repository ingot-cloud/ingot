package com.ingot.cloud.acs.authentication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>Description  : IngotAuthenticationFailureHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/3.</p>
 * <p>Time         : 11:42 上午.</p>
 */
@Slf4j
public class IngotAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override public void onAuthenticationFailure(HttpServletRequest request,
                                                  HttpServletResponse response,
                                                  AuthenticationException exception) throws IOException, ServletException {
        log.info("Authentication failure");
    }
}
