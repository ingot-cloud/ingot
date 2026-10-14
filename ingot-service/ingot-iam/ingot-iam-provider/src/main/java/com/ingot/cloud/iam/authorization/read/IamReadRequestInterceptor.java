package com.ingot.cloud.iam.authorization.read;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 * <p>只为显式声明的方法建立读取上下文，在所有完成和异步移交路径清理。</p>
 * @author jy
 * @since 1.0.0
 */
public final class IamReadRequestInterceptor implements AsyncHandlerInterceptor {
    /** {@inheritDoc} */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        IamReadRequestContext.clear(request);
        if (handler instanceof HandlerMethod method && method.hasMethodAnnotation(IamReadOnlyRequest.class)) {
            IamReadRequestContext.begin(request);
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        IamReadRequestContext.clear(request);
    }

    /** {@inheritDoc} */
    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler) {
        IamReadRequestContext.clear(request);
    }
}
