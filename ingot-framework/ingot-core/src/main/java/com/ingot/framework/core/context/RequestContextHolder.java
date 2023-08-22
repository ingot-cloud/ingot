package com.ingot.framework.core.context;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * <p>Description  : RequestContextHolder.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2020/11/26.</p>
 * <p>Time         : 2:27 下午.</p>
 */
public final class RequestContextHolder {

    /**
     * Get current HttpServletRequest
     *
     * @return Servlet request
     */
    public static Optional<HttpServletRequest> getRequest() {
        return Optional.ofNullable(org.springframework.web.context.request.RequestContextHolder.getRequestAttributes())
                .filter(requestAttributes -> ServletRequestAttributes.class.isAssignableFrom(requestAttributes.getClass()))
                .map(requestAttributes -> ((ServletRequestAttributes) requestAttributes))
                .map(ServletRequestAttributes::getRequest);
    }
}
