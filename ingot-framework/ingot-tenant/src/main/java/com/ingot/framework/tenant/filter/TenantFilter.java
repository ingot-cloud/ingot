package com.ingot.framework.tenant.filter;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.core.constants.TenantConstants;
import com.ingot.framework.tenant.TenantContextHolder;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>Description  : TenantFilter.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/23.</p>
 * <p>Time         : 6:05 下午.</p>
 */
@Slf4j
public class TenantFilter extends GenericFilterBean {

    @Override
    @SneakyThrows
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        final String url = request.getRequestURI();
        log.debug("{} at filter chain; firing Filter: 'TenantFilter'", url);

        String tenantId = request.getHeader(TenantConstants.TENANT_HEADER_KEY);
        log.debug(">>> 获取 header 中的租户 ID={}", tenantId);

        if (StrUtil.isNotBlank(tenantId)) {
            TenantContextHolder.set(Long.valueOf(tenantId));
        } else {
            TenantContextHolder.set(TenantConstants.DEFAULT_TENANT_ID);
        }

        filterChain.doFilter(request, response);
        TenantContextHolder.clear();
    }
}
