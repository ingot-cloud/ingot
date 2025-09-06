package com.ingot.framework.core.utils;

import cn.hutool.core.util.BooleanUtil;
import com.ingot.framework.core.config.CoreProperties;
import com.ingot.framework.core.context.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Arrays;

/**
 * <p>Description  : RequestLogAop.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/25.</p>
 * <p>Time         : 10:37 AM.</p>
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class RequestLogAop {
    private final CoreProperties properties;

    /**
     * Point cut wrapper.
     */
    @Pointcut("execution(public com.ingot.framework.commons.model.support.R *(..))")
    public void pointCutWrapper() {
    }

    /**
     * Method wrapper handler object.
     *
     * @param pjp the pjp
     * @return the object
     */
    @Around("pointCutWrapper()")
    public Object methodWrapperHandler(ProceedingJoinPoint pjp) throws Throwable {
        if (BooleanUtil.isFalse(properties.getRequestLog())) {
            return pjp.proceed();
        }

        long startTime = System.currentTimeMillis();

        HttpServletRequest request = RequestContextHolder.getRequest().orElse(null);

        if (request == null) {
            return pjp.proceed();
        }

        log.info("""
                        
                        
                        [RequestLog] - {}
                        IP: {}
                        HTTP METHOD: {}
                        CLASS_METHOD: {}
                        ARGS: {}
                        """,
                request.getRequestURI(),
                request.getRemoteAddr(),
                request.getMethod(),
                pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName(),
                Arrays.toString(pjp.getArgs()));

        Object result;
        try {
            result = pjp.proceed();
            log.info("""
                            
                            
                            [RequestLog] - {}
                            IP: {}
                            HTTP METHOD: {}
                            CLASS_METHOD: {}
                            RESPONSE: {}
                            """,
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    request.getMethod(),
                    pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName(),
                    result);
        } finally {
            log.info("""
                            
                            
                            [RequestLog] - {}
                            IP: {}
                            HTTP METHOD: {}
                            CLASS_METHOD: {}
                            USE TIME: {}ms
                            """,
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    request.getMethod(),
                    pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName(),
                    (System.currentTimeMillis() - startTime));
        }

        return result;
    }
}
