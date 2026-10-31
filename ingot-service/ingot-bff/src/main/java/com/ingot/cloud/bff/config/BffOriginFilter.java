package com.ingot.cloud.bff.config;

import java.io.IOException;
import com.ingot.cloud.bff.error.BffAuthException;
import com.ingot.framework.commons.constants.BffConstants;
import com.ingot.framework.commons.model.bff.BffErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>
 * 认证自举同样验证精确来源，拒绝表单和查询参数传入 OAuth/重定向身份。
 * </p>
 *
 * @author jy
 * @since 1.0.0
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class BffOriginFilter extends OncePerRequestFilter {

    private final BffAppRegistry registry;

    private boolean json(String contentType) {
        try {
            return MediaType.APPLICATION_JSON.isCompatibleWith(MediaType.parseMediaType(contentType));
        }
        catch (IllegalArgumentException exception) {
            return false;
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith(BffConstants.ROOT)) {
            chain.doFilter(request, response);
            return;
        }
        response.setHeader(org.springframework.http.HttpHeaders.CACHE_CONTROL, "no-store");
        try {
            registry.requireSource(request, registry.requireFromRequest(request));
            if (!request.getParameterMap().isEmpty())
                throw new BffAuthException(BffErrorCode.INVALID_REQUEST);
            if ("POST".equals(request.getMethod())
                    && (request.getContentType() == null || !json(request.getContentType())))
                throw new BffAuthException(BffErrorCode.INVALID_REQUEST);
            chain.doFilter(request, response);
        }
        catch (BffAuthException exception) {
            BffErrorCode error = exception.getErrorCode();
            response.setStatus(error.getHttpStatus());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(java.nio.charset.StandardCharsets.UTF_8.name());
            response.getWriter()
                .write("{\"code\":\"" + error.getCode() + "\",\"message\":\"" + error.getText() + "\"}");
        }
    }

}
