package com.ingot.framework.core.utils.preconditions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ingot.framework.commons.error.BizException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.lang.NonNull;

import static com.ingot.framework.commons.model.status.CoreErrorCode.*;

/**
 * <p>Description  : PreconditionsAop.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/25.</p>
 * <p>Time         : 9:15 AM.</p>
 */
@Slf4j
@Aspect
public class PreconditionsAop implements ApplicationContextAware {

    private ApplicationContext context;

    /**
     * PrePreconditionsAop annotation.
     */
    @Pointcut("@within(com.ingot.framework.core.utils.preconditions.InPreconditions)")
    public void prePreconditionsAop() {
    }

    /**
     * Do before.
     *
     * @param joinPoint the join point
     */
    @Before("prePreconditionsAop()")
    public void doBefore(final JoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        log.info(">>> PreconditionsAop doBefore - method={}", methodName);
        Object target = joinPoint.getTarget();
        InPreconditions annotation = target.getClass().getAnnotation(InPreconditions.class);
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();

        if (method == null) {
            throw new BizException(PRECONDITION_NO_SUCH_METHOD);
        }

        Class<?> preconditionsCls = annotation.value();
        try {
            Object preconditionsObj = context.getBean(preconditionsCls);
            Method preconditionsMethod = preconditionsCls.getDeclaredMethod(methodName, method.getParameterTypes());
            log.info(">>> PreconditionsAop - 预校验调用{}的{}方法，参数类型={}", preconditionsObj.getClass().getSimpleName(), methodName, method.getParameterTypes());
            preconditionsMethod.invoke(preconditionsObj, joinPoint.getArgs());
        } catch (BeansException e) {
            log.error(">>> PreconditionsAop - 预校验类没有注入，请在预校验类中增加 @component 注解！！！", e);
            throw new BizException(PRECONDITION_BEANS);
        } catch (NoSuchMethodException e) {
            throw new BizException(PRECONDITION_NO_SUCH_METHOD);
        } catch (IllegalAccessException e) {
            throw new BizException(PRECONDITION_ILLEGAL_ACCESS);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
        log.info(">>> PreconditionsAop method name={}, invoke success.", methodName);
    }

//    /**
//     * Do after.
//     */
//    @AfterReturning(pointcut = "prePreconditionsAop()")
//    public void doAfter(final JoinPoint joinPoint) {
//        log.info(">>> PreconditionsAop end");
//    }

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
}
