package com.ingot.cloud.bff.config;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.model.bff.BffAppRegistration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 按注册表 origin 精确校验请求来源。
 *
 * @author jy
 * @since 1.0.0
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class BffOriginFilter extends OncePerRequestFilter {
    private final BffProperties properties;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        List<String> allowedOrigins = registeredOrigins();
        if (CollUtil.isEmpty(allowedOrigins)) {
            filterChain.doFilter(request, response);
            return;
        }

        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        if (isAllowed(origin, allowedOrigins) || isRefererAllowed(referer, allowedOrigins)) {
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("[BffOriginFilter] blocked request from origin={}, referer={}, uri={}",
                origin, referer, request.getRequestURI());
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.getWriter().write("{\"code\":\"BFF_ENTRY_MISMATCH\",\"message\":\"Forbidden: origin not allowed\"}");
    }

    private List<String> registeredOrigins() {
        List<String> origins = new ArrayList<>();
        if (CollUtil.isNotEmpty(properties.getApps())) {
            for (BffAppRegistration app : properties.getApps()) {
                addOrigin(origins, app.getAdminOrigin());
                addOrigin(origins, app.getLoginOrigin());
            }
        }
        return origins;
    }

    private static void addOrigin(List<String> origins, String origin) {
        if (StrUtil.isNotBlank(origin)) {
            origins.add(StrUtil.removeSuffix(origin, "/"));
        }
    }

    private boolean isAllowed(String origin, List<String> allowedOrigins) {
        if (StrUtil.isEmpty(origin)) {
            return false;
        }
        return allowedOrigins.stream().anyMatch(allowed -> StrUtil.equalsIgnoreCase(origin, allowed));
    }

    private boolean isRefererAllowed(String referer, List<String> allowedOrigins) {
        if (StrUtil.isEmpty(referer)) {
            return false;
        }
        try {
            String refererOrigin = URI.create(referer).resolve("/").toString();
            String normalized = StrUtil.removeSuffix(refererOrigin, "/");
            return allowedOrigins.stream().anyMatch(
                    allowed -> StrUtil.equalsIgnoreCase(normalized, StrUtil.removeSuffix(allowed, "/")));
        } catch (Exception exception) {
            return false;
        }
    }
}
