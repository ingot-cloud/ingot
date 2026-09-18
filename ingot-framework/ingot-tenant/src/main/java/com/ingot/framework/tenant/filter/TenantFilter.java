package com.ingot.framework.tenant.filter;

import java.io.IOException;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.utils.RequestParamsUtil;
import com.ingot.framework.tenant.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * <p>从请求头解析租户并写入当前线程上下文；请求结束后清空。</p>
 *
 * <p>未携带租户或租户 ID 非法时保持上下文为空，不填入默认租户。平台请求不属于任何租户。</p>
 *
 * @author wangchao
 * @since 1.0.0
 */
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String url = request.getRequestURI();
        log.info("[TenantFilter] do filter url = {}", url);

        try {
            Long tenant = parseTenantId(RequestParamsUtil.getTenantId(request));
            if (tenant != null) {
                TenantContextHolder.set(tenant);
                log.info("[TenantFilter] 设置 tenantId = {}", tenant);
            } else {
                log.info("[TenantFilter] 未设置租户");
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }

    private static Long parseTenantId(String tenantId) {
        if (StrUtil.isBlank(tenantId)) {
            return null;
        }
        try {
            return Long.parseLong(tenantId);
        } catch (NumberFormatException ex) {
            log.info("[TenantFilter] 非法租户ID，忽略. value={}", tenantId);
            return null;
        }
    }
}
