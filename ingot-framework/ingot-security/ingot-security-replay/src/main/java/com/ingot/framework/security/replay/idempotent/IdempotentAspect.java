package com.ingot.framework.security.replay.idempotent;

import java.lang.reflect.Method;
import java.time.Duration;

import cn.hutool.core.util.StrUtil;
import com.ingot.framework.commons.error.BizException;
import com.ingot.framework.security.replay.ReplayErrorCode;
import com.ingot.framework.security.replay.ReplayProperties;
import com.ingot.framework.security.replay.store.NonceStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * <p>{@link Idempotent} 注解的环绕切面。</p>
 *
 * <p>解析 SpEL 幂等键，借助 {@link NonceStore} 在时间窗内拒绝重复请求：首次调用占用幂等键并放行，
 * 窗口内重复调用抛出 {@link ReplayErrorCode#REPLAY_NONCE_DUPLICATE}；存储不可用时按 fail-open/fail-close 策略处理。</p>
 *
 * @author jy
 * @since 1.0.0
 * @see Idempotent
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class IdempotentAspect {
    private final NonceStore nonceStore;
    private final ReplayProperties properties;

    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        String keyValue = resolveKey(joinPoint, idempotent.key());
        if (StrUtil.isBlank(keyValue)) {
            log.warn("[ingot-replay] 幂等键为空，跳过校验 - key={}", idempotent.key());
            return joinPoint.proceed();
        }

        String storeKey = properties.getKeyPrefix() + idempotent.namespace() + ":" + keyValue;
        Duration ttl = idempotent.ttl() > 0 ? Duration.ofSeconds(idempotent.ttl()) : properties.getWindow();

        boolean acquired;
        try {
            acquired = nonceStore.tryAcquire(storeKey, ttl);
        } catch (Exception e) {
            if (properties.isFailOpen()) {
                log.warn("[ingot-replay] 幂等存储不可用，fail-open 放行 - key={}", storeKey, e);
                return joinPoint.proceed();
            }
            log.error("[ingot-replay] 幂等存储不可用，fail-close 拒绝 - key={}", storeKey, e);
            throw new BizException(ReplayErrorCode.REPLAY_STORE_UNAVAILABLE);
        }

        if (!acquired) {
            String message = StrUtil.isBlank(idempotent.message())
                    ? ReplayErrorCode.REPLAY_NONCE_DUPLICATE.getText()
                    : idempotent.message();
            throw new BizException(ReplayErrorCode.REPLAY_NONCE_DUPLICATE.getCode(), message);
        }
        return joinPoint.proceed();
    }

    private String resolveKey(ProceedingJoinPoint joinPoint, String keyExpression) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        EvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        Expression expression = parser.parseExpression(keyExpression);
        Object value = expression.getValue(context);
        return value == null ? null : value.toString();
    }
}
