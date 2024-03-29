package com.ingot.framework.core.utils;

import cn.hutool.core.map.MapUtil;
import com.ingot.framework.core.context.RequestContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Arrays;
import java.util.Map;

/**
 * <p>Description  : RequestLogAop.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2019/3/25.</p>
 * <p>Time         : 10:37 AM.</p>
 */
@Slf4j
@Aspect
public class RequestLogAop {

    /**
     * Point cut wrapper.
     */
    @Pointcut("execution(public com.ingot.framework.core.model.support.R *(..))")
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

        Map<String, Object> param = MapUtil.newHashMap();
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
