package com.ingot.core.aspect;

import com.ingot.common.exception.BaseException;
import com.ingot.core.annotation.IngotPreconditions;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static com.ingot.common.constants.GlobalResponseCode.*;

/**
 * <p>Description  : PreconditionsAop.</p>
 * <p>Author       : wangchao.</p>
 * <p>Date         : 2018/12/25.</p>
 * <p>Time         : 9:15 AM.</p>
 */
@Slf4j
@Aspect
@Component
public class PreconditionsAop implements ApplicationContextAware {

    private ApplicationContext context;

    /**
     * PrePreconditionsAop annotation.
     */
    @Pointcut("@within(com.ingot.core.annotation.IngotPreconditions)")
    public void prePreconditionsAop() {
    }

    /**
     * Do before.
     *
     * @param joinPoint the join point
     */
    @Before("prePreconditionsAop()")
    @SuppressWarnings("unchecked")
    public void doBefore(final JoinPoint joinPoint) throws Throwable{
        String methodName = joinPoint.getSignature().getName();
        log.info(">>> PreconditionsAop doBefore - method={}", methodName);
        Object target = joinPoint.getTarget();
        IngotPreconditions annotation = target.getClass().getAnnotation(IngotPreconditions.class);
        Method method = ((MethodSignature)joinPoint.getSignature()).getMethod();

        if (method == null){
            throw new BaseException(GL99991000);
        }

        Class preconditionsCls = annotation.value();
        try {
            Object preconditionsObj = context.getBean(preconditionsCls);
            Method preconditionsMethod = preconditionsCls.getDeclaredMethod(methodName, method.getParameterTypes());
            log.info(">>> PreconditionsAop - 预校验调用{}的{}方法，参数类型={}", preconditionsObj.getClass().getSimpleName(), methodName, method.getParameterTypes());
            preconditionsMethod.invoke(preconditionsObj, joinPoint.getArgs());
        } catch (BeansException e) {
            log.error(">>> PreconditionsAop - 预校验类没有注入，请在预校验类中增加 @component 注解！！！", e);
            throw new BaseException(GL99991003);
        } catch (NoSuchMethodException e) {
            throw new BaseException(GL99991001);
        } catch (IllegalAccessException e) {
            throw new BaseException(GL99991002);
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

    @Override public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }
}
