package com.ingot.framework.security.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.base.status.BaseStatusCode;
import com.ingot.framework.core.wrapper.IngotResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.ingot.framework.core.constants.BeanIds.ACCESS_DENIED_HANDLER;

/**
 * <p>Description  : IngotAccessDeniedHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/9/12.</p>
 * <p>Time         : 下午2:26.</p>
 */
@Slf4j
@Component(ACCESS_DENIED_HANDLER)
public class IngotAccessDeniedHandler implements AccessDeniedHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override public void handle(HttpServletRequest request,
                                 HttpServletResponse response,
                                 AccessDeniedException e) throws IOException, ServletException {
        log.info(">>> IngotAccessDeniedHandler - 访问权限异常。exception={}", e, e);
        IngotResponse<?> body = new IngotResponse<>(BaseStatusCode.UNAUTHORIZED);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
