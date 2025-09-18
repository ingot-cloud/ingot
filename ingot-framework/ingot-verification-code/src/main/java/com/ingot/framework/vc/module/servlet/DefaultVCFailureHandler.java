package com.ingot.framework.vc.module.servlet;

import java.io.IOException;

import cn.hutool.core.util.CharsetUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ingot.framework.commons.model.support.R;
import com.ingot.framework.vc.common.VCException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * <p>Description  : DefaultVCFailureHandler.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2023/6/15.</p>
 * <p>Time         : 11:12 AM.</p>
 */
@Slf4j
public class DefaultVCFailureHandler implements VCFailureHandler {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onFailure(HttpServletRequest request,
                          HttpServletResponse response,
                          VCException exception) throws IOException, ServletException {
        R<?> body = R.error(exception.getCode(), exception.getMessage());
        response.setStatus(HttpStatus.BAD_REQUEST.value());
        response.setCharacterEncoding(CharsetUtil.UTF_8);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        response.flushBuffer();
    }
}
