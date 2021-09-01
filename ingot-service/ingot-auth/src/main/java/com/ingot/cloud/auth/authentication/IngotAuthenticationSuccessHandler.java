package com.ingot.cloud.auth.authentication;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>Description  : IngotAuthenticationSuccessHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/3.</p>
 * <p>Time         : 11:39 上午.</p>
 */
@Slf4j
public class IngotAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override public void onAuthenticationSuccess(HttpServletRequest request,
                                                  HttpServletResponse response,
                                                  Authentication authentication) throws IOException, ServletException {
        log.info("Authentication Success");
    }
}
