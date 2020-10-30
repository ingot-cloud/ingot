package com.ingot.framework.security.provider;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * <p>Description  : IngotAuthenticationEntryPoint.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/12.</p>
 * <p>Time         : 下午2:59.</p>
 */
@Slf4j
@AllArgsConstructor
public class IngotAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override public void commence(HttpServletRequest request,
                                   HttpServletResponse response,
                                   AuthenticationException e) throws IOException, ServletException {
        log.info(">>> IngotAuthenticationEntryPoint - 认证入口点。exception={}", e.getMessage());
//        BaseResponse body = new BaseResponse(GL99990401.code(), e.getMessage());
//        response.setStatus(HttpStatus.UNAUTHORIZED.value());
//        response.setContentType(MediaType.APPLICATION_JSON_UTF8.toString());
//        response.getWriter().write(JSONObject.toJSONString(body));
        handlerExceptionResolver.resolveException(request, response, null, e);
    }
}
