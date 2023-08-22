package com.ingot.framework.tenant.filter;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.HeaderConstants;
import com.ingot.framework.tenant.TenantContextHolder;
import com.ingot.framework.tenant.properties.TenantProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * <p>Description  : TenantFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/23.</p>
 * <p>Time         : 6:05 下午.</p>
 */
@Slf4j
@RequiredArgsConstructor
public class TenantFilter extends OncePerRequestFilter {
    private final TenantProperties tenantProperties;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String url = request.getRequestURI();
        log.info("[TenantFilter] do filter url = {}", url);

        String tenantId = request.getHeader(HeaderConstants.TENANT);
        boolean hasHeaderTenantId = StrUtil.isNotBlank(tenantId);

        try {
            log.info("[TenantFilter] Header 中{} key = {}",
                    hasHeaderTenantId ? "存在" : "不存在", HeaderConstants.TENANT);
            if (hasHeaderTenantId) {
                Long tenant = Long.parseLong(tenantId);
                TenantContextHolder.set(tenant);
                log.info("[TenantFilter] 设置 tenantId = {}", tenant);
            } else {
                TenantContextHolder.setDefault(tenantProperties.getDefaultId());
                log.info("[TenantFilter] 设置 tenantId = {}, 使用默认值", tenantProperties.getDefaultId());
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContextHolder.clear();
        }
    }
}
