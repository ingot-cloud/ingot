package com.ingot.framework.core.aspect;

import java.util.Arrays;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.google.common.collect.Maps;
import com.ingot.framework.core.context.RequestContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * <p>Description  : RequestLogAop.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/25.</p>
 * <p>Time         : 10:37 AM.</p>
 */
@Slf4j
@Aspect
@Component
@Order(Integer.MIN_VALUE + 1)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class RequestLogAop {

    /**
     * Point cut wrapper.
     */
    @Pointcut("execution(public com.ingot.framework.core.wrapper.R *(..))")
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
        long startTime = System.currentTimeMillis();

        HttpServletRequest request = RequestContextHolder.getRequest().orElse(null);

        if (request == null){
            return pjp.proceed();
        }

        Map<String, Object> param = Maps.newHashMap();
        param.put("URL", request.getRequestURI());
        param.put("HTTP_METHOD", request.getMethod());
        param.put("IP", request.getRemoteAddr());
        param.put("CLASS_METHOD", pjp.getSignature().getDeclaringTypeName() + "." + pjp.getSignature().getName());
        param.put("ARGS", Arrays.toString(pjp.getArgs()));

        log.info("[RequestLog] - request param={}", param);

        Object result;

        try {
            result = pjp.proceed();
            log.debug("[RequestLog] - {} response = {}", pjp.getSignature(), request);
        } finally {
            log.info("[RequestLog] - " + pjp.getSignature() + " use time:" + (System.currentTimeMillis() - startTime));
        }

        return result;
    }
}
